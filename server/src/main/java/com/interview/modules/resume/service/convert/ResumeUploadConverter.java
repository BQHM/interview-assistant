package com.interview.modules.resume.service.convert;

import com.interview.modules.resume.model.dto.ResumeUploadResponseDTO;
import com.interview.modules.resume.model.entity.ResumeEntity;
import org.springframework.stereotype.Component;

/**
 * 简历上传转换器，负责上传场景下的返回 DTO 组装。
 */
@Component
public class ResumeUploadConverter {

    /**
     * 将简历实体转换为上传接口返回 DTO。
     */
    public ResumeUploadResponseDTO convertToResumeUploadResponseDTO(
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
