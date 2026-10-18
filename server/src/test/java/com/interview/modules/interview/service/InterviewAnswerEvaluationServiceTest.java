package com.interview.modules.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.interview.infrastructure.ai.StructuredOutputInvoker;
import com.interview.infrastructure.ai.StructuredOutputProperties;
import com.interview.modules.interview.model.dto.InterviewAnswerEvaluationDTO;
import com.interview.modules.interview.model.entity.InterviewAnswerEntity;

/**
 * 面试答案评估服务测试。
 */
class InterviewAnswerEvaluationServiceTest {

    @Test
    void evaluateAnswer_shouldUseRuleBasedFallbackWhenAiFails() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        InterviewAnswerEvaluationService interviewAnswerEvaluationService = createService(chatClient);
        InterviewAnswerEntity answerEntity = createAnswer(
                "我负责项目中的接口设计和异常处理，因为外部服务不稳定，所以实现了兜底逻辑。");

        when(chatClient.prompt()).thenThrow(new IllegalStateException("AI unavailable"));

        InterviewAnswerEvaluationDTO evaluationDTO = interviewAnswerEvaluationService
                .evaluateAnswer(answerEntity);

        assertThat(evaluationDTO.getScore()).isBetween(1, 100);
        assertThat(evaluationDTO.getFeedback()).isNotBlank();
        assertThat(evaluationDTO.getReferenceAnswer()).isNotBlank();
        assertThat(evaluationDTO.getKeyPoints()).isNotEmpty();
    }

    @Test
    void evaluateAnswer_shouldReturnScoreFeedbackReferenceAnswerAndKeyPoints() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewAnswerEvaluationService interviewAnswerEvaluationService = createService(chatClient);
        InterviewAnswerEntity answerEntity = createAnswer(
                "我会结合项目场景说明状态流转、事务边界和异常兜底。");
        String aiResponse = """
                {
                  "score": 86,
                  "feedback": "回答结构清晰，能结合项目说明。",
                  "referenceAnswer": "可以从状态机、事务和并发控制展开回答。",
                  "keyPoints": ["状态机", "事务边界", "并发控制"]
                }
                """;

        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn(aiResponse);

        InterviewAnswerEvaluationDTO evaluationDTO = interviewAnswerEvaluationService
                .evaluateAnswer(answerEntity);

        assertThat(evaluationDTO.getScore()).isEqualTo(86);
        assertThat(evaluationDTO.getFeedback()).isEqualTo("回答结构清晰，能结合项目说明。");
        assertThat(evaluationDTO.getReferenceAnswer())
                .isEqualTo("可以从状态机、事务和并发控制展开回答。");
        assertThat(evaluationDTO.getKeyPoints())
                .containsExactly("状态机", "事务边界", "并发控制");
    }

    @Test
    void evaluateAnswer_shouldRetryWhenFirstAttemptReturnsInvalidJson() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewAnswerEvaluationService interviewAnswerEvaluationService = createService(chatClient);
        InterviewAnswerEntity answerEntity = createAnswer(
                "我会结合项目场景说明状态流转、事务边界和异常兜底。");
        String aiResponse = """
                {
                  "score": 86,
                  "feedback": "回答结构清晰，能结合项目说明。",
                  "referenceAnswer": "可以从状态机、事务和并发控制展开回答。",
                  "keyPoints": ["状态机", "事务边界", "并发控制"]
                }
                """;

        // 第一次返回非法 JSON，第二次返回合法 JSON；只有重试生效，最终才能拿到 AI 给出的 86 分。
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn("这不是合法的 JSON", aiResponse);

        InterviewAnswerEvaluationDTO evaluationDTO = interviewAnswerEvaluationService
                .evaluateAnswer(answerEntity);

        // 规则版评分只会给出固定档位，86 分只可能来自 AI 评估，因此这个断言同时证明了重试成功。
        assertThat(evaluationDTO.getScore()).isEqualTo(86);
    }

    private InterviewAnswerEvaluationService createService(ChatClient chatClient) throws IOException {
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        return new InterviewAnswerEvaluationService(
                chatClientBuilder,
                createResourceLoader(),
                new StructuredOutputInvoker(new StructuredOutputProperties()));
    }

    private ResourceLoader createResourceLoader() throws IOException {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource systemPromptResource = mock(Resource.class);
        Resource userPromptResource = mock(Resource.class);

        when(resourceLoader.getResource("classpath:prompts/interview-answer-evaluation-system.st"))
                .thenReturn(systemPromptResource);
        when(resourceLoader.getResource("classpath:prompts/interview-answer-evaluation-user.st"))
                .thenReturn(userPromptResource);
        when(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .thenReturn("你是一名 Java 技术面试官。");
        when(userPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .thenReturn("分类：{category}\n题目：{question}\n回答：{userAnswer}");

        return resourceLoader;
    }

    private InterviewAnswerEntity createAnswer(String userAnswer) {
        InterviewAnswerEntity answerEntity = new InterviewAnswerEntity();
        answerEntity.setId(1L);
        answerEntity.setQuestionIndex(0);
        answerEntity.setCategory("项目经验");
        answerEntity.setQuestion("请说明你如何处理项目中的异常兜底。");
        answerEntity.setUserAnswer(userAnswer);
        return answerEntity;
    }
}
