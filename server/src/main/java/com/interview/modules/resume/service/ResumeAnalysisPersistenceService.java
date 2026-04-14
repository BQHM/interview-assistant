package com.interview.modules.resume.service;

import com.interview.modules.resume.model.ResumeAnalysisEntity;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 简历分析结果持久化服务，负责把分析结果落到数据库。
 */
@Service
@RequiredArgsConstructor
public class ResumeAnalysisPersistenceService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;

    /**
     * 保存一份简历的分析结果。
     */
    public ResumeAnalysisEntity saveAnalysis(
            ResumeEntity resume,
            Integer overallScore,
            Integer contentScore,
            Integer structureScore,
            Integer skillMatchScore,
            Integer expressionScore,
            Integer projectScore,
            String summary,
            String strengthsJson,
            String suggestionsJson
    ) {
        ResumeAnalysisEntity analysis = new ResumeAnalysisEntity();
        analysis.setResume(resume);
        analysis.setOverallScore(overallScore);
        analysis.setContentScore(contentScore);
        analysis.setStructureScore(structureScore);
        analysis.setSkillMatchScore(skillMatchScore);
        analysis.setExpressionScore(expressionScore);
        analysis.setProjectScore(projectScore);
        analysis.setSummary(summary);
        analysis.setStrengthsJson(strengthsJson);
        analysis.setSuggestionsJson(suggestionsJson);
        return resumeAnalysisRepository.save(analysis);
    }

}
