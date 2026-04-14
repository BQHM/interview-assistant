package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.ResumeAnalysisDTO;
import com.interview.modules.resume.model.ResumeAnalysisEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 简历分析结果查询服务，负责按 resumeId 返回分析结果。
 */
@Service
@RequiredArgsConstructor
public class ResumeAnalysisQueryService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;

    /**
     * 查询指定简历的分析结果，查不到时返回明确业务异常。
     */
    public ResumeAnalysisDTO getResumeAnalysis(Long resumeId) {

        Optional<ResumeAnalysisEntity> optionalResumeAnalysis = resumeAnalysisRepository.findByResumeId(resumeId);

        if (optionalResumeAnalysis.isPresent()) {
            ResumeAnalysisEntity resumeAnalysis = optionalResumeAnalysis.get();
            return convert(resumeAnalysis);
        }else {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_NOT_FOUND);
        }

    }

    /**
     * 将分析结果实体转换为接口返回 DTO。
     */
    private ResumeAnalysisDTO convert(ResumeAnalysisEntity resumeAnalysis) {
        ResumeAnalysisDTO dto = new ResumeAnalysisDTO();
        dto.setResumeId(resumeAnalysis.getResume().getId());
        dto.setOverallScore(resumeAnalysis.getOverallScore());
        dto.setContentScore(resumeAnalysis.getContentScore());
        dto.setStructureScore(resumeAnalysis.getStructureScore());
        dto.setSkillMatchScore(resumeAnalysis.getSkillMatchScore());
        dto.setExpressionScore(resumeAnalysis.getExpressionScore());
        dto.setProjectScore(resumeAnalysis.getProjectScore());
        dto.setSummary(resumeAnalysis.getSummary());
        dto.setStrengthsJson(resumeAnalysis.getStrengthsJson());
        dto.setSuggestionsJson(resumeAnalysis.getSuggestionsJson());
        dto.setAnalyzedAt(resumeAnalysis.getAnalyzedAt());
        return dto;
    }
}
