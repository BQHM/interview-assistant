package com.interview.modules.interview.service;

import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
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

    /**
     * 功能说明
     * <p>初始化 AI 客户端和 Prompt 模板。</p>
     *
     * @param chatClientBuilder AI 客户端构建器
     * @param resourceLoader 资源加载器
     * @throws IOException 当 Prompt 模板读取失败时抛出
     * @author NobuNo
     * @date 2026-07-02
     */
    public InterviewQuestionService(ChatClient.Builder chatClientBuilder, ResourceLoader resourceLoader)
            throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(QuestionListDTO.class);
        this.systemPromptTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/interview-question-system.st")
                        .getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/interview-question-user.st")
                        .getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * 功能说明
     * <p>根据简历文本生成面试题。</p>
     *
     * @param strResumeText 简历文本
     * @param intQuestionCount 题目数量
     * @return 面试题列表
     * @author NobuNo
     * @date 2026-07-02
     */
    public List<InterviewQuestionDTO> generateQuestions(String strResumeText, Integer intQuestionCount) {
        try {
            // 尝试使用 AI 生成问题
            List<InterviewQuestionDTO> lstAiQuestionDTO = generateQuestionsByAi(strResumeText, intQuestionCount);

            // 如果 AI 生成的问题列表不为空，则返回 AI 生成的问题列表
            if (!lstAiQuestionDTO.isEmpty()) {
                // 返回归一化处理后的问题列表
                return normalizeGeneratedQuestions(lstAiQuestionDTO, strResumeText, intQuestionCount);
            }
        } catch (Exception e) {
            log.warn("AI 出题失败，fallback to rule-based question generation", e);
        }
        // 如果 AI 生成的问题列表为空，则使用规则生成问题
        return generateRuleBasedQuestions(strResumeText, intQuestionCount);
    }

    /**
     * 功能说明
     * <p>调用 AI 生成面试题。</p>
     *
     * @param strResumeText 简历文本
     * @param intQuestionCount 题目数量
     * @return AI 生成的题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> generateQuestionsByAi(String strResumeText, Integer intQuestionCount) {

        Map<String, Object> mapVariables = new HashMap<>();
        mapVariables.put("questionCount", intQuestionCount == null || intQuestionCount <= 0 ? 3 : intQuestionCount);
        mapVariables.put("resumeText", strResumeText == null ? "" : strResumeText);

        String strSystemPrompt = systemPromptTemplate.render() + "\n\n" + outputConverter.getFormat();
        String strUserPrompt = userPromptTemplate.render(mapVariables);

        String strRawContent = chatClient.prompt()
                .system(strSystemPrompt)
                .user(strUserPrompt)
                .call()
                .content();

        QuestionListDTO cplQuestionListDTO = outputConverter.convert(strRawContent);

        if (cplQuestionListDTO == null || cplQuestionListDTO.getQuestions() == null) {
            return List.of();
        }

        return cplQuestionListDTO.getQuestions();
    }

    /**
     * 功能说明
     * <p>规整 AI 返回的题目列表。</p>
     *
     * @param lstAiQuestionDTO AI 题目列表
     * @param strResumeText 简历文本
     * @param intQuestionCount 题目数量
     * @return 规整后的题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> normalizeGeneratedQuestions(
            List<InterviewQuestionDTO> lstAiQuestionDTO,
            String strResumeText,
            Integer intQuestionCount) {

        Integer intSafeQuestionCount = intQuestionCount == null || intQuestionCount <= 0 ? 3 : intQuestionCount;

        List<InterviewQuestionDTO> lstNormalizedQuestionDTO = new ArrayList<>();

        for (InterviewQuestionDTO cplQuestionDTO : lstAiQuestionDTO) {
            if (lstNormalizedQuestionDTO.size() >= intSafeQuestionCount) {
                break;
            }

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

        if (lstNormalizedQuestionDTO.size() < intSafeQuestionCount) {
            List<InterviewQuestionDTO> lstRuleBasedQuestionDTO = generateRuleBasedQuestions(strResumeText, intSafeQuestionCount);

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
     * @param strResumeText 简历文本
     * @param intQuestionCount 题目数量
     * @return 规则版题目列表
     * @author NobuNo
     * @date 2026-07-02
     */
    private List<InterviewQuestionDTO> generateRuleBasedQuestions(String strResumeText, Integer intQuestionCount) {

        String strSafeResumeText = null;// 简历文本
        if (strResumeText == null) {
            strSafeResumeText = "";
        } else {
            strSafeResumeText = strResumeText;
        }

        Integer intSafeQuestionCount = null;// 面试问题数量
        if (intQuestionCount == null || intQuestionCount <= 0) {
            intSafeQuestionCount = 3;
        } else {
            intSafeQuestionCount = intQuestionCount;
        }
        List<InterviewQuestionDTO> lstInterviewQuestionDTO = new ArrayList<>();

        // 添加通用问题
        lstInterviewQuestionDTO.add(createQuestion(
                0,
                "请你介绍一下自己，并重点说明你在简历中提到的后端项目经验。",
                "GENERAL",
                "综合表达"));

        // 添加 Spring Boot 相关问题
        if (strSafeResumeText.contains("Spring Boot")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请讲一下你在项目中是如何使用 Spring Boot 做模块划分和接口设计的？",
                    "SPRING_BOOT",
                    "Spring Boot"));
        }

        // 添加 MySQL 相关问题
        if (strSafeResumeText.contains("MySQL")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "你在项目中是如何设计 MySQL 索引并做 SQL 优化的？",
                    "MYSQL",
                    "MySQL"));
        }

        // 添加 Redis 相关问题
        if (strSafeResumeText.contains("Redis")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请介绍一下你在项目中使用 Redis 的场景，以及你是如何处理缓存一致性的？",
                    "REDIS",
                    "Redis"));
        }

        // 添加 Docker 相关问题
        if (strSafeResumeText.contains("Docker")) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "你在项目中是怎么使用 Docker 的？它帮你解决了什么问题？",
                    "DOCKER",
                    "Docker"));
        }

        // 添加项目经验相关问题
        while (lstInterviewQuestionDTO.size() < intSafeQuestionCount) {
            lstInterviewQuestionDTO.add(createQuestion(
                    lstInterviewQuestionDTO.size(),
                    "请结合你的项目经历，讲一个你实际解决过的技术问题，以及你的排查和优化过程。",
                    "PROJECT",
                    "项目经验"));
        }

        // 截取所需数量的问题
        if (lstInterviewQuestionDTO.size() > intSafeQuestionCount) {
            return lstInterviewQuestionDTO.subList(0, intSafeQuestionCount);
        }

        return lstInterviewQuestionDTO;
    }

    /**
     * 功能说明
     * <p>创建单道面试题对象。</p>
     *
     * @param intQuestionIndex 题目索引
     * @param strQuestion 题目内容
     * @param strType 题目类型
     * @param strCategory 题目分类
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
