package com.interview.modules.resume.service.convert;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisResultDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.model.entity.ResumeEntity;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 文件功能说明
 * <p>负责简历分析结果持久化转换。</p>
 *
 * @author NobuNo
 * @date 2026-04-20
 */
public class ResumeAnalysisPersistenceConverter {

    private ResumeAnalysisPersistenceConverter() {
    }

    /**
     * 功能说明
     * <p>转换简历分析实体。</p>
     *
     * @param resumeEntity 简历实体
     * @param resumeAnalysisResultDTO 简历分析结果
     * @param objectMapper JSON 转换器
     * @return 简历分析实体
     * @author NobuNo
     * @date 2026-04-20
     */
    public static ResumeAnalysisEntity convertToResumeAnalysisEntity(
            ResumeEntity resumeEntity,
            ResumeAnalysisResultDTO resumeAnalysisResultDTO,
            ObjectMapper objectMapper) {

        ResumeAnalysisEntity resumeAnalysisEntity = new ResumeAnalysisEntity();
        resumeAnalysisEntity.setResume(resumeEntity); // 关联简历
        resumeAnalysisEntity.setOverallScore(resumeAnalysisResultDTO.getOverallScore()); // 综合评分
        resumeAnalysisEntity.setContentScore(resumeAnalysisResultDTO.getScoreDetail().getContentScore()); // 内容完整性评分
        resumeAnalysisEntity.setStructureScore(resumeAnalysisResultDTO.getScoreDetail().getStructureScore()); // 结构清晰度评分
        resumeAnalysisEntity.setSkillMatchScore(resumeAnalysisResultDTO.getScoreDetail().getSkillMatchScore()); // 技能匹配度评分
        resumeAnalysisEntity.setExpressionScore(resumeAnalysisResultDTO.getScoreDetail().getExpressionScore()); // 表达专业性评分
        resumeAnalysisEntity.setProjectScore(resumeAnalysisResultDTO.getScoreDetail().getProjectScore()); // 项目经验评分
        resumeAnalysisEntity.setSummary(resumeAnalysisResultDTO.getSummary()); // 分析总结

        try {
            resumeAnalysisEntity.setStrengthsJson(objectMapper.writeValueAsString(resumeAnalysisResultDTO.getStrengths())); // 优点列表JSON
            resumeAnalysisEntity.setSuggestionsJson(objectMapper.writeValueAsString(resumeAnalysisResultDTO.getSuggestions())); // 改进建议JSON
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果序列化失败");
        }

        return resumeAnalysisEntity;
    }
}
