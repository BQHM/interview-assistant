package com.interview.modules.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.skill.InterviewSkillService;
import com.interview.modules.interview.skill.model.InterviewSkillCategoryDTO;
import com.interview.modules.interview.skill.model.InterviewSkillDTO;

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

    @Test
    void generateQuestions_shouldInjectSkillPersonaAndCategoriesIntoPrompt() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewQuestionService interviewQuestionService = createService(chatClient);
        String strAiResponse = """
                {
                  "questions": [
                    {
                      "questionIndex": 0,
                      "question": "AI Skill 注入题",
                      "type": "PROJECT",
                      "category": "项目经验"
                    }
                  ]
                }
                """;

        when(chatClient.prompt()
                .system(argThat((String strPrompt) -> strPrompt.contains("资深 Java 后端面试官")))
                .user(argThat((String strPrompt) -> strPrompt.contains("JAVA")
                        && strPrompt.contains("核心方向，优先覆盖")
                        && strPrompt.contains("PROJECT")
                        && strPrompt.contains("必须至少生成 1 道题")))
                .call()
                .content()).thenReturn(strAiResponse);

        List<InterviewQuestionDTO> lstQuestionDTO = interviewQuestionService.generateQuestions(
                "熟悉 Java，并有真实项目经验。",
                1,
                "java-backend");

        assertThat(lstQuestionDTO).hasSize(1);
        assertThat(lstQuestionDTO.get(0).getQuestion()).isEqualTo("AI Skill 注入题");
    }

    @Test
    void generateQuestions_shouldRejectUnknownSkillBeforeCallingAi() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        InterviewSkillService interviewSkillService = mock(InterviewSkillService.class);
        InterviewQuestionService interviewQuestionService = createService(
                chatClient,
                interviewSkillService);

        when(interviewSkillService.getSkill("unknown"))
                .thenThrow(new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "面试方向不存在: unknown"));

        BusinessException exception = catchThrowableOfType(
                () -> interviewQuestionService.generateQuestions(
                        "熟悉 Spring Boot。",
                        3,
                        "unknown"),
                BusinessException.class);

        assertThat(exception.getCode()).isEqualTo(ErrorCode.BAD_REQUEST.getCode());
        assertThat(exception.getMessage()).contains("面试方向不存在: unknown");
        verify(chatClient, never()).prompt();
    }

    private InterviewQuestionService createService(
            ChatClient chatClient
    ) throws IOException {
        InterviewSkillService interviewSkillService =
                mock(InterviewSkillService.class);

        InterviewSkillDTO skillDTO = new InterviewSkillDTO();
        skillDTO.setId("java-backend");
        skillDTO.setName("Java 后端");
        skillDTO.setPersona("你是一名资深 Java 后端面试官。");
        skillDTO.setCategories(List.of(
                createCategory("JAVA", "Java 基础", "CORE"),
                createCategory("PROJECT", "项目经验", "ALWAYS_ONE")
        ));

        when(interviewSkillService.getSkill("java-backend"))
                .thenReturn(skillDTO);

        return createService(chatClient, interviewSkillService);
    }

    private InterviewQuestionService createService(
            ChatClient chatClient,
            InterviewSkillService interviewSkillService
    ) throws IOException {
        ChatClient.Builder chatClientBuilder =
                mock(ChatClient.Builder.class);

        when(chatClientBuilder.build()).thenReturn(chatClient);

        return new InterviewQuestionService(
                chatClientBuilder,
                createResourceLoader(),
                interviewSkillService
        );
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
                .thenReturn("{persona}");
        when(userPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .thenReturn("请根据简历 {resumeText} 生成 {questionCount} 道题。考察方向：{skillCategories}");

        return resourceLoader;
    }

    private InterviewSkillCategoryDTO createCategory(
            String strKey,
            String strLabel,
            String strPriority
    ) {
        InterviewSkillCategoryDTO categoryDTO = new InterviewSkillCategoryDTO();
        categoryDTO.setKey(strKey);
        categoryDTO.setLabel(strLabel);
        categoryDTO.setPriority(strPriority);
        return categoryDTO;
    }
}
