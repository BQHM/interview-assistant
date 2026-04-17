package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.ResumeDetailDTO;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 简历查询服务，负责按 id 返回已保存的简历详情。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeQueryService {

    private final ResumeRepository resumeRepository;

    /**
     * 根据简历主键查询简历详情。
     * 查询命中时返回详情 DTO，未命中时抛出明确业务异常。
     */
    public ResumeDetailDTO getById(Long lngResumeId) {

        log.info("开始查询简历详情: resumeId={}", lngResumeId);
        Optional<ResumeEntity> optResumeEntity = resumeRepository.findById(lngResumeId);

        if (optResumeEntity.isPresent()) {
            ResumeEntity tblResumeEntity = optResumeEntity.get();
            log.info("查询简历详情成功: resumeId={}", lngResumeId);

            return convert(tblResumeEntity);
        } else {
            log.warn("查询简历详情未命中: resumeId={}", lngResumeId);
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
    }

    /**
     * 将简历实体转换为详情 DTO，隔离数据库实体和接口返回结构。
     */
    private ResumeDetailDTO convert(ResumeEntity tblResumeEntity) {
        ResumeDetailDTO cplResumeDetailDTO = new ResumeDetailDTO();
        cplResumeDetailDTO.setId(tblResumeEntity.getId());
        cplResumeDetailDTO.setOriginalFilename(tblResumeEntity.getOriginalFilename());
        cplResumeDetailDTO.setFileSize(tblResumeEntity.getFileSize());
        cplResumeDetailDTO.setContentType(tblResumeEntity.getContentType());
        cplResumeDetailDTO.setStorageKey(tblResumeEntity.getStorageKey());
        cplResumeDetailDTO.setResumeText(tblResumeEntity.getResumeText());
        cplResumeDetailDTO.setUploadedAt(tblResumeEntity.getUploadedAt());
        cplResumeDetailDTO.setAnalyzeStatus(tblResumeEntity.getAnalyzeStatus());
        return cplResumeDetailDTO;
    }

}
