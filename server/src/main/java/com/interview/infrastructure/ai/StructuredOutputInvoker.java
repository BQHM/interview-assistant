package com.interview.infrastructure.ai;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

/**
 * 文件功能说明
 * <p>负责结构化输出调用、重试和最终异常转换。</p>
 *
 * @author NobuNo
 * @date 2026-10-13
 */
@Slf4j
@Component
public class StructuredOutputInvoker {

    // 重试时追加的严格 JSON 指令，用来提醒模型只返回可解析的 JSON。
    private static final String STRICT_JSON_INSTRUCTION = """
            请仅返回可被 JSON 解析器直接解析的 JSON 对象，并严格满足字段结构要求：
            1. 不要输出 Markdown 代码块。
            2. 不要输出解释文字、前后缀或注释。
            3. 所有字符串中的引号必须正确转义。
            """;

    // 最多尝试调用 AI 几次；构造方法会保证该值至少为 1。
    private final int maxAttempts;
    // 第二次调用时，是否把上一次失败原因放进提示词。
    private final boolean includeLastErrorInRetryPrompt;
    // 第二次调用时，是否使用追加了修复要求的提示词。
    private final boolean retryUseRepairPrompt;
    // 修复提示词中，是否追加“只能返回合法 JSON”的严格指令。
    private final boolean retryAppendStrictJsonInstruction;
    // 放进重试提示词中的错误信息最大长度，避免错误文本过长。
    private final int errorMessageMaxLength;

    /**
     * 功能说明
     * <p>读取并保护结构化输出配置。</p>
     *
     * @param properties 结构化输出配置
     * @author NobuNo
     * @date 2026-10-13
     */
    public StructuredOutputInvoker(StructuredOutputProperties properties) {
        // Math.max 用于兜底保护，防止配置成 0 或负数后一次都不调用 AI。
        this.maxAttempts = Math.max(1, properties.getStructuredMaxAttempts());
        // 下面几个开关来自配置对象，决定重试时是否追加错误和严格 JSON 要求。
        this.includeLastErrorInRetryPrompt = properties.isStructuredIncludeLastError();
        this.retryUseRepairPrompt = properties.isStructuredRetryUseRepairPrompt();
        this.retryAppendStrictJsonInstruction = properties.isStructuredRetryAppendStrictJsonInstruction();
        // 错误信息太短没有参考价值，因此至少保留 20 个字符。
        this.errorMessageMaxLength = Math.max(20, properties.getStructuredErrorMessageMaxLength());
    }

    /**
     * 功能说明
     * <p>调用 AI 并把文本结果转换为指定对象，解析失败时按配置重试。</p>
     *
     * @param chatClient            AI 客户端
     * @param systemPromptWithFormat 已包含输出格式说明的 system prompt
     * @param userPrompt             user prompt
     * @param outputConverter        结构化输出转换器
     * @param errorCode              全部尝试失败时使用的错误码
     * @param errorPrefix            业务错误前缀
     * @param logContext             日志上下文
     * @param <T>                    转换后的对象类型
     * @return 转换后的业务对象
     * @throws BusinessException 当所有尝试均失败时抛出
     * @author NobuNo
     * @date 2026-10-13
     */
    public <T> T invoke(
            ChatClient chatClient,
            String systemPromptWithFormat,
            String userPrompt,
            BeanOutputConverter<T> outputConverter,
            ErrorCode errorCode,
            String errorPrefix,
            String logContext
    ) {
        // 保存最近一次失败原因，第二次重试时可以告诉 AI 上次哪里错了。
        Exception lastError = null;

        // attempt 从 1 开始，循环最多执行 maxAttempts 次。
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            // 第一次使用原始提示词；后续尝试才追加修复要求。
            String attemptSystemPrompt = attempt == 1 ? systemPromptWithFormat : buildRetrySystemPrompt(systemPromptWithFormat, lastError);

            try {
                // 向 AI 发起一次同步请求，拿回文本内容。
                String content = chatClient
                        .prompt()
                        .system(attemptSystemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();

                // 空内容无法转换成 Java 对象，因此按失败处理。
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("AI 返回内容为空");
                }

                // 转换成功立即返回，不再继续后续尝试。
                // 转换结果可能为 null，这也按失败处理，交给下一次重试。
                T result = outputConverter.convert(content);
                if(result == null){
                    throw new IllegalStateException("AI 返回内容无法转换为目标对象");
                }

                return result;

            } catch (Exception e) {
                // 记住本次异常，供下一次重试提示词使用。
                lastError = e;

                // 还能重试时使用 warn；已经用完次数时使用 error。
                if (attempt < maxAttempts) {
                    log.warn("{}结构化解析失败，准备重试: attempt={}/{}", logContext, attempt, maxAttempts, e);
                } else {
                    log.error("{}结构化解析失败，已达到最大尝试次数: attempts={}", logContext, maxAttempts, e);
                }
            }
        }

        // 所有尝试都失败后，统一转换为业务异常交给上层处理。
        String message = (errorPrefix == null ? "" : errorPrefix) + "请稍后重试";
        throw new BusinessException(errorCode, message);
    }

    /**
     * 功能说明
     * <p>构造第二次及后续尝试使用的修复提示词。</p>
     *
     * @param systemPromptWithFormat 原始 system prompt
     * @param lastError             上一次失败异常
     * @return 修复后的 system prompt
     * @author NobuNo
     * @date 2026-10-13
     */
    private String buildRetrySystemPrompt(String systemPromptWithFormat, Exception lastError) {
        // 如果关闭修复提示词，重试时继续使用原始提示词。
        if (!retryUseRepairPrompt) {
            return systemPromptWithFormat;
        }

        // 以原始提示词为基础，追加一个空行后再补充修复要求。
        StringBuilder promptBuilder = new StringBuilder(systemPromptWithFormat)
                .append("\n\n");

        // 根据配置决定是否追加严格的 JSON 输出格式要求。
        if (retryAppendStrictJsonInstruction) {
            promptBuilder.append(STRICT_JSON_INSTRUCTION).append('\n');
        }

        // 明确告诉 AI：上一次解析失败，本次只返回合法 JSON。
        promptBuilder.append("上次输出解析失败，请仅返回合法 JSON。");

        // 根据配置决定是否把清理后的失败原因也提供给 AI。
        if (includeLastErrorInRetryPrompt
                && lastError != null
                && lastError.getMessage() != null) {
            promptBuilder.append("\n上次失败原因：")
                    .append(sanitizeErrorMessage(lastError.getMessage()));
        }

        return promptBuilder.toString();
    }

    /**
     * 功能说明
     * <p>把错误信息整理成适合放进提示词的单行文本。</p>
     *
     * @param message 原始错误信息
     * @return 单行且长度受限的错误信息
     * @author NobuNo
     * @date 2026-10-13
     */
    private String sanitizeErrorMessage(String message) {
        // 换行会打乱提示词结构，因此统一替换为空格并去掉首尾空白。
        String oneLineMessage = message.replace('\n', ' ')
                .replace('\r', ' ')
                .trim();

        // 超长错误只保留配置允许的前缀，避免 Prompt 过长。
        if (oneLineMessage.length() > errorMessageMaxLength) {
            return oneLineMessage.substring(0, errorMessageMaxLength) + "...";
        }

        return oneLineMessage;
    }
}