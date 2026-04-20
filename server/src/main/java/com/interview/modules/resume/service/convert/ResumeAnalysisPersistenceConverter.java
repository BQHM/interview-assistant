package com.interview.modules.resume.service.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisResultDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.model.entity.ResumeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 简历分析结果持久化转换器，负责分析结果 DTO 到实体的转换。
 */
@Component
@RequiredArgsConstructor
public class ResumeAnalysisPersistenceConverter {

    // JSON 序列化工具
    private final ObjectMapper objectMapper;

    /**
     * 将结构化分析结果转换为持久化实体。
     */
    public ResumeAnalysisEntity convertToResumeAnalysisEntity(
            ResumeEntity tblResumeEntity,
            ResumeAnalysisResultDTO cplResumeAnalysisResultDTO) {

        ResumeAnalysisEntity tblResumeAnalysisEntity = new ResumeAnalysisEntity();
        tblResumeAnalysisEntity.setResume(tblResumeEntity); // 关联简历
        tblResumeAnalysisEntity.setOverallScore(cplResumeAnalysisResultDTO.getOverallScore()); // 综合评分
        tblResumeAnalysisEntity.setContentScore(cplResumeAnalysisResultDTO.getScoreDetail().getContentScore()); // 内容完整性评分
        tblResumeAnalysisEntity.setStructureScore(cplResumeAnalysisResultDTO.getScoreDetail().getStructureScore()); // 结构清晰度评分
        tblResumeAnalysisEntity.setSkillMatchScore(cplResumeAnalysisResultDTO.getScoreDetail().getSkillMatchScore()); // 技能匹配度评分
        tblResumeAnalysisEntity.setExpressionScore(cplResumeAnalysisResultDTO.getScoreDetail().getExpressionScore()); // 表达专业性评分
        tblResumeAnalysisEntity.setProjectScore(cplResumeAnalysisResultDTO.getScoreDetail().getProjectScore()); // 项目经验评分
        tblResumeAnalysisEntity.setSummary(cplResumeAnalysisResultDTO.getSummary()); // 分析总结

        try {
            tblResumeAnalysisEntity.setStrengthsJson(objectMapper.writeValueAsString(cplResumeAnalysisResultDTO.getStrengths())); // 优点列表JSON
            tblResumeAnalysisEntity.setSuggestionsJson(objectMapper.writeValueAsString(cplResumeAnalysisResultDTO.getSuggestions())); // 改进建议JSON
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果序列化失败");
        }

        return tblResumeAnalysisEntity;
    }
}
