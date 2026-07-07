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
 * @since 2026-04-20
 */
public class ResumeDetailConverter {

    private ResumeDetailConverter() {
    }

    /**
     * 功能说明
     * <p>转换简历详情。</p>
     *
     * @param tblResumeEntity 简历实体
     * @return 简历详情
     * @author NobuNo
     * @since 2026-04-20
     */
    public static ResumeDetailDTO convertToResumeDetailDTO(ResumeEntity tblResumeEntity) {
        ResumeDetailDTO cplResumeDetailDTO = new ResumeDetailDTO();
        cplResumeDetailDTO.setId(tblResumeEntity.getId()); // 简历ID
        cplResumeDetailDTO.setOriginalFilename(tblResumeEntity.getOriginalFilename()); // 原始文件名
        cplResumeDetailDTO.setFileSize(tblResumeEntity.getFileSize()); // 文件大小
        cplResumeDetailDTO.setContentType(tblResumeEntity.getContentType()); // 文件类型
        cplResumeDetailDTO.setStorageKey(tblResumeEntity.getStorageKey()); // 对象存储键
        cplResumeDetailDTO.setResumeText(tblResumeEntity.getResumeText()); // 简历正文
        cplResumeDetailDTO.setUploadedAt(tblResumeEntity.getUploadedAt()); // 上传时间
        cplResumeDetailDTO.setAnalyzeStatus(tblResumeEntity.getAnalyzeStatus()); // 分析状态
        cplResumeDetailDTO.setAnalyzeError(tblResumeEntity.getAnalyzeError()); // 分析失败原因
        return cplResumeDetailDTO;
    }

    /**
     * 功能说明
     * <p>转换简历分析历史列表。</p>
     *
     * @param lstResumeAnalysisEntity 简历分析实体列表
     * @return 简历分析历史列表
     * @author NobuNo
     * @since 2026-04-20
     */
    public static List<ResumeDetailDTO.AnalysisHistoryDTO> convertToAnalysisHistoryDTO(
            List<ResumeAnalysisEntity> lstResumeAnalysisEntity) {

        List<ResumeDetailDTO.AnalysisHistoryDTO> lstAnalysisHistoryDTO = new ArrayList<>();

        for (ResumeAnalysisEntity tblResumeAnalysisEntity : lstResumeAnalysisEntity) {
            ResumeDetailDTO.AnalysisHistoryDTO cplAnalysisHistoryDTO = new ResumeDetailDTO.AnalysisHistoryDTO();
            cplAnalysisHistoryDTO.setId(tblResumeAnalysisEntity.getId()); // 分析记录ID
            cplAnalysisHistoryDTO.setOverallScore(tblResumeAnalysisEntity.getOverallScore()); // 综合评分
            cplAnalysisHistoryDTO.setContentScore(tblResumeAnalysisEntity.getContentScore()); // 内容完整性评分
            cplAnalysisHistoryDTO.setStructureScore(tblResumeAnalysisEntity.getStructureScore()); // 结构清晰度评分
            cplAnalysisHistoryDTO.setSkillMatchScore(tblResumeAnalysisEntity.getSkillMatchScore()); // 技能匹配度评分
            cplAnalysisHistoryDTO.setExpressionScore(tblResumeAnalysisEntity.getExpressionScore()); // 表达专业性评分
            cplAnalysisHistoryDTO.setProjectScore(tblResumeAnalysisEntity.getProjectScore()); // 项目经验评分
            cplAnalysisHistoryDTO.setSummary(tblResumeAnalysisEntity.getSummary()); // 分析总结
            cplAnalysisHistoryDTO.setAnalyzedAt(tblResumeAnalysisEntity.getAnalyzedAt()); // 分析时间
            lstAnalysisHistoryDTO.add(cplAnalysisHistoryDTO);
        }

        return lstAnalysisHistoryDTO;
    }

    /**
     * 功能说明
     * <p>转换简历列表项。</p>
     *
     * @param tblResumeEntity 简历实体
     * @param tblResumeAnalysisEntity 简历分析实体
     * @return 简历列表项
     * @author NobuNo
     * @since 2026-04-20
     */
    public static ResumeListItemDTO convertToResumeListItemDTO(
            ResumeEntity tblResumeEntity,
            ResumeAnalysisEntity tblResumeAnalysisEntity) {

        ResumeListItemDTO cplResumeListItemDTO = new ResumeListItemDTO();
        cplResumeListItemDTO.setId(tblResumeEntity.getId()); // 简历ID
        cplResumeListItemDTO.setFilename(tblResumeEntity.getOriginalFilename()); // 文件名
        cplResumeListItemDTO.setFileSize(tblResumeEntity.getFileSize()); // 文件大小
        cplResumeListItemDTO.setUploadedAt(tblResumeEntity.getUploadedAt()); // 上传时间
        cplResumeListItemDTO.setAnalyzeStatus(tblResumeEntity.getAnalyzeStatus()); // 分析状态
        cplResumeListItemDTO.setAnalyzeError(tblResumeEntity.getAnalyzeError()); // 分析失败原因

        if (tblResumeAnalysisEntity != null) {
            cplResumeListItemDTO.setLatestScore(tblResumeAnalysisEntity.getOverallScore()); // 最近一次分析得分
            cplResumeListItemDTO.setLastAnalyzedAt(tblResumeAnalysisEntity.getAnalyzedAt()); // 最近一次分析时间
        }

        return cplResumeListItemDTO;
    }
}
