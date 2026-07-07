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
 * 文件功能说明
 * <p>负责面试答案评估业务逻辑。</p>
 *
 * @author NobuNo
 * @since 2026-06-29
 */
@Service
public class InterviewAnswerEvaluationService {

    /**
     * AI 聊天客户端。
     */
    private final ChatClient chatClient;

    /**
     * 系统 Prompt 模板。
     */
    private final PromptTemplate systemPromptTemplate;

    /**
     * 用户 Prompt 模板。
     */
    private final PromptTemplate userPromptTemplate;

    /**
     * AI 输出转换器。
     */
    private final BeanOutputConverter<InterviewAnswerEvaluationDTO> outputConverter;

    private static final Logger log = LoggerFactory.getLogger(InterviewAnswerEvaluationService.class);

    /**
     * 功能说明
     * <p>初始化 AI 客户端和 Prompt 模板。</p>
     *
     * @param chatClientBuilder AI 客户端构建器
     * @param resourceLoader 资源加载器
     * @throws IOException 当 Prompt 模板读取失败时抛出
     * @author NobuNo
     * @since 2026-06-29
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
     * 功能说明
     * <p>评估单题答案。</p>
     *
     * @param tblInterviewAnswerEntity 答案实体
     * @return 答案评估结果
     * @author NobuNo
     * @since 2026-06-29
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
     * 功能说明
     * <p>构建空答案评估结果。</p>
     *
     * @return 空答案评估结果
     * @author NobuNo
     * @since 2026-06-29
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
     * 功能说明
     * <p>调用 AI 评估答案。</p>
     *
     * @param tblInterviewAnswerEntity 答案实体
     * @return AI 评估结果
     * @author NobuNo
     * @since 2026-06-29
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
     * 功能说明
     * <p>构建规则版评估结果。</p>
     *
     * @param tblInterviewAnswerEntity 答案实体
     * @return 规则版评估结果
     * @author NobuNo
     * @since 2026-06-29
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
     * 功能说明
     * <p>计算规则版评分。</p>
     *
     * @param strUserAnswer 用户答案
     * @return 规则版评分
     * @author NobuNo
     * @since 2026-06-29
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
     * 功能说明
     * <p>判断文本是否包含任意关键词。</p>
     *
     * @param strText 文本内容
     * @param arrKeywords 关键词数组
     * @return 是否包含关键词
     * @author NobuNo
     * @since 2026-06-29
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
     * 功能说明
     * <p>根据评分生成反馈。</p>
     *
     * @param intScore 评分
     * @return 文字反馈
     * @author NobuNo
     * @since 2026-06-29
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
