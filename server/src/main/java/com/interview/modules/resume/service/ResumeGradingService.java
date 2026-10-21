package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.infrastructure.ai.StructuredOutputInvoker;
import com.interview.modules.resume.model.dto.ResumeAnalysisResultDTO;
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
 * 文件功能说明
 * <p>负责简历分析业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-04-14
 */
@Service
public class ResumeGradingService {

    private final ChatClient chatClient;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<ResumeAnalysisResultDTO> outputConverter;
    private final StructuredOutputInvoker structuredOutputInvoker;

    private static final Logger log = LoggerFactory.getLogger(ResumeGradingService.class);

    /**
     * 功能说明
     * <p>初始化 AI 客户端和 Prompt 模板。</p>
     *
     * @param chatClientBuilder       AI 客户端构建器
     * @param properties              简历分析配置
     * @param resourceLoader          资源加载器
     * @param structuredOutputInvoker 结构化输出调用器
     * @throws IOException 当 Prompt 模板读取失败时抛出
     * @author NobuNo
     * @date 2026-04-14
     */
    public ResumeGradingService(
            ChatClient.Builder chatClientBuilder,
            ResumeAnalysisProperties properties,
            ResourceLoader resourceLoader,
            StructuredOutputInvoker structuredOutputInvoker) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.outputConverter = new BeanOutputConverter<>(ResumeAnalysisResultDTO.class);
        this.systemPromptTemplate = new PromptTemplate(
                resourceLoader.getResource(properties.getSystemPromptPath())
                        .getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(
                resourceLoader.getResource(properties.getUserPromptPath())
                        .getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * 功能说明
     * <p>分析简历正文。</p>
     *
     * @param rawResumeText 简历正文，尚未裁剪首尾空白
     * @return 简历分析结果
     * @author NobuNo
     * @date 2026-04-14
     */
    public ResumeAnalysisResultDTO analyzeResume(String rawResumeText) {

        if (rawResumeText == null || rawResumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历文本为空，无法分析");
        }
        String resumeText = rawResumeText.trim();
        log.info("开始执行简历分析: textLength={}", resumeText.length());
        try {
            ResumeAnalysisResultDTO resumeAnalysisResultDTO = analyzeByAi(resumeText);
            log.info("AI 简历分析成功: overallScore={}", resumeAnalysisResultDTO.getOverallScore());
            return resumeAnalysisResultDTO;
        } catch (Exception e) {
            log.warn("AI 简历分析失败, fallback to rule-based analysis", e);
            ResumeAnalysisResultDTO resumeAnalysisResultDTO = buildRuleBasedResult(resumeText);
            log.info("规则版简历分析完成: overallScore={}", resumeAnalysisResultDTO.getOverallScore());
            return resumeAnalysisResultDTO;
        }
    }

    /**
     * 功能说明
     * <p>调用 AI 分析简历。</p>
     *
     * @param resumeText 简历正文
     * @return 简历分析结果
     * @author NobuNo
     * @date 2026-04-14
     */
    private ResumeAnalysisResultDTO analyzeByAi(String resumeText) {
        String systemPrompt = systemPromptTemplate.render();

        Map<String, Object> variables = new HashMap<>();
        variables.put("resumeText", resumeText);
        String userPrompt = userPromptTemplate.render(variables);

        String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();
        // 交给统一的结构化输出调用器：失败会按配置重试；
        // 重试仍失败抛 BusinessException，由 analyzeResume 的 catch 降级到规则版分析。
        ResumeAnalysisResultDTO resumeAnalysisResultDTO = structuredOutputInvoker.invoke(
                chatClient, systemPromptWithFormat, userPrompt, outputConverter,
                ErrorCode.AI_STRUCTURED_OUTPUT_FAILED, "AI 简历分析结构化解析失败：", "AI 简历分析");

        return resumeAnalysisResultDTO;
    }

    /**
     * 功能说明
     * <p>构建规则版简历分析结果。</p>
     *
     * @param resumeText 简历正文
     * @return 简历分析结果
     * @author NobuNo
     * @date 2026-04-14
     */
    private ResumeAnalysisResultDTO buildRuleBasedResult(String resumeText) {
        ResumeAnalysisResultDTO resumeAnalysisResultDTO = new ResumeAnalysisResultDTO();

        int contentScore = scoreContent(resumeText);
        int structureScore = scoreStructure(resumeText);
        int skillMatchScore = scoreSkillMatch(resumeText);
        int expressionScore = scoreExpression(resumeText);
        int projectScore = scoreProject(resumeText);

        int overallScore = contentScore + structureScore + skillMatchScore + expressionScore + projectScore;

        ResumeAnalysisResultDTO.ScoreDetail scoreDetail = new ResumeAnalysisResultDTO.ScoreDetail();
        scoreDetail.setContentScore(contentScore);
        scoreDetail.setStructureScore(structureScore);
        scoreDetail.setSkillMatchScore(skillMatchScore);
        scoreDetail.setExpressionScore(expressionScore);
        scoreDetail.setProjectScore(projectScore);

        resumeAnalysisResultDTO.setOverallScore(overallScore);
        resumeAnalysisResultDTO.setScoreDetail(scoreDetail);
        resumeAnalysisResultDTO.setSummary(buildSummary(overallScore));
        resumeAnalysisResultDTO.setStrengths(buildStrengths(resumeText));
        resumeAnalysisResultDTO.setSuggestions(buildSuggestions(resumeText));
        return resumeAnalysisResultDTO;
    }

    /**
     * 功能说明
     * <p>统计关键词命中数量。</p>
     *
     * @param resumeText 简历正文
     * @param keywords   关键词数组
     * @return 命中数量
     * @author NobuNo
     * @date 2026-04-14
     */
    private int countMatches(String resumeText, String... keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (resumeText.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 功能说明
     * <p>判断文本是否包含任意关键词。</p>
     *
     * @param resumeText 简历正文
     * @param keywords   关键词数组
     * @return 是否包含关键词
     * @author NobuNo
     * @date 2026-04-14
     */
    private boolean containsAny(String resumeText, String... keywords) {
        for (String keyword : keywords) {
            if (resumeText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 功能说明
     * <p>计算简历内容评分。</p>
     *
     * @param resumeText 简历正文
     * @return 内容评分
     * @author NobuNo
     * @date 2026-04-14
     */
    private int scoreContent(String resumeText) {
        int score;

        if (resumeText.length() < 200) {
            score = 5;
        } else if (resumeText.length() < 500) {
            score = 10;
        } else if (resumeText.length() < 1000) {
            score = 15;
        } else {
            score = 18;
        }

        int sectionCount = countMatches(resumeText, "教育", "项目", "工作经历", "技能", "实习");
        score += sectionCount * 2;

        return Math.min(score, 25);
    }

    /**
     * 功能说明
     * <p>计算简历结构评分。</p>
     *
     * @param resumeText 简历正文
     * @return 结构评分
     * @author NobuNo
     * @date 2026-04-14
     */
    private int scoreStructure(String resumeText) {
        int sectionCount = countMatches(resumeText, "教育", "项目", "工作经历", "技能", "实习");

        if (sectionCount <= 1) {
            return 6;
        } else if (sectionCount == 2) {
            return 10;
        } else if (sectionCount == 3) {
            return 14;
        } else {
            return 18;
        }
    }

    /**
     * 功能说明
     * <p>计算技能匹配评分。</p>
     *
     * @param resumeText 简历正文
     * @return 技能匹配评分
     * @author NobuNo
     * @date 2026-04-14
     */
    private int scoreSkillMatch(String resumeText) {
        int skillCount = countMatches(resumeText, "Java", "Spring", "Spring Boot", "MySQL", "Redis", "Docker", "Linux", "Git");

        if (skillCount <= 1) {
            return 6;
        } else if (skillCount <= 3) {
            return 12;
        } else if (skillCount <= 5) {
            return 18;
        } else {
            return 22;
        }
    }

    /**
     * 功能说明
     * <p>计算表达评分。</p>
     *
     * @param resumeText 简历正文
     * @return 表达评分
     * @author NobuNo
     * @date 2026-04-14
     */
    private int scoreExpression(String resumeText) {
        boolean hasAction = containsAny(resumeText, "负责", "设计", "实现", "优化", "重构");
        boolean hasMetric = containsAny(resumeText, "%", "提升", "降低", "减少", "QPS", "耗时");

        if (hasAction && hasMetric) {
            return 13;
        } else if (hasAction) {
            return 10;
        } else {
            return 6;
        }
    }

    /**
     * 功能说明
     * <p>计算项目经验评分。</p>
     *
     * @param resumeText 简历正文
     * @return 项目经验评分
     * @author NobuNo
     * @date 2026-04-14
     */
    private int scoreProject(String resumeText) {
        boolean hasProject = containsAny(resumeText, "项目");
        boolean hasResponsibility = containsAny(resumeText, "负责", "实现", "设计", "优化");
        boolean hasOutcome = containsAny(resumeText, "提升", "降低", "%", "上线");

        if (hasProject && hasResponsibility && hasOutcome) {
            return 14;
        } else if (hasProject && hasResponsibility) {
            return 11;
        } else if (hasProject) {
            return 8;
        } else {
            return 3;
        }
    }

    /**
     * 功能说明
     * <p>生成简历分析总结。</p>
     *
     * @param overallScore 综合评分
     * @return 分析总结
     * @author NobuNo
     * @date 2026-04-14
     */
    private String buildSummary(int overallScore) {
        if (overallScore >= 75) {
            return "简历整体较完整，具备一定岗位匹配度，建议进一步强化项目成果表达。";
        } else if (overallScore >= 55) {
            return "简历具备基础内容，但在结构清晰度和项目亮点上还有提升空间。";
        } else {
            return "简历内容较弱，建议补充完整经历、技能栈和项目说明。";
        }
    }

    /**
     * 功能说明
     * <p>生成简历优点列表。</p>
     *
     * @param resumeText 简历正文
     * @return 简历优点列表
     * @author NobuNo
     * @date 2026-04-14
     */
    private List<String> buildStrengths(String resumeText) {
        return List.of("具备基础简历结构", "包含一定技术关键词");
    }

    /**
     * 功能说明
     * <p>生成简历修改建议。</p>
     *
     * @param resumeText 简历正文
     * @return 简历修改建议
     * @author NobuNo
     * @date 2026-04-14
     */
    private List<ResumeAnalysisResultDTO.Suggestion> buildSuggestions(String resumeText) {
        ResumeAnalysisResultDTO.Suggestion suggestionProject = new ResumeAnalysisResultDTO.Suggestion();
        suggestionProject.setCategory("项目");
        suggestionProject.setPriority("高");
        suggestionProject.setIssue("项目成果量化描述不足");
        suggestionProject.setRecommendation("补充性能提升、效率提升、成本降低等可量化结果");

        ResumeAnalysisResultDTO.Suggestion suggestionSkill = new ResumeAnalysisResultDTO.Suggestion();
        suggestionSkill.setCategory("技能");
        suggestionSkill.setPriority("中");
        suggestionSkill.setIssue("岗位匹配技能展示不够突出");
        suggestionSkill.setRecommendation("将与目标岗位最相关的技术栈前置展示，并结合项目说明使用场景");

        return List.of(suggestionProject, suggestionSkill);
    }
}
