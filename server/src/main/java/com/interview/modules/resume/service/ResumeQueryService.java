package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.ResumeEntity;
import com.interview.modules.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResumeQueryService {

    private final ResumeRepository resumeRepository;

    public ResumeEntity getById(Long id) {

        Optional<ResumeEntity> optionalResume = resumeRepository.findById(id);

        if (optionalResume.isPresent()) {
            return optionalResume.get();
        }

        throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
    }

}
