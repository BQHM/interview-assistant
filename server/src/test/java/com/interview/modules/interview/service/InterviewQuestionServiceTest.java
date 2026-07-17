package com.interview.modules.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.interview.modules.interview.model.dto.InterviewQuestionDTO;

/**
 * 面试题生成服务测试。
 */
class InterviewQuestionServiceTest {

    @Test
    void generateQuestions_shouldUseRuleBasedFallbackWhenAiFails() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        InterviewQuestionService interviewQuestionService = createService(chatClient);

        when(chatClient.prompt()).thenThrow(new IllegalStateException("AI unavailable"));

        List<InterviewQuestionDTO> lstQuestionDTO = interviewQuestionService.generateQuestions(
                "熟悉 Spring Boot、MySQL、Redis、Docker。",
                4);

        assertThat(lstQuestionDTO).hasSize(4);
        assertThat(lstQuestionDTO)
                .extracting(InterviewQuestionDTO::getQuestionIndex)
                .containsExactly(0, 1, 2, 3);
        assertThat(lstQuestionDTO)
                .extracting(InterviewQuestionDTO::getCategory)
                .contains("Spring Boot", "MySQL", "Redis");
    }

    @Test
    void generateQuestions_shouldNormalizeUnstableAiQuestionCount() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewQuestionService interviewQuestionService = createService(chatClient);
        String strAiResponse = """
                {
                  "questions": [
                    {
                      "questionIndex": 5,
                      "question": "AI 题目一",
                      "type": "",
                      "category": ""
                    },
                    {
                      "questionIndex": 9,
                      "question": "AI 题目二",
                      "type": "JAVA",
                      "category": "Java"
                    }
                  ]
                }
                """;

        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn(strAiResponse);

        List<InterviewQuestionDTO> lstQuestionDTO = interviewQuestionService.generateQuestions(
                "熟悉 Spring Boot。",
                4);

        assertThat(lstQuestionDTO).hasSize(4);
        assertThat(lstQuestionDTO)
                .extracting(InterviewQuestionDTO::getQuestionIndex)
                .containsExactly(0, 1, 2, 3);
        assertThat(lstQuestionDTO.get(0).getCategory()).isEqualTo("综合能力");
        assertThat(lstQuestionDTO.get(0).getType()).isEqualTo("GENERAL");
        assertThat(lstQuestionDTO.get(1).getCategory()).isEqualTo("Java");
        assertThat(lstQuestionDTO)
                .extracting(InterviewQuestionDTO::getQuestion)
                .allSatisfy(strQuestion -> assertThat(strQuestion).isNotBlank());
    }

    private InterviewQuestionService createService(ChatClient chatClient) throws IOException {
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        return new InterviewQuestionService(chatClientBuilder, createResourceLoader());
    }

    private ResourceLoader createResourceLoader() throws IOException {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource systemPromptResource = mock(Resource.class);
        Resource userPromptResource = mock(Resource.class);

        when(resourceLoader.getResource("classpath:prompts/interview-question-system.st"))
                .thenReturn(systemPromptResource);
        when(resourceLoader.getResource("classpath:prompts/interview-question-user.st"))
                .thenReturn(userPromptResource);
        when(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .thenReturn("你是一名严格但友好的技术面试官。");
        when(userPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .thenReturn("请根据简历 {resumeText} 生成 {questionCount} 道题。");

        return resourceLoader;
    }
}
