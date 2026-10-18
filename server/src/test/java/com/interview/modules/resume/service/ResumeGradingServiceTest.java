package com.interview.modules.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.interview.common.exception.BusinessException;
import com.interview.infrastructure.ai.StructuredOutputInvoker;
import com.interview.infrastructure.ai.StructuredOutputProperties;
import com.interview.modules.resume.model.dto.ResumeAnalysisResultDTO;

/**
 * 简历分析服务测试。
 */
class ResumeGradingServiceTest {

    private static final String RESUME_TEXT = "教育经历：计算机科学与技术专业。"
            + "项目经历：负责订单系统的设计与实现，使用 Java、Spring Boot、MySQL、Redis、Docker、Linux、Git，"
            + "上线后接口耗时降低 40%。"
            + "技能：熟悉 Java 并发与 JVM。"
            + "实习：参与日志平台重构。";

    private static final String AI_SUMMARY = "这是 AI 生成的分析总结，规则版不会产出这段文字。";

    private static final String AI_RESPONSE = """
            {
              "overallScore": 99,
              "scoreDetail": {
                "contentScore": 25,
                "structureScore": 18,
                "skillMatchScore": 22,
                "expressionScore": 20,
                "projectScore": 14
              },
              "summary": "这是 AI 生成的分析总结，规则版不会产出这段文字。",
              "strengths": ["技术栈完整", "项目描述有量化结果"],
              "suggestions": [
                {
                  "category": "项目",
                  "priority": "高",
                  "issue": "项目亮点不够突出",
                  "recommendation": "补充核心指标"
                }
              ]
            }
            """;

    @Test
    void analyzeResume_shouldRejectBlankResumeText() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        ResumeGradingService resumeGradingService = createService(chatClient);

        assertThatThrownBy(() -> resumeGradingService.analyzeResume("   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("简历文本为空");
        assertThatThrownBy(() -> resumeGradingService.analyzeResume(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("简历文本为空");

        // 空简历在进入 AI 调用前就被拦截，不应该产生任何客户端交互。
        verifyNoInteractions(chatClient);
    }

    @Test
    void analyzeResume_shouldTrimResumeTextBeforeBuildingRuleBasedResult() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        ResumeGradingService resumeGradingService = createService(chatClient);
        // 首尾空白各 2 个字符：裁剪后长度 199（内容分 5），未裁剪则长度 203（内容分 10）。
        String paddedResumeText = "  " + "教".repeat(199) + "  ";

        when(chatClient.prompt()).thenThrow(new IllegalStateException("AI unavailable"));

        ResumeAnalysisResultDTO resultDTO = resumeGradingService.analyzeResume(paddedResumeText);

        // 内容分为 5，证明分析用的是 trim 之后的正文；综合分 26 = 5 + 6 + 6 + 6 + 3。
        assertThat(resultDTO.getScoreDetail().getContentScore()).isEqualTo(5);
        assertThat(resultDTO.getOverallScore()).isEqualTo(26);
    }

    @Test
    void analyzeResume_shouldReturnAiAnalysisWhenStructuredOutputSucceeds() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ResumeGradingService resumeGradingService = createService(chatClient);

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(AI_RESPONSE);

        ResumeAnalysisResultDTO resultDTO = resumeGradingService.analyzeResume(RESUME_TEXT);

        assertThat(resultDTO.getOverallScore()).isEqualTo(99);
        assertThat(resultDTO.getSummary()).isEqualTo(AI_SUMMARY);
        assertThat(resultDTO.getScoreDetail().getContentScore()).isEqualTo(25);
        assertThat(resultDTO.getScoreDetail().getExpressionScore()).isEqualTo(20);
        assertThat(resultDTO.getStrengths()).containsExactly("技术栈完整", "项目描述有量化结果");
        assertThat(resultDTO.getSuggestions()).hasSize(1);
        assertThat(resultDTO.getSuggestions().get(0).getCategory()).isEqualTo("项目");
        assertThat(resultDTO.getSuggestions().get(0).getRecommendation()).isEqualTo("补充核心指标");
    }

    @Test
    void analyzeResume_shouldRetryWhenFirstAttemptReturnsInvalidJson() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ResumeGradingService resumeGradingService = createService(chatClient);

        // 第一次返回非法 JSON，第二次返回合法 JSON；只有重试生效，最终才能拿到 AI 给出的 99 分。
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("这不是合法的 JSON", AI_RESPONSE);

        ResumeAnalysisResultDTO resultDTO = resumeGradingService.analyzeResume(RESUME_TEXT);

        // 规则版综合分上限是 92（25 + 18 + 22 + 13 + 14），99 分只可能来自重试成功的那次 AI 返回。
        assertThat(resultDTO.getOverallScore()).isEqualTo(99);
        assertThat(resultDTO.getSummary()).isEqualTo(AI_SUMMARY);
    }

