package com.interview.modules.interview.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.interview.common.exception.ErrorCode;
import com.interview.infrastructure.ai.StructuredOutputInvoker;
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
 * @date 2026-06-29
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

    /**
     * 结构化输出调用器。
     */
    private final StructuredOutputInvoker structuredOutputInvoker;

    private static final Logger log = LoggerFactory.getLogger(InterviewAnswerEvaluationService.class);

    /**
     * 功能说明
     * <p>初始化 AI 客户端和 Prompt 模板。</p>
     *
     * @param chatClientBuilder AI 客户端构建器
     * @param resourceLoader 资源加载器
     * @param structuredOutputInvoker 结构化输出调用器
     * @throws IOException 当 Prompt 模板读取失败时抛出
     * @author NobuNo
     * @date 2026-06-29
     */
    public InterviewAnswerEvaluationService(ChatClient.Builder chatClientBuilder, ResourceLoader resourceLoader, StructuredOutputInvoker structuredOutputInvoker)
            throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
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
     * @param interviewAnswerEntity 答案实体
     * @return 答案评估结果
     * @author NobuNo
     * @date 2026-06-29
     */
    public InterviewAnswerEvaluationDTO evaluateAnswer(InterviewAnswerEntity interviewAnswerEntity) {
        // 极端保护：如果调用方传入 null，就按“未作答”处理，避免空指针异常。
        if (interviewAnswerEntity == null) {
            return buildEmptyAnswerEvaluation();
        }

        // 没有用户答案时，不需要调用 AI，直接返回 0 分兜底结果。
        String userAnswer = interviewAnswerEntity.getUserAnswer();
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return buildEmptyAnswerEvaluation();
        }

        log.info(
                "开始评估面试答案: answerId={}, questionIndex={}",
                interviewAnswerEntity.getId(),
                interviewAnswerEntity.getQuestionIndex());

        try {
            // 主链路：优先让 AI 根据题目和用户答案生成结构化评估。
            InterviewAnswerEvaluationDTO interviewAnswerEvaluationDTO = evaluateByAi(interviewAnswerEntity);

            log.info(
                    "AI 面试答案评估成功: answerId={}, score={}",
                    interviewAnswerEntity.getId(),
                    interviewAnswerEvaluationDTO.getScore());

            return interviewAnswerEvaluationDTO;
        } catch (Exception e) {
            // AI 失败不能影响主流程，所以这里只记录日志，然后切换到规则版评分。
            // 常见失败原因：API Key 失效、网络异常、AI 返回格式不是 JSON、JSON 转 DTO 失败。
            log.warn(
                    "AI 面试答案评估失败，fallback to rule-based evaluation: answerId={}",
                    interviewAnswerEntity.getId(),
                    e);

            return buildRuleBasedEvaluation(interviewAnswerEntity);
        }
    }

    /**
     * 功能说明
     * <p>构建空答案评估结果。</p>
     *
     * @return 空答案评估结果
     * @author NobuNo
     * @date 2026-06-29
     */
    private InterviewAnswerEvaluationDTO buildEmptyAnswerEvaluation() {
        InterviewAnswerEvaluationDTO interviewAnswerEvaluationDTO = new InterviewAnswerEvaluationDTO();

        interviewAnswerEvaluationDTO.setScore(0);
        interviewAnswerEvaluationDTO.setFeedback("当前题目未作答，无法体现相关技术理解和项目经验。");
        interviewAnswerEvaluationDTO.setReferenceAnswer("建议先正面回答题目，再结合项目中的具体场景、实现方式和结果进行说明。");
        interviewAnswerEvaluationDTO.setKeyPoints(List.of(
                "正面回答问题",
                "说明核心知识点",
                "结合项目经验",
                "表达清晰完整"));

        return interviewAnswerEvaluationDTO;
    }

    /**
     * 功能说明
     * <p>调用 AI 评估答案。</p>
     *
     * @param interviewAnswerEntity 答案实体
     * @return AI 评估结果
     * @author NobuNo
     * @date 2026-06-29
     */
    private InterviewAnswerEvaluationDTO evaluateByAi(InterviewAnswerEntity interviewAnswerEntity) {
        // 渲染 system prompt。当前 system prompt 没有变量，所以直接 render()。
        String systemPrompt = systemPromptTemplate.render();

        // 给 user prompt 准备变量。
        // 这些 key 必须和 interview-answer-evaluation-user.st 里的 {category}/{question}/{userAnswer} 对上。
        Map<String, Object> variables = new HashMap<>();
        variables.put("category", interviewAnswerEntity.getCategory());
        variables.put("question", interviewAnswerEntity.getQuestion());
        variables.put("userAnswer", interviewAnswerEntity.getUserAnswer());

        // 把变量填入 user prompt，形成真正发给 AI 的用户消息。
        String userPrompt = userPromptTemplate.render(variables);

        // 把 BeanOutputConverter 生成的格式说明追加到 system prompt 后面。
        // 这样 AI 更容易返回符合 InterviewAnswerEvaluationDTO 结构的 JSON。
        String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();

        // 交给统一的结构化输出调用器：失败会按配置重试；
        // 重试仍失败抛 BusinessException，由 evaluateAnswer 的 catch 降级到规则评分。
        InterviewAnswerEvaluationDTO interviewAnswerEvaluationDTO = structuredOutputInvoker.invoke(
                chatClient, systemPromptWithFormat, userPrompt, outputConverter,
                ErrorCode.AI_STRUCTURED_OUTPUT_FAILED, "AI 答案评估结构化解析失败：", "AI 答案评估");

        return interviewAnswerEvaluationDTO;
    }

    /**
     * 功能说明
     * <p>构建规则版评估结果。</p>
     *
     * @param interviewAnswerEntity 答案实体
     * @return 规则版评估结果
     * @author NobuNo
     * @date 2026-06-29
     */
    private InterviewAnswerEvaluationDTO buildRuleBasedEvaluation(InterviewAnswerEntity interviewAnswerEntity) {
        InterviewAnswerEvaluationDTO interviewAnswerEvaluationDTO = new InterviewAnswerEvaluationDTO();

        String userAnswer = interviewAnswerEntity.getUserAnswer();
        int score = calculateRuleBasedScore(userAnswer);

        interviewAnswerEvaluationDTO.setScore(score);
        interviewAnswerEvaluationDTO.setFeedback(buildRuleBasedFeedback(score));
        interviewAnswerEvaluationDTO.setReferenceAnswer("建议围绕题目核心概念、项目实践、问题处理过程和最终效果进行回答。");
        interviewAnswerEvaluationDTO.setKeyPoints(List.of(
                "说明核心概念",
                "结合项目场景",
                "描述具体做法",
                "补充问题处理和优化效果"));

        return interviewAnswerEvaluationDTO;
    }

    /**
     * 功能说明
     * <p>计算规则版评分。</p>
     *
     * @param userAnswer 用户答案
     * @return 规则版评分
     * @author NobuNo
     * @date 2026-06-29
     */
    private int calculateRuleBasedScore(String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return 0;
        }

        String trimmedAnswer = userAnswer.trim();

        int score;

        // 第一步：按答案长度给基础分。
        if (trimmedAnswer.length() < 20) {
            score = 30;
        } else if (trimmedAnswer.length() < 80) {
            score = 60;
        } else if (trimmedAnswer.length() < 160) {
            score = 75;
        } else {
            score = 85;
        }

        // 第二步：如果答案能结合项目或具体动作，说明不是纯概念背诵，适当加分。
        if (containsAny(trimmedAnswer, "项目", "负责", "设计", "实现", "优化", "排查")) {
            score += 5;
        }

        // 第三步：如果答案有表达结构，说明候选人回答更有条理，适当加分。
        if (containsAny(trimmedAnswer, "因为", "所以", "首先", "其次", "最后", "例如")) {
            score += 5;
        }

        // 防止加分后超过 100。
        return Math.min(score, 100);
    }

    /**
     * 功能说明
     * <p>判断文本是否包含任意关键词。</p>
     *
     * @param text 文本内容
     * @param keywords 关键词数组
     * @return 是否包含关键词
     * @author NobuNo
     * @date 2026-06-29
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能说明
     * <p>根据评分生成反馈。</p>
     *
     * @param score 评分
     * @return 文字反馈
     * @author NobuNo
     * @date 2026-06-29
     */
    private String buildRuleBasedFeedback(int score) {
        if (score >= 85) {
            return "回答较完整，能够体现一定的项目经验和技术理解，建议继续补充更多边界情况和优化细节。";
        } else if (score >= 70) {
            return "回答基本完整，具备一定条理，但还可以进一步补充原理、场景和项目结果。";
        } else if (score >= 50) {
            return "回答覆盖了部分内容，但整体偏简略，建议补充具体做法和项目经验。";
        } else {
            return "回答较少或不够相关，建议先围绕题目核心概念进行正面回答。";
        }
    }
}
