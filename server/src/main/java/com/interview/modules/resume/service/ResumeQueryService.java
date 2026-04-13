package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.ResumeDetailDTO;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 简历查询服务，负责按 id 返回已保存的简历详情。
 */
@Service
@RequiredArgsConstructor
public class ResumeQueryService {

    private final ResumeRepository resumeRepository;

    /**
     * 当前使用展开写法处理 Optional，便于理解查询命中和未命中的分支。
     */
    public ResumeDetailDTO getById(Long id) {

        Optional<ResumeEntity> optionalResume = resumeRepository.findById(id);

        if (optionalResume.isPresent()) {
            ResumeEntity resume = optionalResume.get();

            return convert(resume);
        } else {
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
    }

    private ResumeDetailDTO convert(ResumeEntity resume) {
        ResumeDetailDTO dto = new ResumeDetailDTO();
        dto.setId(resume.getId());
        dto.setOriginalFilename(resume.getOriginalFilename());
        dto.setFileSize(resume.getFileSize());
        dto.setContentType(resume.getContentType());
        dto.setStorageKey(resume.getStorageKey());
        dto.setResumeText(resume.getResumeText());
        dto.setUploadedAt(resume.getUploadedAt());
        dto.setAnalyzeStatus(resume.getAnalyzeStatus());
        return dto;
    }

}
