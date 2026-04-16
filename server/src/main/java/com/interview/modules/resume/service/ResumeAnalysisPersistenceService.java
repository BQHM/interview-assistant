package com.interview.modules.resume.service;

import com.interview.modules.resume.model.ResumeAnalysisEntity;
import com.interview.modules.resume.model.ResumeAnalysisResultDTO;
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
    public void saveAnalysis(ResumeEntity resume, ResumeAnalysisResultDTO result) {
        ResumeAnalysisEntity analysis = new ResumeAnalysisEntity();
        analysis.setResume(resume);
        analysis.setOverallScore(result.getOverallScore());
        analysis.setContentScore(result.getContentScore());
        analysis.setStructureScore(result.getStructureScore());
        analysis.setSkillMatchScore(result.getSkillMatchScore());
        analysis.setExpressionScore(result.getExpressionScore());
        analysis.setProjectScore(result.getProjectScore());
        analysis.setSummary(result.getSummary());
        analysis.setStrengthsJson(result.getStrengthsJson());
        analysis.setSuggestionsJson(result.getSuggestionsJson());
        resumeAnalysisRepository.save(analysis);
    }

}
