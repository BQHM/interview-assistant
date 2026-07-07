package com.interview.modules.resume.service.convert;

import com.interview.modules.resume.model.dto.ResumeUploadResponseDTO;
import com.interview.modules.resume.model.entity.ResumeEntity;

/**
 * 文件功能说明
 * <p>负责简历上传 DTO 转换。</p>
 *
 * @author NobuNo
 * @since 2026-04-20
 */
public class ResumeUploadConverter {

    private ResumeUploadConverter() {
    }

    /**
     * 功能说明
     * <p>转换简历上传响应。</p>
     *
     * @param tblResumeEntity 简历实体
     * @param boolIsDuplicate 是否重复简历
     * @return 简历上传响应
     * @author NobuNo
     * @since 2026-04-20
     */
    public static ResumeUploadResponseDTO convertToResumeUploadResponseDTO(
            ResumeEntity tblResumeEntity,
            Boolean boolIsDuplicate) {

        ResumeUploadResponseDTO cplResumeUploadResponseDTO = new ResumeUploadResponseDTO();
        cplResumeUploadResponseDTO.setResumeId(tblResumeEntity.getId()); // 简历ID
        cplResumeUploadResponseDTO.setFilename(tblResumeEntity.getOriginalFilename()); // 文件名
        cplResumeUploadResponseDTO.setStorageKey(tblResumeEntity.getStorageKey()); // 对象存储键
        cplResumeUploadResponseDTO.setAnalyzeStatus(tblResumeEntity.getAnalyzeStatus()); // 分析状态
        cplResumeUploadResponseDTO.setDuplicate(boolIsDuplicate); // 是否重复简历
        return cplResumeUploadResponseDTO;
    }
}
