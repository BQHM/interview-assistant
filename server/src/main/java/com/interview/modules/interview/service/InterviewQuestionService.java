package com.interview.modules.interview.service;

import com.interview.common.exception.ErrorCode;
import com.interview.infrastructure.ai.StructuredOutputInvoker;
import com.interview.modules.interview.model.HistoricalQuestion;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.skill.InterviewSkillService;
import com.interview.modules.interview.skill.model.InterviewSkillCategoryDTO;
import com.interview.modules.interview.skill.model.InterviewSkillDTO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件功能说明
 * <p>负责面试题生成业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-07-02
 */
@Slf4j
@Service
public class InterviewQuestionService {

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<QuestionListDTO> outputConverter;
    private final InterviewSkillService interviewSkillService;
    private final StructuredOutputInvoker structuredOutputInvoker;

    /**
     * 功能说明
     * <p>初始化 AI 客户端和 Prompt 模板。</p>
     *
     * @param chatClientBuilder     AI 客户端构建器
     * @param resourceLoader        资源加载器
     * @param interviewSkillService 面试方向配置服务
     * @param structuredOutputInvoker 结构化输出调用器
     * @throws IOException 当 Prompt 模板读取失败时抛出
     * @author NobuNo
     * @date 2026-07-02
     */
    public InterviewQuestionService(
            ChatClient.Builder chatClientBuilder,
            ResourceLoader resourceLoader,
            InterviewSkillService interviewSkillService,
            StructuredOutputInvoker structuredOutputInvoker
    ) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.outputConverter = new BeanOutputConverter<>(QuestionListDTO.class);
        this.systemPromptTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/interview-question-system.st").getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/interview-question-user.st").getContentAsString(StandardCharsets.UTF_8));
        this.interviewSkillService = interviewSkillService;
    }

    /**
     * 功能说明
     * <p>根据简历文本生成面试题。</p>
     *
     * @param resumeText    简历文本
     * @param questionCount 题目数量
     * @return 面试题列表
     * @author NobuNo
     * @date 2026-07-02
     */
    public List<InterviewQuestionDTO> generateQuestions(String resumeText, Integer questionCount) {
        // 未指定面试方向时，默认使用 Java 后端 Skill
        return generateQuestions(resumeText, questionCount, "java-backend");
    }

    /**
     * 功能说明
     * <p>根据面试方向生成面试题，不参考历史题目。</p>
     *
     * @param resumeText    简历文本
     * @param questionCount 题目数量
     * @param skillId       面试方向编号
     * @return 面试题列表
     * @author NobuNo
     * @date 2026-07-02
     */
    public List<InterviewQuestionDTO> generateQuestions(String resumeText, Integer questionCount, String skillId) {
        return generateQuestions(resumeText, questionCount, skillId, List.of());
    }

    /**
     * 功能说明
     * <p>根据面试方向和历史题目生成面试题。</p>
     *
     * @param resumeText         简历文本
     * @param questionCount      题目数量
     * @param skillId            面试方向编号
     * @param historicalQuestions 同一简历、同一面试方向的历史题目
     * @return 面试题列表
     * @author NobuNo
     * @date 2026-07-02
     */
    public List<InterviewQuestionDTO> generateQuestions(String resumeText, Integer questionCount, String skillId, List<HistoricalQuestion> historicalQuestions) {


        // 先读取 Skill 配置，后续的 AI 出题和规则兜底都必须使用同一个面试方向。
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill(skillId);
        log.debug("开始生成面试题: skillId={}, skillName={}", skillDTO.getId(), skillDTO.getName());
        try {
            // 优先使用 AI 生成问题，便于结合简历内容产生更有针对性的题目。
            List<InterviewQuestionDTO> aiQuestionDTOs = generateQuestionsByAi(resumeText, questionCount, skillDTO, historicalQuestions);

            if (!aiQuestionDTOs.isEmpty()) {
                // AI 返回的题号、类型或题目数量可能不稳定，先统一整理后再返回。
                return normalizeGeneratedQuestions(
                        aiQuestionDTOs,
                        resumeText,
                        questionCount,
                        skillDTO);
            }
        } catch (Exception e) {
            log.warn("AI 出题失败，fallback to rule-based question generation", e);
        }

        // AI 调用失败或返回空结果时，使用规则题保证面试主流程仍然可以继续。
        return generateRuleBasedQuestions(resumeText, questionCount, skillDTO);
    }

    /**
     * 功能说明
     * <p>调用 AI 生成面试题。</p>
     *
     * @param resumeText         简历文本
     * @param questionCount      题目数量
     * @param skillDTO              面试方向配置
     * @param historicalQuestions 同一简历、同一面试方向的历史题目
     * @return AI 生成的题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> generateQuestionsByAi(String resumeText, Integer questionCount, InterviewSkillDTO skillDTO, List<HistoricalQuestion> historicalQuestions) {

        // 后端约束题目数量为正数；这里再次设置默认值，保护 Service 被其他调用方直接使用时的行为。
        Integer safeQuestionCount = (questionCount == null || questionCount <= 0) ? 3 : questionCount;

        // 根据 Skill 的分类优先级计算每个分类应分配的题目数量，并把结果写入 Prompt。
        Map<String, Integer> allocation = interviewSkillService.calculateAllocation(skillDTO.getCategories(), safeQuestionCount);

        Map<String, Object> variables = new HashMap<>();
        variables.put("questionCount", safeQuestionCount);
        variables.put("resumeText", resumeText == null ? "" : resumeText);

        // 构建面试方向分类说明
        String skillCategorySection = buildSkillCategorySection(skillDTO.getCategories(), allocation);
        variables.put("skillCategories", skillCategorySection);
        // 构建历史考点说明，提醒 AI 避免重复出题
        String historicalSection = buildHistoricalSection(historicalQuestions);
        variables.put("historicalSection", historicalSection);

        Map<String, Object> systemVariables = new HashMap<>();
        systemVariables.put("persona", skillDTO.getPersona() == null ? "" : skillDTO.getPersona());

        String systemPromptWithFormat = systemPromptTemplate.render(systemVariables) + "\n\n" + outputConverter.getFormat();
        String userPrompt = userPromptTemplate.render(variables);

        // 交给统一的结构化输出调用器：失败会按配置重试；重试仍失败抛 BusinessException，由上层 catch 降级到规则题。
        QuestionListDTO questionListDTO = structuredOutputInvoker.invoke(
                chatClient, systemPromptWithFormat, userPrompt, outputConverter,
                ErrorCode.AI_STRUCTURED_OUTPUT_FAILED, "AI 出题结构化解析失败：", "AI 出题");

        // invoke 不会返回 null，这里只防御 questions 字段缺失。
        if (questionListDTO.getQuestions() == null) {
            return List.of();
        }

        return questionListDTO.getQuestions();
    }

    /**
     * 功能说明
     * <p>构建面试方向分类说明。</p>
     *
     * @param categories 面试方向分类列表
     * @param allocation 分类题目数量
     * @return 面试方向分类说明
     * @author NobuNo
     * @date 2026-07-02
     */
    private String buildSkillCategorySection(List<InterviewSkillCategoryDTO> categories, Map<String, Integer> allocation) {
        if (categories == null || categories.isEmpty()) {
            return "未配置固定考察方向，请结合候选人简历生成题目。";
        }

        StringBuilder sectionBuilder = new StringBuilder();

        for (InterviewSkillCategoryDTO category : categories) {
            if (category == null
                    || category.getKey() == null
                    || category.getKey().isBlank()
                    || category.getLabel() == null
                    || category.getLabel().isBlank()) {
                continue;
            }

            Integer categoryQuestionCount = allocation.getOrDefault(category.getKey(), 0);

            if (categoryQuestionCount <= 0) {
                continue;
            }

            String priority = category.getPriority() == null ? "" : category.getPriority();

            String priorityInstruction = switch (priority) {
                case "ALWAYS_ONE" -> "必须至少生成 1 道题";
                case "CORE" -> "核心方向，优先覆盖";
                case "NORMAL" -> "常规方向，结合简历内容覆盖";
                default -> "结合简历内容合理覆盖";
            };

            sectionBuilder.append("- ")
                    .append(category.getKey())
                    .append("（")
                    .append(category.getLabel())
                    .append("）：目标生成 ")
                    .append(categoryQuestionCount)
                    .append(" 道题，")
                    .append(priorityInstruction)
                    .append("\n");
        }

        if (sectionBuilder.length() == 0) {
            return "未配置有效考察方向，请结合候选人简历生成题目。";
        }

        return sectionBuilder.toString().trim();
    }

    /**
     * 功能说明
     * <p>构建已考知识点说明，提醒 AI 避免重复出题。</p>
     *
     * @param historicalQuestions 历史题目列表
     * @return 已考知识点说明
     * @author NobuNo
     * @date 2026-07-02
     */
    private String buildHistoricalSection(List<HistoricalQuestion> historicalQuestions) {
        if (historicalQuestions == null || historicalQuestions.isEmpty()) {
            return "暂无历史提问";
        }

        Map<String, List<String>> groupedSummary = new LinkedHashMap<>();

        for (HistoricalQuestion historicalQuestion : historicalQuestions) {

            // 类型为空就归到默认类型
            String type = (historicalQuestion.type() != null && !historicalQuestion.type().isBlank())
                    ? historicalQuestion.type() : "GENERAL";

            // 摘要优先；没有摘要就截断题目正文兜底
            String summary = historicalQuestion.topicSummary();
            if (summary == null || summary.isBlank()) {
                String question = historicalQuestion.question();
                summary = question.length() > 30 ? question.substring(0, 30) + "…" : question;
            }

            // 放进分组：没有该类型就先建一个空列表
            groupedSummary.computeIfAbsent(type, k -> new ArrayList<>()).add(summary);
        }

        StringBuilder sectionBuilder = new StringBuilder("已考过的知识点（避免重复出题）：\n");

        for (Map.Entry<String, List<String>> entry : groupedSummary.entrySet()) {
            sectionBuilder.append("- ").append(entry.getKey()).append(": ").append(String.join(", ", entry.getValue())).append('\n');
        }

        return sectionBuilder.toString();
    }

    /**
     * 功能说明
     * <p>规整 AI 返回的题目列表。</p>
     *
     * @param aiQuestionDTOs AI 题目列表
     * @param resumeText    简历文本
     * @param questionCount 题目数量
     * @param skillDTO         面试方向配置
     * @return 规整后的题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> normalizeGeneratedQuestions(
            List<InterviewQuestionDTO> aiQuestionDTOs,
            String resumeText,
            Integer questionCount,
            InterviewSkillDTO skillDTO) {

        // 无论 AI 返回什么题号，最终都由程序重新按 0、1、2... 编号，保证前端按顺序答题。
        Integer safeQuestionCount = questionCount == null || questionCount <= 0 ? 3 : questionCount;

        List<InterviewQuestionDTO> normalizedQuestionDTOs = new ArrayList<>();

        for (InterviewQuestionDTO questionDTO : aiQuestionDTOs) {
            if (normalizedQuestionDTOs.size() >= safeQuestionCount) {
                break;
            }

            // 跳过 AI 返回的空对象或空题目，避免无效数据进入会话快照。
            if (questionDTO == null || questionDTO.getQuestion() == null
                    || questionDTO.getQuestion().isBlank()) {
                continue;
            }

            String type = questionDTO.getType();
            if (type == null || type.isBlank()) {
                type = "GENERAL";
            }

            String category = questionDTO.getCategory();
            if (category == null || category.isBlank()) {
                category = "综合能力";
            }

            normalizedQuestionDTOs.add(createQuestion(
                    normalizedQuestionDTOs.size(),
                    questionDTO.getQuestion(),
                    type,
                    category,
                    questionDTO.getTopicSummary()));
        }

        // AI 题目不足时，用当前 Skill 的规则题补齐，确保最终题量满足请求。
        if (normalizedQuestionDTOs.size() < safeQuestionCount) {
            List<InterviewQuestionDTO> ruleBasedQuestionDTOs = generateRuleBasedQuestions(
                    resumeText,
                    safeQuestionCount,
                    skillDTO);

            for (InterviewQuestionDTO questionDTO : ruleBasedQuestionDTOs) {
                if (normalizedQuestionDTOs.size() >= safeQuestionCount) {
                    break;
                }

                normalizedQuestionDTOs.add(createQuestion(
                        normalizedQuestionDTOs.size(),
                        questionDTO.getQuestion(),
                        questionDTO.getType(),
                        questionDTO.getCategory()));
            }
        }

        return normalizedQuestionDTOs;
    }

    /**
     * 功能说明
     * <p>生成规则版兜底面试题。</p>
     *
     * @param resumeText    简历文本
     * @param questionCount 题目数量
     * @param skillDTO         面试方向配置
     * @return 规则版题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> generateRuleBasedQuestions(
            String resumeText,
            Integer questionCount,
            InterviewSkillDTO skillDTO) {

        // 根据是否存在简历确定问题上下文
        String experienceContext =
                resumeText == null || resumeText.isBlank()
                        ? "请结合你的实际经历"
                        : "请结合你的简历或项目经历";

        Integer safeQuestionCount = null;// 面试问题数量
        if (questionCount == null || questionCount <= 0) {
            safeQuestionCount = 3;
        } else {
            safeQuestionCount = questionCount;
        }

        // 计算题目分配
        Map<String, Integer> allocation = interviewSkillService.calculateAllocation(skillDTO.getCategories(), safeQuestionCount);

        List<InterviewQuestionDTO> interviewQuestionDTOs = new ArrayList<>();

        List<String> questionFocuses = List.of(
                "核心原理",
                "项目实践",
                "问题排查",
                "性能优化");

        List<InterviewSkillCategoryDTO> categoryDTOs =
                skillDTO.getCategories() == null
                        ? List.of()
                        : skillDTO.getCategories();

        for (InterviewSkillCategoryDTO categoryDTO : categoryDTOs) {
            if (categoryDTO == null
                    || categoryDTO.getKey() == null
                    || categoryDTO.getKey().isBlank()
                    || categoryDTO.getLabel() == null
                    || categoryDTO.getLabel().isBlank()) {
                continue;
            }

            Integer categoryQuestionCount = allocation.getOrDefault(categoryDTO.getKey(), 0);

            for (int questionIndex = 0; questionIndex < categoryQuestionCount; questionIndex++) {

                String focus = questionFocuses.get(questionIndex % questionFocuses.size());

                String question = experienceContext + "，谈谈你在“" + categoryDTO.getLabel() + "”方向的" + focus + "。";

                interviewQuestionDTOs.add(createQuestion(
                        interviewQuestionDTOs.size(),
                        question,
                        categoryDTO.getKey(),
                        categoryDTO.getLabel()));
            }
        }

        String skillName = skillDTO.getName() == null ? "当前面试方向" : skillDTO.getName();

        while (interviewQuestionDTOs.size() < safeQuestionCount) {
            String focus = questionFocuses.get(interviewQuestionDTOs.size() % questionFocuses.size());

            interviewQuestionDTOs.add(createQuestion(
                    interviewQuestionDTOs.size(),
                    experienceContext + "，说明你对" + skillName + "岗位" + focus + "的理解。",
                    "GENERAL",
                    skillName));
        }

        return interviewQuestionDTOs;
    }

    /**
     * 功能说明
     * <p>创建单道面试题对象。</p>
     *
     * @param questionIndex 题目索引
     * @param question      题目内容
     * @param type          题目类型
     * @param category      题目分类
     * @param topicSummary  知识点摘要
     * @return 面试题 DTO
     * @author NobuNo
     * @date 2026-07-02
     */
    private InterviewQuestionDTO createQuestion(
            Integer questionIndex,
            String question,
            String type,
            String category,
            String topicSummary) {

        InterviewQuestionDTO interviewQuestionDTO = new InterviewQuestionDTO();
        interviewQuestionDTO.setQuestionIndex(questionIndex);
        interviewQuestionDTO.setQuestion(question);
        interviewQuestionDTO.setType(type);
        interviewQuestionDTO.setCategory(category);
        interviewQuestionDTO.setTopicSummary(topicSummary);
        return interviewQuestionDTO;
    }

    /**
     * 功能说明
     * <p>创建不包含知识点摘要的面试题对象。</p>
     *
     * @param questionIndex 题目索引
     * @param question      题目内容
     * @param type          题目类型
     * @param category      题目分类
     * @return 面试题 DTO
     * @author NobuNo
     * @date 2026-07-02
     */
    private InterviewQuestionDTO createQuestion(
            Integer questionIndex,
            String question,
            String type,
            String category) {

        return createQuestion(
                questionIndex,
                question,
                type,
                category,
                null);
    }

    /**
     * 文件功能说明
     * <p>负责承接 AI 返回的题目列表。</p>
     *
     * @author NobuNo
     * @date 2026-07-02
     */
    @Data
    public static class QuestionListDTO {
        private List<InterviewQuestionDTO> questions;
    }


}
