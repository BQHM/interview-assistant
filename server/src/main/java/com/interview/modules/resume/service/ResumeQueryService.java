package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
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
    public ResumeEntity getById(Long id) {

        Optional<ResumeEntity> optionalResume = resumeRepository.findById(id);

        if (optionalResume.isPresent()) {
            return optionalResume.get();
        }

        throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
    }

}
