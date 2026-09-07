package com.interview.modules.interview.service;

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

    /**
     * 功能说明
     * <p>初始化 AI 客户端和 Prompt 模板。</p>
     *
     * @param chatClientBuilder     AI 客户端构建器
     * @param resourceLoader        资源加载器
     * @param interviewSkillService 面试方向配置服务
     * @throws IOException 当 Prompt 模板读取失败时抛出
     * @author NobuNo
     * @date 2026-07-02
     */
    public InterviewQuestionService(
            ChatClient.Builder chatClientBuilder,
            ResourceLoader resourceLoader,
            InterviewSkillService interviewSkillService
    ) throws IOException {
        this.chatClient = chatClientBuilder.build();
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
     * @param strResumeText    简历文本
     * @param intQuestionCount 题目数量
     * @return 面试题列表
     * @author NobuNo
     * @date 2026-07-02
     */
    public List<InterviewQuestionDTO> generateQuestions(String strResumeText, Integer intQuestionCount) {
        // 未指定面试方向时，默认使用 Java 后端 Skill
        return generateQuestions(strResumeText, intQuestionCount, "java-backend");
    }

    /**
     * 功能说明
     * <p>根据面试方向生成面试题。</p>
     *
     * @param strResumeText    简历文本
     * @param intQuestionCount 题目数量
     * @param strSkillId       面试方向编号
     * @return 面试题列表
     * @author NobuNo
     * @date 2026-07-02
     */
    public List<InterviewQuestionDTO> generateQuestions(String strResumeText, Integer intQuestionCount, String strSkillId) {
        // 先读取 Skill 配置，后续的 AI 出题和规则兜底都必须使用同一个面试方向。
        InterviewSkillDTO skillDTO = interviewSkillService.getSkill(strSkillId);
        log.debug("开始生成面试题: skillId={}, skillName={}", skillDTO.getId(), skillDTO.getName());
        try {
            // 优先使用 AI 生成问题，便于结合简历内容产生更有针对性的题目。
            List<InterviewQuestionDTO> lstAiQuestionDTO = generateQuestionsByAi(strResumeText, intQuestionCount, skillDTO);

            if (!lstAiQuestionDTO.isEmpty()) {
                // AI 返回的题号、类型或题目数量可能不稳定，先统一整理后再返回。
                return normalizeGeneratedQuestions(
                        lstAiQuestionDTO,
                        strResumeText,
                        intQuestionCount,
                        skillDTO);
            }
        } catch (Exception e) {
            log.warn("AI 出题失败，fallback to rule-based question generation", e);
        }

        // AI 调用失败或返回空结果时，使用规则题保证面试主流程仍然可以继续。
        return generateRuleBasedQuestions(strResumeText, intQuestionCount, skillDTO);
    }

    /**
     * 功能说明
     * <p>调用 AI 生成面试题。</p>
     *
     * @param strResumeText    简历文本
     * @param intQuestionCount 题目数量
     * @param skillDTO         面试方向配置
     * @return AI 生成的题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> generateQuestionsByAi(String strResumeText, Integer intQuestionCount, InterviewSkillDTO skillDTO) {

        // 后端约束题目数量为正数；这里再次设置默认值，保护 Service 被其他调用方直接使用时的行为。
        Integer intSafeQuestionCount = (intQuestionCount == null || intQuestionCount <= 0) ? 3 : intQuestionCount;

        // 根据 Skill 的分类优先级计算每个分类应分配的题目数量，并把结果写入 Prompt。
        Map<String, Integer> mapAllocation = interviewSkillService.calculateAllocation(skillDTO.getCategories(), intSafeQuestionCount);

        Map<String, Object> mapVariables = new HashMap<>();
        mapVariables.put("questionCount", intSafeQuestionCount);
        mapVariables.put("resumeText", strResumeText == null ? "" : strResumeText);

        // 构建面试方向分类说明
        String strSkillCategorySection = buildSkillCategorySection(skillDTO.getCategories(), mapAllocation);

        mapVariables.put("skillCategories", strSkillCategorySection);
        Map<String, Object> mapSystemVariables = new HashMap<>();
        mapSystemVariables.put("persona", skillDTO.getPersona() == null ? "" : skillDTO.getPersona());

        String strSystemPrompt = systemPromptTemplate.render(mapSystemVariables) + "\n\n" + outputConverter.getFormat();
        String strUserPrompt = userPromptTemplate.render(mapVariables);

        // 发起一次同步 AI 请求；调用方会捕获网络异常、认证失败和结构化解析失败。
        String strRawContent = chatClient.prompt()
                .system(strSystemPrompt)
                .user(strUserPrompt)
                .call()
                .content();

        // 将 AI 返回的 JSON 转成 Java DTO，后续才能进行数量规整和业务校验。
        QuestionListDTO cplQuestionListDTO = outputConverter.convert(strRawContent);

        if (cplQuestionListDTO == null || cplQuestionListDTO.getQuestions() == null) {
            return List.of();
        }

        return cplQuestionListDTO.getQuestions();
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
     * <p>规整 AI 返回的题目列表。</p>
     *
     * @param lstAiQuestionDTO AI 题目列表
     * @param strResumeText    简历文本
     * @param intQuestionCount 题目数量
     * @param skillDTO         面试方向配置
     * @return 规整后的题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> normalizeGeneratedQuestions(
            List<InterviewQuestionDTO> lstAiQuestionDTO,
            String strResumeText,
            Integer intQuestionCount,
            InterviewSkillDTO skillDTO) {

        // 无论 AI 返回什么题号，最终都由程序重新按 0、1、2... 编号，保证前端按顺序答题。
        Integer intSafeQuestionCount = intQuestionCount == null || intQuestionCount <= 0 ? 3 : intQuestionCount;

        List<InterviewQuestionDTO> lstNormalizedQuestionDTO = new ArrayList<>();

        for (InterviewQuestionDTO cplQuestionDTO : lstAiQuestionDTO) {
            if (lstNormalizedQuestionDTO.size() >= intSafeQuestionCount) {
                break;
            }

            // 跳过 AI 返回的空对象或空题目，避免无效数据进入会话快照。
            if (cplQuestionDTO == null || cplQuestionDTO.getQuestion() == null
                    || cplQuestionDTO.getQuestion().isBlank()) {
                continue;
            }

            String strType = cplQuestionDTO.getType();
            if (strType == null || strType.isBlank()) {
                strType = "GENERAL";
            }

            String strCategory = cplQuestionDTO.getCategory();
            if (strCategory == null || strCategory.isBlank()) {
                strCategory = "综合能力";
            }

            lstNormalizedQuestionDTO.add(createQuestion(
                    lstNormalizedQuestionDTO.size(),
                    cplQuestionDTO.getQuestion(),
                    strType,
                    strCategory));
        }

        // AI 题目不足时，用当前 Skill 的规则题补齐，确保最终题量满足请求。
        if (lstNormalizedQuestionDTO.size() < intSafeQuestionCount) {
            List<InterviewQuestionDTO> lstRuleBasedQuestionDTO = generateRuleBasedQuestions(
                    strResumeText,
                    intSafeQuestionCount,
                    skillDTO);

            for (InterviewQuestionDTO cplQuestionDTO : lstRuleBasedQuestionDTO) {
                if (lstNormalizedQuestionDTO.size() >= intSafeQuestionCount) {
                    break;
                }

                lstNormalizedQuestionDTO.add(createQuestion(
                        lstNormalizedQuestionDTO.size(),
                        cplQuestionDTO.getQuestion(),
                        cplQuestionDTO.getType(),
                        cplQuestionDTO.getCategory()));
            }
        }

        return lstNormalizedQuestionDTO;
    }

    /**
     * 功能说明
     * <p>生成规则版兜底面试题。</p>
     *
     * @param strResumeText    简历文本
     * @param intQuestionCount 题目数量
     * @param skillDTO         面试方向配置
     * @return 规则版题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> generateRuleBasedQuestions(
            String strResumeText,
            Integer intQuestionCount,
            InterviewSkillDTO skillDTO) {

        // 根据是否存在简历确定问题上下文
        String strExperienceContext =
                strResumeText == null || strResumeText.isBlank()
                        ? "请结合你的实际经历"
                        : "请结合你的简历或项目经历";

        Integer intSafeQuestionCount = null;// 面试问题数量
        if (intQuestionCount == null || intQuestionCount <= 0) {
            intSafeQuestionCount = 3;
        } else {
            intSafeQuestionCount = intQuestionCount;
        }

        // 计算题目分配
        Map<String, Integer> mapAllocation = interviewSkillService.calculateAllocation(skillDTO.getCategories(), intSafeQuestionCount);

        List<InterviewQuestionDTO> lstInterviewQuestionDTO = new ArrayList<>();

        List<String> lstQuestionFocus = List.of(
                "核心原理",
                "项目实践",
                "问题排查",
                "性能优化");

        List<InterviewSkillCategoryDTO> lstCategoryDTO =
                skillDTO.getCategories() == null
                        ? List.of()
                        : skillDTO.getCategories();

        for (InterviewSkillCategoryDTO categoryDTO : lstCategoryDTO) {
            if (categoryDTO == null
                    || categoryDTO.getKey() == null
                    || categoryDTO.getKey().isBlank()
                    || categoryDTO.getLabel() == null
                    || categoryDTO.getLabel().isBlank()) {
                continue;
            }

            Integer intCategoryQuestionCount = mapAllocation.getOrDefault(categoryDTO.getKey(), 0);

            for (int intQuestionIndex = 0; intQuestionIndex < intCategoryQuestionCount; intQuestionIndex++) {

                String strFocus = lstQuestionFocus.get(intQuestionIndex % lstQuestionFocus.size());

                String strQuestion = strExperienceContext + "，谈谈你在“" + categoryDTO.getLabel() + "”方向的" + strFocus + "。";

                lstInterviewQuestionDTO.add(createQuestion(
                        lstInterviewQuestionDTO.size(),
                        strQuestion,
                        categoryDTO.getKey(),
                        categoryDTO.getLabel()));
            }
        }

        String strSkillName = skillDTO.getName() == null ? "当前面试方向" : skillDTO.getName();

        while (lstInterviewQuestionDTO.size() < intSafeQuestionCount) {
            String strFocus = lstQuestionFocus.get(lstInterviewQuestionDTO.size() % lstQuestionFocus.size());

            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    strExperienceContext + "，说明你对" + strSkillName + "岗位" + strFocus + "的理解。",
                    "GENERAL",
                    strSkillName));
        }

        return lstInterviewQuestionDTO;
    }

    /**
     * 功能说明
     * <p>创建单道面试题对象。</p>
     *
     * @param intQuestionIndex 题目索引
     * @param strQuestion      题目内容
     * @param strType          题目类型
     * @param strCategory      题目分类
     * @return 面试题 DTO
     * @author NobuNo
     * @date 2026-07-02
     */
    private InterviewQuestionDTO createQuestion(
            Integer intQuestionIndex,
            String strQuestion,
            String strType,
            String strCategory) {

        InterviewQuestionDTO cplInterviewQuestionDTO = new InterviewQuestionDTO();
        cplInterviewQuestionDTO.setQuestionIndex(intQuestionIndex);
        cplInterviewQuestionDTO.setQuestion(strQuestion);
        cplInterviewQuestionDTO.setType(strType);
        cplInterviewQuestionDTO.setCategory(strCategory);
        return cplInterviewQuestionDTO;
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
