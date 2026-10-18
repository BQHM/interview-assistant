package com.interview.modules.resume.service.convert;

import com.interview.modules.resume.model.dto.ResumeUploadResponseDTO;
import com.interview.modules.resume.model.entity.ResumeEntity;

/**
 * 文件功能说明
 * <p>负责简历上传 DTO 转换。</p>
 *
 * @author NobuNo
 * @date 2026-04-20
 */
public class ResumeUploadConverter {

    private ResumeUploadConverter() {
    }

    /**
     * 功能说明
     * <p>转换简历上传响应。</p>
     *
     * @param resumeEntity 简历实体
     * @param isDuplicate 是否重复简历
     * @return 简历上传响应
     * @author NobuNo
     * @date 2026-04-20
     */
    public static ResumeUploadResponseDTO convertToResumeUploadResponseDTO(
            ResumeEntity resumeEntity,
            Boolean isDuplicate) {

        ResumeUploadResponseDTO resumeUploadResponseDTO = new ResumeUploadResponseDTO();
        resumeUploadResponseDTO.setResumeId(resumeEntity.getId()); // 简历ID
        resumeUploadResponseDTO.setFilename(resumeEntity.getOriginalFilename()); // 文件名
        resumeUploadResponseDTO.setStorageKey(resumeEntity.getStorageKey()); // 对象存储键
        resumeUploadResponseDTO.setAnalyzeStatus(resumeEntity.getAnalyzeStatus()); // 分析状态
        resumeUploadResponseDTO.setDuplicate(isDuplicate); // 是否重复简历
        return resumeUploadResponseDTO;
    }
}
