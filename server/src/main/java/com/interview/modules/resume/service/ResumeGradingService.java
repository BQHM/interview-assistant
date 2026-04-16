package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.ResumeAnalysisResultDTO;
import org.springframework.stereotype.Service;

@Service
public class ResumeGradingService {

    public ResumeAnalysisResultDTO analyzeResume(String resumeText) {

        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历文本为空，无法分析");
        }
        String text = resumeText.trim();
        ResumeAnalysisResultDTO result = new ResumeAnalysisResultDTO();

        int contentScore = scoreContent(text);
        int structureScore = scoreStructure(text);
        int skillMatchScore = scoreSkillMatch(text);
        int expressionScore = scoreExpression(text);
        int projectScore = scoreProject(text);

        int overallScore = contentScore + structureScore + skillMatchScore + expressionScore + projectScore;

        result.setOverallScore(overallScore);
        result.setContentScore(contentScore);
        result.setStructureScore(structureScore);
        result.setSkillMatchScore(skillMatchScore);
        result.setExpressionScore(expressionScore);
        result.setProjectScore(projectScore);
        result.setSummary(buildSummary(overallScore));
        result.setStrengthsJson(buildStrengthsJson(text));
        result.setSuggestionsJson(buildSuggestionsJson(text));

        return result;

    }

    private int countMatches(String text, String... keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    private int scoreContent(String text) {
        int score;

        if (text.length() < 200) {
            score = 5;
        } else if (text.length() < 500) {
            score = 10;
        } else if (text.length() < 1000) {
            score = 15;
        } else {
            score = 18;
        }

        int sectionCount = countMatches(text, "教育", "项目", "工作经历", "技能", "实习");
        score += sectionCount * 2;

        return Math.min(score, 25);
    }
    private int scoreStructure(String text) {
        int sectionCount = countMatches(text, "教育", "项目", "工作经历", "技能", "实习");

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
    private int scoreSkillMatch(String text) {
        int skillCount = countMatches(text, "Java", "Spring", "Spring Boot", "MySQL", "Redis", "Docker", "Linux", "Git");

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
    private int scoreExpression(String text) {
        boolean hasAction = containsAny(text, "负责", "设计", "实现", "优化", "重构");
        boolean hasMetric = containsAny(text, "%", "提升", "降低", "减少", "QPS", "耗时");

        if (hasAction && hasMetric) {
            return 13;
        } else if (hasAction) {
            return 10;
        } else {
            return 6;
        }
    }
    private int scoreProject(String text) {
        boolean hasProject = containsAny(text, "项目");
        boolean hasResponsibility = containsAny(text, "负责", "实现", "设计", "优化");
        boolean hasOutcome = containsAny(text, "提升", "降低", "%", "上线");

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
    private String buildSummary(int overallScore) {
        if (overallScore >= 75) {
            return "简历整体较完整，具备一定岗位匹配度，建议进一步强化项目成果表达。";
        } else if (overallScore >= 55) {
            return "简历具备基础内容，但在结构清晰度和项目亮点上还有提升空间。";
        } else {
            return "简历内容较弱，建议补充完整经历、技能栈和项目说明。";
        }
    }
    private String buildStrengthsJson(String text) {
        return "[\"具备基础简历结构\", \"包含一定技术关键词\"]";
    }
    private String buildSuggestionsJson(String text) {
        return "[\"建议补充项目成果量化描述\", \"建议突出岗位匹配技能\"]";
    }


}
