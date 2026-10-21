package com.interview.modules.interview.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
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
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.infrastructure.ai.StructuredOutputInvoker;
import com.interview.infrastructure.ai.StructuredOutputProperties;
import com.interview.modules.interview.model.HistoricalQuestion;
import com.interview.modules.interview.model.dto.InterviewQuestionDTO;
import com.interview.modules.interview.skill.InterviewSkillService;
import com.interview.modules.interview.skill.model.InterviewSkillCategoryDTO;
import com.interview.modules.interview.skill.model.InterviewSkillDTO;

/**
 * 面试题生成服务测试。
 */
class InterviewQuestionServiceTest {

    /**
     * 固定的合法 AI 出题响应，用于只关心 Prompt 内容的测试。
     */
    private static final String AI_QUESTION_RESPONSE = """
            {
              "questions": [
                {
                  "questionIndex": 0,
                  "question": "请说明历史考点注入后的面试题。",
                  "type": "JAVA",
                  "category": "Java 基础",
                  "topicSummary": "历史考点注入"
                }
              ]
            }
            """;

    @Test
    void generateQuestions_shouldUseSkillBasedFallbackWhenAiFails() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        InterviewQuestionService interviewQuestionService = createService(chatClient);

        when(chatClient.prompt()).thenThrow(new IllegalStateException("AI unavailable"));

        List<InterviewQuestionDTO> questionDTOs = interviewQuestionService.generateQuestions(
                "熟悉 Spring Boot、MySQL、Redis、Docker。",
                4);