    @Test
    void analyzeResume_shouldFallbackToRuleBasedResultWhenAllAttemptsReturnInvalidJson() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ResumeGradingService resumeGradingService = createService(chatClient);

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("这不是合法的 JSON");

        ResumeAnalysisResultDTO resultDTO = resumeGradingService.analyzeResume(RESUME_TEXT);

        assertRuleBasedResult(resultDTO);
    }

    @Test
    void analyzeResume_shouldFallbackToRuleBasedResultWhenAiReturnsNullContent() throws IOException {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ResumeGradingService resumeGradingService = createService(chatClient);

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn((String) null);

        ResumeAnalysisResultDTO resultDTO = resumeGradingService.analyzeResume(RESUME_TEXT);

        // 旧实现直接把 null 交给 outputConverter.convert 会抛 NPE；接入调用器后统一走规则兜底。
        assertRuleBasedResult(resultDTO);
    }

    @Test
    void analyzeResume_shouldFallbackToRuleBasedResultWhenAiThrows() throws IOException {
        ChatClient chatClient = mock(ChatClient.class);
        ResumeGradingService resumeGradingService = createService(chatClient);

        when(chatClient.prompt()).thenThrow(new IllegalStateException("AI unavailable"));

        ResumeAnalysisResultDTO resultDTO = resumeGradingService.analyzeResume(RESUME_TEXT);

        assertRuleBasedResult(resultDTO);
        // 默认配置 structuredMaxAttempts=2，失败后会按配置重试一次再降级。
        verify(chatClient, times(2)).prompt();
    }

    /**
     * 断言结果来自规则版分析而不是 AI。
     *
     * @param resultDTO 简历分析结果
     */
    private void assertRuleBasedResult(ResumeAnalysisResultDTO resultDTO) {
        ResumeAnalysisResultDTO.ScoreDetail scoreDetail = resultDTO.getScoreDetail();
        int detailSum = scoreDetail.getContentScore() + scoreDetail.getStructureScore()
                + scoreDetail.getSkillMatchScore() + scoreDetail.getExpressionScore()
                + scoreDetail.getProjectScore();

        // 规则版综合分由五个维度相加得到，上限 92，且优点和建议是固定文案。
        assertThat(resultDTO.getOverallScore()).isEqualTo(detailSum);
        assertThat(resultDTO.getOverallScore()).isLessThanOrEqualTo(92);
        assertThat(scoreDetail.getContentScore()).isBetween(5, 25);
        assertThat(scoreDetail.getStructureScore()).isIn(6, 10, 14, 18);
        assertThat(scoreDetail.getSkillMatchScore()).isIn(6, 12, 18, 22);
        assertThat(scoreDetail.getExpressionScore()).isIn(6, 10, 13);
        assertThat(scoreDetail.getProjectScore()).isIn(3, 8, 11, 14);
        assertThat(resultDTO.getStrengths()).containsExactly("具备基础简历结构", "包含一定技术关键词");
        assertThat(resultDTO.getSuggestions()).hasSize(2);
        assertThat(resultDTO.getSummary()).isNotBlank();
    }

    /**
     * 构造被测服务的实例。
     *
     * @param chatClient AI 客户端替身
     * @return 简历分析服务
     * @throws IOException 当 Prompt 模板读取失败时抛出
     */
    private ResumeGradingService createService(ChatClient chatClient) throws IOException {
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        return new ResumeGradingService(
                chatClientBuilder,
                new ResumeAnalysisProperties(),
                createResourceLoader(),
                new StructuredOutputInvoker(new StructuredOutputProperties()));
    }

    /**
     * 构造返回固定 Prompt 模板的资源加载器替身。
     *
     * @return 资源加载器替身
     * @throws IOException 当模板内容读取失败时抛出
     */
    private ResourceLoader createResourceLoader() throws IOException {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        Resource systemPromptResource = mock(Resource.class);
        Resource userPromptResource = mock(Resource.class);

        when(resourceLoader.getResource("classpath:prompts/resume-analysis-system.st"))
                .thenReturn(systemPromptResource);
        when(resourceLoader.getResource("classpath:prompts/resume-analysis-user.st"))
                .thenReturn(userPromptResource);
        when(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .thenReturn("你是一名资深简历顾问。");
        when(userPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .thenReturn("简历正文：{resumeText}");

        return resourceLoader;
    }
}
