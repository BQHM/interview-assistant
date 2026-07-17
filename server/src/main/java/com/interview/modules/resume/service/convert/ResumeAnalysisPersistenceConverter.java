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
     * @param tblResumeEntity 简历实体
     * @param cplResumeAnalysisResultDTO 简历分析结果
     * @param objectMapper JSON 转换器
     * @return 简历分析实体
     * @author NobuNo
     * @date 2026-04-20
     */
    public static ResumeAnalysisEntity convertToResumeAnalysisEntity(
            ResumeEntity tblResumeEntity,
            ResumeAnalysisResultDTO cplResumeAnalysisResultDTO,
            ObjectMapper objectMapper) {

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
        } catch (JacksonException e) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果序列化失败");
        }

        return tblResumeAnalysisEntity;
    }
}
