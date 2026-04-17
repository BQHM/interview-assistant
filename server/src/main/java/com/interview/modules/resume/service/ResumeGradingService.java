package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.ResumeAnalysisResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历分析服务，负责调用 AI 完成结构化分析，并在失败时回退到规则版分析。
 */
@Service
public class ResumeGradingService {

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<ResumeAnalysisResultDTO> outputConverter;
    private static final Logger log = LoggerFactory.getLogger(ResumeGradingService.class);

    public ResumeGradingService(
            ChatClient.Builder chatClientBuilder,
            ResumeAnalysisProperties properties,
            ResourceLoader resourceLoader) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.outputConverter = new BeanOutputConverter<>(ResumeAnalysisResultDTO.class);
        this.systemPromptTemplate = new PromptTemplate(
                resourceLoader.getResource(properties.getSystemPromptPath())
                        .getContentAsString(StandardCharsets.UTF_8)
        );
        this.userPromptTemplate = new PromptTemplate(
                resourceLoader.getResource(properties.getUserPromptPath())
                        .getContentAsString(StandardCharsets.UTF_8)
        );
    }

    /**
     * 分析简历正文，优先走 AI 分析链路，失败时自动回退到规则版分析。
     */
    public ResumeAnalysisResultDTO analyzeResume(String resumeText) {

        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历文本为空，无法分析");
        }
        String strResumeText = resumeText.trim();
        log.info("开始执行简历分析: textLength={}", strResumeText.length());
        try {
            ResumeAnalysisResultDTO cplResumeAnalysisResultDTO = analyzeByAi(strResumeText);
            log.info("AI 简历分析成功: overallScore={}", cplResumeAnalysisResultDTO.getOverallScore());
            return cplResumeAnalysisResultDTO;
        } catch (Exception e) {
            log.warn("AI 简历分析失败, fallback to rule-based analysis", e);
            ResumeAnalysisResultDTO cplResumeAnalysisResultDTO = buildRuleBasedResult(strResumeText);
            log.info("规则版简历分析完成: overallScore={}", cplResumeAnalysisResultDTO.getOverallScore());
            return cplResumeAnalysisResultDTO;
        }
    }

    /**
     * 调用大模型完成简历分析，并将原始文本结果转换为结构化 DTO。
     */
    private ResumeAnalysisResultDTO analyzeByAi(String strResumeText) {
        String strSystemPrompt = systemPromptTemplate.render();

        Map<String, Object> mapVariables = new HashMap<>();
        mapVariables.put("resumeText", strResumeText);
        String strUserPrompt = userPromptTemplate.render(mapVariables);

        String strSystemPromptWithFormat = strSystemPrompt + "\n\n" + outputConverter.getFormat();
        String strRawContent = chatClient.prompt()
                .system(strSystemPromptWithFormat)
                .user(strUserPrompt)
                .call()
                .content();

        log.info("Resume analysis raw AI response: 【{}】", strRawContent);
        return outputConverter.convert(strRawContent);
    }

    /**
     * 当 AI 分析失败时，使用本地规则生成兜底分析结果。
     */
    private ResumeAnalysisResultDTO buildRuleBasedResult(String strResumeText) {
        ResumeAnalysisResultDTO cplResumeAnalysisResultDTO = new ResumeAnalysisResultDTO();

        int intContentScore = scoreContent(strResumeText);
        int intStructureScore = scoreStructure(strResumeText);
        int intSkillMatchScore = scoreSkillMatch(strResumeText);
        int intExpressionScore = scoreExpression(strResumeText);
        int intProjectScore = scoreProject(strResumeText);

        int intOverallScore = intContentScore + intStructureScore + intSkillMatchScore + intExpressionScore + intProjectScore;

        ResumeAnalysisResultDTO.ScoreDetail cplScoreDetail = new ResumeAnalysisResultDTO.ScoreDetail();
        cplScoreDetail.setContentScore(intContentScore);
        cplScoreDetail.setStructureScore(intStructureScore);
        cplScoreDetail.setSkillMatchScore(intSkillMatchScore);
        cplScoreDetail.setExpressionScore(intExpressionScore);
        cplScoreDetail.setProjectScore(intProjectScore);

        cplResumeAnalysisResultDTO.setOverallScore(intOverallScore);
        cplResumeAnalysisResultDTO.setScoreDetail(cplScoreDetail);
        cplResumeAnalysisResultDTO.setSummary(buildSummary(intOverallScore));
        cplResumeAnalysisResultDTO.setStrengths(buildStrengths(strResumeText));
        cplResumeAnalysisResultDTO.setSuggestions(buildSuggestions(strResumeText));
        return cplResumeAnalysisResultDTO;
    }

    private int countMatches(String strResumeText, String... arrKeywords) {
        int intCount = 0;
        for (String strKeyword : arrKeywords) {
            if (strResumeText.contains(strKeyword)) {
                intCount++;
            }
        }
        return intCount;
    }

    private boolean containsAny(String strResumeText, String... arrKeywords) {
        for (String strKeyword : arrKeywords) {
            if (strResumeText.contains(strKeyword)) {
                return true;
            }
        }
        return false;
    }

    private int scoreContent(String strResumeText) {
        int intScore;

        if (strResumeText.length() < 200) {
            intScore = 5;
        } else if (strResumeText.length() < 500) {
            intScore = 10;
        } else if (strResumeText.length() < 1000) {
            intScore = 15;
        } else {
            intScore = 18;
        }

        int intSectionCount = countMatches(strResumeText, "教育", "项目", "工作经历", "技能", "实习");
        intScore += intSectionCount * 2;

        return Math.min(intScore, 25);
    }

    private int scoreStructure(String strResumeText) {
        int intSectionCount = countMatches(strResumeText, "教育", "项目", "工作经历", "技能", "实习");

        if (intSectionCount <= 1) {
            return 6;
        } else if (intSectionCount == 2) {
            return 10;
        } else if (intSectionCount == 3) {
            return 14;
        } else {
            return 18;
        }
    }

    private int scoreSkillMatch(String strResumeText) {
        int intSkillCount = countMatches(strResumeText, "Java", "Spring", "Spring Boot", "MySQL", "Redis", "Docker", "Linux", "Git");

        if (intSkillCount <= 1) {
            return 6;
        } else if (intSkillCount <= 3) {
            return 12;
        } else if (intSkillCount <= 5) {
            return 18;
        } else {
            return 22;
        }
    }

    private int scoreExpression(String strResumeText) {
        boolean boolHasAction = containsAny(strResumeText, "负责", "设计", "实现", "优化", "重构");
        boolean boolHasMetric = containsAny(strResumeText, "%", "提升", "降低", "减少", "QPS", "耗时");

        if (boolHasAction && boolHasMetric) {
            return 13;
        } else if (boolHasAction) {
            return 10;
        } else {
            return 6;
        }
    }

    private int scoreProject(String strResumeText) {
        boolean boolHasProject = containsAny(strResumeText, "项目");
        boolean boolHasResponsibility = containsAny(strResumeText, "负责", "实现", "设计", "优化");
        boolean boolHasOutcome = containsAny(strResumeText, "提升", "降低", "%", "上线");

        if (boolHasProject && boolHasResponsibility && boolHasOutcome) {
            return 14;
        } else if (boolHasProject && boolHasResponsibility) {
            return 11;
        } else if (boolHasProject) {
            return 8;
        } else {
            return 3;
        }
    }

    private String buildSummary(int intOverallScore) {
        if (intOverallScore >= 75) {
            return "简历整体较完整，具备一定岗位匹配度，建议进一步强化项目成果表达。";
        } else if (intOverallScore >= 55) {
            return "简历具备基础内容，但在结构清晰度和项目亮点上还有提升空间。";
        } else {
            return "简历内容较弱，建议补充完整经历、技能栈和项目说明。";
        }
    }

    private List<String> buildStrengths(String strResumeText) {
        return List.of("具备基础简历结构", "包含一定技术关键词");
    }

    private List<ResumeAnalysisResultDTO.Suggestion> buildSuggestions(String strResumeText) {
        ResumeAnalysisResultDTO.Suggestion cplSuggestionProject = new ResumeAnalysisResultDTO.Suggestion();
        cplSuggestionProject.setCategory("项目");
        cplSuggestionProject.setPriority("高");
        cplSuggestionProject.setIssue("项目成果量化描述不足");
        cplSuggestionProject.setRecommendation("补充性能提升、效率提升、成本降低等可量化结果");

        ResumeAnalysisResultDTO.Suggestion cplSuggestionSkill = new ResumeAnalysisResultDTO.Suggestion();
        cplSuggestionSkill.setCategory("技能");
        cplSuggestionSkill.setPriority("中");
        cplSuggestionSkill.setIssue("岗位匹配技能展示不够突出");
        cplSuggestionSkill.setRecommendation("将与目标岗位最相关的技术栈前置展示，并结合项目说明使用场景");

        return List.of(cplSuggestionProject, cplSuggestionSkill);
    }
}
