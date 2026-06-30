package com.interview.modules.interview.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.interview.modules.interview.model.dto.InterviewAnswerEvaluationDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;

/**
 * 面试单题答案评估服务。
 *
 * 当前阶段它只负责一件事：
 * 接收一条 interview_answers 表中的答案记录，然后生成这道题的评估结果。
 *
 * 这里暂时不直接保存数据库，是为了让职责更单一：
 * - 本类负责“怎么评估”
 * - 调用方负责“评估结果保存到哪里”
 *
 * 整体流程：
 * 1. 先检查答案是否为空。
 * 2. 答案不为空时，优先调用 AI。
 * 3. 如果 AI 调用失败，使用本地规则兜底。
 * 4. 最终统一返回 InterviewAnswerEvaluationDTO。
 */
@Service
public class InterviewAnswerEvaluationService {

    /**
     * Spring AI 的聊天客户端，真正负责向大模型发送请求。
     */
    private final ChatClient chatClient;

    /**
     * system prompt 模板。
     * 用来告诉 AI：你是谁、你要按什么标准评分、输出什么格式。
     */
    private final PromptTemplate systemPromptTemplate;

    /**
     * user prompt 模板。
     * 用来放入本次评估的具体题目、分类和用户答案。
     */
    private final PromptTemplate userPromptTemplate;

    /**
     * AI 结构化输出转换器。
     *
     * 它有两个作用：
     * 1. outputConverter.getFormat() 会生成一段格式要求，提示 AI 按 DTO 字段返回 JSON。
     * 2. outputConverter.convert(...) 会把 AI 返回的 JSON 字符串转成 InterviewAnswerEvaluationDTO。
     */
    private final BeanOutputConverter<InterviewAnswerEvaluationDTO> outputConverter;

    private static final Logger log = LoggerFactory.getLogger(InterviewAnswerEvaluationService.class);

