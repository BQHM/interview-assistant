package com.interview.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;

/**
 * 结构化输出调用器测试。
 */
class StructuredOutputInvokerTest {

    private static final ErrorCode ERROR_CODE = ErrorCode.AI_STRUCTURED_OUTPUT_FAILED;
    private static final String ERROR_PREFIX = "AI 出题结构化解析失败：";
    private static final String LOG_CONTEXT = "AI 出题";
    private static final String SYSTEM_PROMPT = "SYSTEM_PROMPT_WITH_FORMAT";
    private static final String USER_PROMPT = "USER_PROMPT";
    private static final String VALID_JSON = "{\"topic\":\"ok\"}";
    private static final String INVALID_JSON = "这不是合法的 JSON";

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private BeanOutputConverter<TestPayload> outputConverter;
    private StructuredOutputInvoker invoker;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        outputConverter = new BeanOutputConverter<>(TestPayload.class);
        invoker = new StructuredOutputInvoker(new StructuredOutputProperties());
    }

    @Test
    void invoke_shouldReturnConvertedResultWhenFirstAttemptSucceeds() {
        stubAiContents(VALID_JSON);

        TestPayload payload = invoke();

        assertThat(payload.getTopic()).isEqualTo("ok");
        verify(callResponseSpec, times(1)).content();
        verify(requestSpec, times(1)).system(SYSTEM_PROMPT);
    }

    @Test
    void invoke_shouldRetryWithStricterPromptWhenFirstAttemptReturnsInvalidJson() {
        stubAiContents(INVALID_JSON, VALID_JSON);

        TestPayload payload = invoke();

        assertThat(payload.getTopic()).isEqualTo("ok");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec, times(2)).system(systemPromptCaptor.capture());

        // 第一次必须原样使用传入的 prompt，否则会破坏调用方已有的 Prompt 结构。
        assertThat(systemPromptCaptor.getAllValues().get(0)).isEqualTo(SYSTEM_PROMPT);
        // 第二次必须在原 prompt 基础上追加严格 JSON 指令和上一次的失败原因。
        assertThat(systemPromptCaptor.getAllValues().get(1))
                .startsWith(SYSTEM_PROMPT)
                .contains("不要输出 Markdown 代码块")
                .contains("上次输出解析失败，请仅返回合法 JSON")
                .contains("上次失败原因：");
    }

    @Test
    void invoke_shouldThrowBusinessExceptionWhenAllAttemptsFail() {
        stubAiContents(INVALID_JSON, INVALID_JSON);

        BusinessException exception = catchThrowableOfType(
                () -> invoke(),
                BusinessException.class);

        assertThat(exception.getCode()).isEqualTo(ERROR_CODE.getCode());
        assertThat(exception.getMessage()).isEqualTo(ERROR_PREFIX + "请稍后重试");
        verify(callResponseSpec, times(2)).content();
    }

    @Test
    void invoke_shouldAttemptOnlyOnceWhenStructuredMaxAttemptsIsOne() {
        stubAiContents(INVALID_JSON, VALID_JSON);

        StructuredOutputProperties properties = new StructuredOutputProperties();
        properties.setStructuredMaxAttempts(1);
        StructuredOutputInvoker cplSingleAttemptInvoker = new StructuredOutputInvoker(properties);

        BusinessException exception = catchThrowableOfType(
                () -> invoke(cplSingleAttemptInvoker),
                BusinessException.class);

        assertThat(exception.getCode()).isEqualTo(ERROR_CODE.getCode());
        verify(callResponseSpec, times(1)).content();
        verify(requestSpec, times(1)).system(SYSTEM_PROMPT);
    }

    @Test
    void invoke_shouldRetryWhenConverterReturnsNull() {
        // "null" 不是空白内容，但无法转换成业务对象，必须按失败处理并重试。
        stubAiContents("null", VALID_JSON);

        TestPayload payload = invoke();

        assertThat(payload.getTopic()).isEqualTo("ok");
        verify(requestSpec, times(2)).system(anyString());
    }

    @Test
    void invoke_shouldRetryWhenAiReturnsBlankContent() {
        stubAiContents("   ", VALID_JSON);

        TestPayload payload = invoke();

        assertThat(payload.getTopic()).isEqualTo("ok");
        verify(callResponseSpec, times(2)).content();
    }

    @Test
    void invoke_shouldKeepOriginalPromptOnRetryWhenRepairPromptDisabled() {
        StructuredOutputProperties properties = new StructuredOutputProperties();
        properties.setStructuredRetryUseRepairPrompt(false);
        StructuredOutputInvoker cplPlainRetryInvoker = new StructuredOutputInvoker(properties);

        stubAiContents(INVALID_JSON, VALID_JSON);

        TestPayload payload = invoke(cplPlainRetryInvoker);

        assertThat(payload.getTopic()).isEqualTo("ok");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec, times(2)).system(systemPromptCaptor.capture());
        assertThat(systemPromptCaptor.getAllValues())
                .containsExactly(SYSTEM_PROMPT, SYSTEM_PROMPT);
    }

    private TestPayload invoke() {
        return invoke(invoker);
    }

    private TestPayload invoke(StructuredOutputInvoker cplInvoker) {
        return cplInvoker.invoke(
                chatClient,
                SYSTEM_PROMPT,
                USER_PROMPT,
                outputConverter,
                ERROR_CODE,
                ERROR_PREFIX,
                LOG_CONTEXT);
    }

    private void stubAiContents(String... strAiContents) {
        when(callResponseSpec.content()).thenReturn(
                strAiContents[0],
                Arrays.copyOfRange(strAiContents, 1, strAiContents.length));
    }

    /**
     * 测试用的结构化结果载体。
     */
    static class TestPayload {

        private String topic;

        public String getTopic() {
            return topic;
        }

        public void setTopic(String strTopic) {
            this.topic = strTopic;
        }
    }
}
