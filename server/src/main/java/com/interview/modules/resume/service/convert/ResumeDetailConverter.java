package com.interview.modules.resume.service.convert;

import java.util.ArrayList;
import java.util.List;

import com.interview.modules.resume.model.dto.ResumeDetailDTO;
import com.interview.modules.resume.model.dto.ResumeListItemDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.model.entity.ResumeEntity;

/**
 * 文件功能说明
 * <p>负责简历详情和列表 DTO 转换。</p>
 *
 * @author NobuNo
 * @date 2026-04-20
 */
public class ResumeDetailConverter {

    private ResumeDetailConverter() {
    }

    /**
     * 功能说明
     * <p>转换简历详情。</p>
     *
     * @param resumeEntity 简历实体
     * @return 简历详情
     * @author NobuNo
     * @date 2026-04-20
     */
    public static ResumeDetailDTO convertToResumeDetailDTO(ResumeEntity resumeEntity) {
        ResumeDetailDTO resumeDetailDTO = new ResumeDetailDTO();
        resumeDetailDTO.setId(resumeEntity.getId()); // 简历ID
        resumeDetailDTO.setOriginalFilename(resumeEntity.getOriginalFilename()); // 原始文件名
        resumeDetailDTO.setFileSize(resumeEntity.getFileSize()); // 文件大小
        resumeDetailDTO.setContentType(resumeEntity.getContentType()); // 文件类型
        resumeDetailDTO.setStorageKey(resumeEntity.getStorageKey()); // 对象存储键
        resumeDetailDTO.setResumeText(resumeEntity.getResumeText()); // 简历正文
        resumeDetailDTO.setUploadedAt(resumeEntity.getUploadedAt()); // 上传时间
        resumeDetailDTO.setAnalyzeStatus(resumeEntity.getAnalyzeStatus()); // 分析状态
        resumeDetailDTO.setAnalyzeError(resumeEntity.getAnalyzeError()); // 分析失败原因
        return resumeDetailDTO;
    }

    /**
     * 功能说明
     * <p>转换简历分析历史列表。</p>
     *
     * @param resumeAnalysisEntities 简历分析实体列表
     * @return 简历分析历史列表
     * @author NobuNo
     * @date 2026-04-20
     */
    public static List<ResumeDetailDTO.AnalysisHistoryDTO> convertToAnalysisHistoryDTO(
            List<ResumeAnalysisEntity> resumeAnalysisEntities) {

        List<ResumeDetailDTO.AnalysisHistoryDTO> analysisHistoryDTOs = new ArrayList<>();

        for (ResumeAnalysisEntity resumeAnalysisEntity : resumeAnalysisEntities) {
            ResumeDetailDTO.AnalysisHistoryDTO analysisHistoryDTO = new ResumeDetailDTO.AnalysisHistoryDTO();
            analysisHistoryDTO.setId(resumeAnalysisEntity.getId()); // 分析记录ID
            analysisHistoryDTO.setOverallScore(resumeAnalysisEntity.getOverallScore()); // 综合评分
            analysisHistoryDTO.setContentScore(resumeAnalysisEntity.getContentScore()); // 内容完整性评分
            analysisHistoryDTO.setStructureScore(resumeAnalysisEntity.getStructureScore()); // 结构清晰度评分
            analysisHistoryDTO.setSkillMatchScore(resumeAnalysisEntity.getSkillMatchScore()); // 技能匹配度评分
            analysisHistoryDTO.setExpressionScore(resumeAnalysisEntity.getExpressionScore()); // 表达专业性评分
            analysisHistoryDTO.setProjectScore(resumeAnalysisEntity.getProjectScore()); // 项目经验评分
            analysisHistoryDTO.setSummary(resumeAnalysisEntity.getSummary()); // 分析总结
            analysisHistoryDTO.setAnalyzedAt(resumeAnalysisEntity.getAnalyzedAt()); // 分析时间
            analysisHistoryDTOs.add(analysisHistoryDTO);
        }

        return analysisHistoryDTOs;
    }

    /**
     * 功能说明
     * <p>转换简历列表项。</p>
     *
     * @param resumeEntity 简历实体
     * @param resumeAnalysisEntity 简历分析实体
     * @return 简历列表项
     * @author NobuNo
     * @date 2026-04-20
     */
    public static ResumeListItemDTO convertToResumeListItemDTO(
            ResumeEntity resumeEntity,
            ResumeAnalysisEntity resumeAnalysisEntity) {

        ResumeListItemDTO resumeListItemDTO = new ResumeListItemDTO();
        resumeListItemDTO.setId(resumeEntity.getId()); // 简历ID
        resumeListItemDTO.setFilename(resumeEntity.getOriginalFilename()); // 文件名
        resumeListItemDTO.setFileSize(resumeEntity.getFileSize()); // 文件大小
        resumeListItemDTO.setUploadedAt(resumeEntity.getUploadedAt()); // 上传时间
        resumeListItemDTO.setAnalyzeStatus(resumeEntity.getAnalyzeStatus()); // 分析状态
        resumeListItemDTO.setAnalyzeError(resumeEntity.getAnalyzeError()); // 分析失败原因

        if (resumeAnalysisEntity != null) {
            resumeListItemDTO.setLatestScore(resumeAnalysisEntity.getOverallScore()); // 最近一次分析得分
            resumeListItemDTO.setLastAnalyzedAt(resumeAnalysisEntity.getAnalyzedAt()); // 最近一次分析时间
        }

        return resumeListItemDTO;
    }
}