    /**
     * 构造方法在 Spring 创建 Bean 的时候执行。
     *
     * 这里提前完成三件事：
     * 1. 创建 ChatClient。
     * 2. 创建 DTO 输出转换器。
     * 3. 从 resources/prompts 目录加载两个 prompt 模板。
     *
     * 这样后面每次评估答案时，就不用重复读取模板文件。
     */
    public InterviewAnswerEvaluationService(ChatClient.Builder chatClientBuilder, ResourceLoader resourceLoader)
            throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(InterviewAnswerEvaluationDTO.class);
        this.systemPromptTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/interview-answer-evaluation-system.st")
                        .getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(
                resourceLoader.getResource("classpath:prompts/interview-answer-evaluation-user.st")
                        .getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * 对外暴露的评估入口。
     *
     * 其他 Service 后续只需要调用这个方法，不需要关心底层到底是 AI 评分还是规则评分。
     *
     * @param tblInterviewAnswerEntity 一道题的答案记录
     * @return 单题评估结果
     */
    public InterviewAnswerEvaluationDTO evaluateAnswer(InterviewAnswerEntity tblInterviewAnswerEntity) {
        // 极端保护：如果调用方传入 null，就按“未作答”处理，避免空指针异常。
        if (tblInterviewAnswerEntity == null) {
            return buildEmptyAnswerEvaluation();
        }

        // 没有用户答案时，不需要调用 AI，直接返回 0 分兜底结果。
        String strUserAnswer = tblInterviewAnswerEntity.getUserAnswer();
        if (strUserAnswer == null || strUserAnswer.trim().isEmpty()) {
            return buildEmptyAnswerEvaluation();
        }

        log.info(
                "开始评估面试答案: answerId={}, questionIndex={}",
                tblInterviewAnswerEntity.getId(),
                tblInterviewAnswerEntity.getQuestionIndex());

        try {
            // 主链路：优先让 AI 根据题目和用户答案生成结构化评估。
            InterviewAnswerEvaluationDTO cplInterviewAnswerEvaluationDTO = evaluateByAi(tblInterviewAnswerEntity);

            log.info(
                    "AI 面试答案评估成功: answerId={}, score={}",
                    tblInterviewAnswerEntity.getId(),
                    cplInterviewAnswerEvaluationDTO.getScore());

            return cplInterviewAnswerEvaluationDTO;
        } catch (Exception e) {
            // AI 失败不能影响主流程，所以这里只记录日志，然后切换到规则版评分。
            // 常见失败原因：API Key 失效、网络异常、AI 返回格式不是 JSON、JSON 转 DTO 失败。
            log.warn(
                    "AI 面试答案评估失败，fallback to rule-based evaluation: answerId={}",
                    tblInterviewAnswerEntity.getId(),
                    e);

            return buildRuleBasedEvaluation(tblInterviewAnswerEntity);
        }
    }

    /**
     * AI 评估主逻辑。
     *
     * 这一步只做“调用 AI 并把结果转成 DTO”，不处理数据库保存。
     */
    private InterviewAnswerEvaluationDTO evaluateByAi(InterviewAnswerEntity tblInterviewAnswerEntity) {
        // 渲染 system prompt。当前 system prompt 没有变量，所以直接 render()。
        String strSystemPrompt = systemPromptTemplate.render();

        // 给 user prompt 准备变量。
        // 这些 key 必须和 interview-answer-evaluation-user.st 里的 {category}/{question}/{userAnswer} 对上。
        Map<String, Object> mapVariables = new HashMap<>();
        mapVariables.put("category", tblInterviewAnswerEntity.getCategory());
        mapVariables.put("question", tblInterviewAnswerEntity.getQuestion());
        mapVariables.put("userAnswer", tblInterviewAnswerEntity.getUserAnswer());

        // 把变量填入 user prompt，形成真正发给 AI 的用户消息。
        String strUserPrompt = userPromptTemplate.render(mapVariables);

        // 把 BeanOutputConverter 生成的格式说明追加到 system prompt 后面。
        // 这样 AI 更容易返回符合 InterviewAnswerEvaluationDTO 结构的 JSON。
        String strSystemPromptWithFormat = strSystemPrompt + "\n\n" + outputConverter.getFormat();

        // 发送请求给 AI，并拿到原始文本响应。
        String strRawContent = chatClient.prompt()
                .system(strSystemPromptWithFormat)
                .user(strUserPrompt)
                .call()
                .content();

        // 开发阶段保留原始响应日志，便于排查“AI 返回格式不对”的问题。
        // 注意：这里不要记录用户隐私过多的内容；当前项目是学习阶段，后续生产化需要收敛日志。
        log.info("Interview answer evaluation raw AI response: 【{}】", strRawContent);

        // 把 AI 返回的 JSON 字符串转换成 Java DTO。
        InterviewAnswerEvaluationDTO cplInterviewAnswerEvaluationDTO = outputConverter.convert(strRawContent);

        // convert 失败时可能得到 null，这里主动抛异常，让外层 catch 后走规则兜底。
        if (cplInterviewAnswerEvaluationDTO == null) {
            throw new IllegalStateException("AI 面试答案评估结果为空");
        }

        return cplInterviewAnswerEvaluationDTO;
    }

    /**
     * 规则版兜底评估。
     *
     * 什么时候会走到这里：
     * - AI 服务不可用
     * - API Key 失效
     * - AI 返回内容不是合法 JSON
     * - 输出转换器转换失败
     *
     * 这个方法是过渡方案，不追求很准，只保证系统在 AI 失败时仍然能生成报告。
     */
    private InterviewAnswerEvaluationDTO buildRuleBasedEvaluation(InterviewAnswerEntity tblInterviewAnswerEntity) {
        InterviewAnswerEvaluationDTO cplInterviewAnswerEvaluationDTO = new InterviewAnswerEvaluationDTO();

        String strUserAnswer = tblInterviewAnswerEntity.getUserAnswer();
        int intScore = calculateRuleBasedScore(strUserAnswer);

        cplInterviewAnswerEvaluationDTO.setScore(intScore);
        cplInterviewAnswerEvaluationDTO.setFeedback(buildRuleBasedFeedback(intScore));
        cplInterviewAnswerEvaluationDTO.setReferenceAnswer("建议围绕题目核心概念、项目实践、问题处理过程和最终效果进行回答。");
        cplInterviewAnswerEvaluationDTO.setKeyPoints(List.of(
                "说明核心概念",
                "结合项目场景",
                "描述具体做法",
                "补充问题处理和优化效果"));

        return cplInterviewAnswerEvaluationDTO;
    }

    /**
     * 空答案评估。
     *
     * 用户没有回答时，不需要浪费一次 AI 调用，直接给固定的 0 分反馈。
     */
    private InterviewAnswerEvaluationDTO buildEmptyAnswerEvaluation() {
        InterviewAnswerEvaluationDTO cplInterviewAnswerEvaluationDTO = new InterviewAnswerEvaluationDTO();

        cplInterviewAnswerEvaluationDTO.setScore(0);
        cplInterviewAnswerEvaluationDTO.setFeedback("当前题目未作答，无法体现相关技术理解和项目经验。");
        cplInterviewAnswerEvaluationDTO.setReferenceAnswer("建议先正面回答题目，再结合项目中的具体场景、实现方式和结果进行说明。");
        cplInterviewAnswerEvaluationDTO.setKeyPoints(List.of(
                "正面回答问题",
                "说明核心知识点",
                "结合项目经验",
                "表达清晰完整"));

        return cplInterviewAnswerEvaluationDTO;
    }

    /**
     * 规则版评分。
     *
     * 当前只是一个非常粗略的临时算法：
     * 1. 先根据答案长度给基础分。
     * 2. 如果提到项目、负责、设计、优化等实践词，加一点分。
     * 3. 如果表达上有首先、其次、因为、所以等结构词，再加一点分。
     *
     * 后续接入稳定 AI 评估后，这个方法只作为兜底保留。
     */
    private int calculateRuleBasedScore(String strUserAnswer) {
        if (strUserAnswer == null || strUserAnswer.trim().isEmpty()) {
            return 0;
        }

        String strTrimmedAnswer = strUserAnswer.trim();

        int intScore;

        // 第一步：按答案长度给基础分。
        if (strTrimmedAnswer.length() < 20) {
            intScore = 30;
        } else if (strTrimmedAnswer.length() < 80) {
            intScore = 60;
        } else if (strTrimmedAnswer.length() < 160) {
            intScore = 75;
        } else {
            intScore = 85;
        }

        // 第二步：如果答案能结合项目或具体动作，说明不是纯概念背诵，适当加分。
        if (containsAny(strTrimmedAnswer, "项目", "负责", "设计", "实现", "优化", "排查")) {
            intScore += 5;
        }

        // 第三步：如果答案有表达结构，说明候选人回答更有条理，适当加分。
        if (containsAny(strTrimmedAnswer, "因为", "所以", "首先", "其次", "最后", "例如")) {
            intScore += 5;
        }

        // 防止加分后超过 100。
        return Math.min(intScore, 100);
    }

    /**
     * 判断文本中是否包含任意一个关键词。
     *
     * 用显式 for 循环写，是为了保持当前学习阶段代码直观易懂。
     */
    private boolean containsAny(String strText, String... arrKeywords) {
        for (String strKeyword : arrKeywords) {
            if (strText.contains(strKeyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据规则评分生成对应的文字反馈。
     */
    private String buildRuleBasedFeedback(int intScore) {
        if (intScore >= 85) {
            return "回答较完整，能够体现一定的项目经验和技术理解，建议继续补充更多边界情况和优化细节。";
        } else if (intScore >= 70) {
            return "回答基本完整，具备一定条理，但还可以进一步补充原理、场景和项目结果。";
        } else if (intScore >= 50) {
            return "回答覆盖了部分内容，但整体偏简略，建议补充具体做法和项目经验。";
        } else {
            return "回答较少或不够相关，建议先围绕题目核心概念进行正面回答。";
        }
    }
}