        assertThat(questionDTOs).hasSize(4);
        assertThat(questionDTOs)
                .extracting(InterviewQuestionDTO::getQuestionIndex)
                .containsExactly(0, 1, 2, 3);
        assertThat(questionDTOs)
                .extracting(InterviewQuestionDTO::getCategory)
                .containsExactly("Java 基础", "Java 基础", "Java 基础", "项目经验");
        assertThat(questionDTOs)
                .extracting(InterviewQuestionDTO::getType)
                .containsExactly("JAVA", "JAVA", "JAVA", "PROJECT");
        assertThat(questionDTOs)
                .extracting(InterviewQuestionDTO::getQuestion)
                .allSatisfy(question -> assertThat(question)
                        .startsWith("请结合你的简历或项目经历，"))
                .noneMatch(question -> question.contains(
                        "请结合你的简历或项目经历请结合你的简历或项目经历"));
    }

    @Test
    void generateQuestions_shouldUseActualExperienceContextWithoutResume() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        InterviewQuestionService interviewQuestionService = createService(chatClient);

        when(chatClient.prompt()).thenThrow(new IllegalStateException("AI unavailable"));

        List<InterviewQuestionDTO> questionDTOs = interviewQuestionService.generateQuestions(
                "  ",
                2);

        assertThat(questionDTOs).hasSize(2);
        assertThat(questionDTOs)
                .extracting(InterviewQuestionDTO::getQuestion)
                .allSatisfy(question -> assertThat(question)
                        .startsWith("请结合你的实际经历，"));
    }

    @Test
    void generateQuestions_shouldNormalizeUnstableAiQuestionCount() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewQuestionService interviewQuestionService = createService(chatClient);
        String aiResponse = """
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
                .content()).thenReturn(aiResponse);

        List<InterviewQuestionDTO> questionDTOs = interviewQuestionService.generateQuestions(
                "熟悉 Spring Boot。",
                4);

        assertThat(questionDTOs).hasSize(4);
        assertThat(questionDTOs)
                .extracting(InterviewQuestionDTO::getQuestionIndex)
                .containsExactly(0, 1, 2, 3);
        assertThat(questionDTOs.get(0).getCategory()).isEqualTo("综合能力");
        assertThat(questionDTOs.get(0).getType()).isEqualTo("GENERAL");
        assertThat(questionDTOs.get(1).getCategory()).isEqualTo("Java");
        assertThat(questionDTOs)
                .extracting(InterviewQuestionDTO::getQuestion)
                .allSatisfy(question -> assertThat(question).isNotBlank());
    }

    @Test
    void generateQuestions_shouldPreserveTopicSummaryFromAiResponse() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewQuestionService interviewQuestionService = createService(chatClient);
        String aiResponse = """
                {
                  "questions": [
                    {
                      "questionIndex": 8,
                      "question": "请说明 Java 线程池核心参数的作用。",
                      "type": "JAVA",
                      "category": "Java 基础",
                      "topicSummary": "Java线程池参数"
                    }
                  ]
                }
                """;

        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn(aiResponse);

        List<InterviewQuestionDTO> questionDTOs = interviewQuestionService.generateQuestions(
                "熟悉 Java 并发编程。",
                1,
                "java-backend");

        assertThat(questionDTOs).hasSize(1);
        assertThat(questionDTOs.get(0).getQuestionIndex()).isZero();
        assertThat(questionDTOs.get(0).getTopicSummary()).isEqualTo("Java线程池参数");
    }

    @Test
    void generateQuestions_shouldInjectSkillPersonaAndCategoriesIntoPrompt() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewQuestionService interviewQuestionService = createService(chatClient);
        String aiResponse = """
                {
                  "questions": [
                    {
                      "questionIndex": 0,
                      "question": "AI Skill 注入题",
                      "type": "PROJECT",
                      "category": "项目经验"
                    },
                    {
                      "questionIndex": 1,
                      "question": "AI Java 分类题",
                      "type": "JAVA",
                      "category": "Java 基础"
                    }
                  ]
                }
                """;

        when(chatClient.prompt()
                .system(argThat((String prompt) -> prompt.contains("资深 Java 后端面试官")))
                .user(argThat((String prompt) -> prompt.contains("JAVA")
                        && prompt.contains("目标生成 1 道题")
                        && prompt.contains("核心方向，优先覆盖")
                        && prompt.contains("PROJECT")
                        && prompt.contains("必须至少生成 1 道题")))
                .call()
                .content()).thenReturn(aiResponse);

        List<InterviewQuestionDTO> questionDTOs = interviewQuestionService.generateQuestions(
                "熟悉 Java，并有真实项目经验。",
                2,
                "java-backend");

        assertThat(questionDTOs).hasSize(2);
        assertThat(questionDTOs.get(0).getQuestion()).isEqualTo("AI Skill 注入题");
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

    @Test
    void generateQuestions_shouldRetryWithStricterPromptWhenAiReturnsInvalidJson() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewQuestionService interviewQuestionService = createService(chatClient);
        String aiResponse = """
                {
                  "questions": [
                    {
                      "questionIndex": 0,
                      "question": "AI 重试后题目",
                      "type": "JAVA",
                      "category": "Java 基础",
                      "topicSummary": "重试成功"
                    }
                  ]
                }
                """;

        // 第一次返回非法 JSON，第二次返回合法 JSON；只有重试真的发生，最终才能拿到 AI 题目。
        when(chatClient.prompt()
                .system(anyString())
                .user(anyString())
                .call()
                .content()).thenReturn("这不是合法的 JSON", aiResponse);

        List<InterviewQuestionDTO> questionDTOs = interviewQuestionService.generateQuestions(
                "熟悉 Java 并发编程。",
                1,
                "java-backend");

        assertThat(questionDTOs).hasSize(1);
        assertThat(questionDTOs.get(0).getQuestion()).isEqualTo("AI 重试后题目");
    }

    @Test
    void generateQuestions_shouldTellAiThatHistoryIsEmpty() throws IOException {
        String userPrompt = renderUserPromptForHistory(List.of());

        assertThat(userPrompt).contains("暂无历史提问");
    }

    @Test
    void generateQuestions_shouldGroupHistoricalQuestionsByType() throws IOException {
        String userPrompt = renderUserPromptForHistory(List.of(
                new HistoricalQuestion("题A", "JAVA", "线程池"),
                new HistoricalQuestion("题B", "JAVA", "GC"),
                new HistoricalQuestion("题C", "PROJECT", "项目难点")));

        assertThat(userPrompt).contains("已考过的知识点（避免重复出题）：");
        assertThat(userPrompt).contains("- JAVA: 线程池, GC\n");
        assertThat(userPrompt).contains("- PROJECT: 项目难点\n");
        assertThat(userPrompt.indexOf("- JAVA:")).isLessThan(userPrompt.indexOf("- PROJECT:"));
    }

    @Test
    void generateQuestions_shouldTreatBlankTypeAsGeneralWhenBuildingHistorySection() throws IOException {
        String userPrompt = renderUserPromptForHistory(List.of(
                new HistoricalQuestion("题A", null, "线程池"),
                new HistoricalQuestion("题B", "   ", "GC")));

        assertThat(userPrompt).contains("- GENERAL: 线程池, GC\n");
    }

    @Test
    void generateQuestions_shouldPreferTopicSummaryOverQuestionText() throws IOException {
        String longQuestion = "请结合你在生产环境中排查线上 CPU 飙高的完整过程，说明你是如何定位问题并最终解决的。";
        String userPrompt = renderUserPromptForHistory(List.of(
                new HistoricalQuestion(longQuestion, "JAVA", "CPU 飙高排查")));

        assertThat(userPrompt).contains("- JAVA: CPU 飙高排查\n");
        assertThat(userPrompt).doesNotContain(longQuestion);
    }

    @Test
    void generateQuestions_shouldTruncateOnlyQuestionTextLongerThanThirtyChars() throws IOException {
        String exactlyThirty = "题".repeat(30);
        String thirtyOne = "文".repeat(31);
        String userPrompt = renderUserPromptForHistory(List.of(
                new HistoricalQuestion(thirtyOne, "JAVA", null),
                new HistoricalQuestion(exactlyThirty, "PROJECT", "   ")));

        // 31 个字：保留前 30 个字并补省略号。
        assertThat(userPrompt).contains("- JAVA: " + "文".repeat(30) + "…\n");
        // 正好 30 个字：不截断也不补省略号，说明判断用的是 > 30 而不是 >= 30。
        assertThat(userPrompt).contains("- PROJECT: " + exactlyThirty + "\n");
        assertThat(userPrompt).doesNotContain("- PROJECT: " + exactlyThirty + "…");
    }

    /**
     * 渲染一次用户 Prompt，用于断言历史考点说明被真实拼进了 Prompt。
     *
     * <p>输出转换器和 Skill 分配都走真实实现，只把 AI 调用替换成固定响应，
     * 这样断言的是生产代码渲染出来的字符串，而不是测试自己拼的字符串。</p>
     */
    private String renderUserPromptForHistory(List<HistoricalQuestion> historicalQuestions) throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewQuestionService interviewQuestionService = createService(chatClient);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);

        when(chatClient.prompt()
                .system(anyString())
                .user(userPromptCaptor.capture())
                .call()
                .content()).thenReturn(AI_QUESTION_RESPONSE);

        interviewQuestionService.generateQuestions("熟悉 Spring Boot。", 1, "java-backend", historicalQuestions);

        // PromptTemplate 底层用 StringTemplate 渲染，Windows 下会把 \n 统一换成 \r\n，
        // 这里归一化换行符，否则断言换行位置的用例会随运行平台时红时绿。
        return userPromptCaptor.getValue().replace("\r\n", "\n");
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

        InterviewSkillService allocationService = new InterviewSkillService(
                mock(ResourceLoader.class));
        when(interviewSkillService.calculateAllocation(anyList(), anyInt()))
                .thenAnswer(invocation -> allocationService.calculateAllocation(
                        invocation.getArgument(0),
                        invocation.getArgument(1)));

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
                interviewSkillService,
                new StructuredOutputInvoker(new StructuredOutputProperties())
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
                .thenReturn("请根据简历 {resumeText} 生成 {questionCount} 道题。考察方向：{skillCategories}\n\n{historicalSection}");

        return resourceLoader;
    }

    private InterviewSkillCategoryDTO createCategory(
            String key,
            String label,
            String priority
    ) {
        InterviewSkillCategoryDTO categoryDTO = new InterviewSkillCategoryDTO();
        categoryDTO.setKey(key);
        categoryDTO.setLabel(label);
        categoryDTO.setPriority(priority);
        return categoryDTO;
    }
}
