package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import com.interview.modules.resume.service.convert.ResumeAnalysisQueryConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 简历分析结果查询服务，负责按 resumeId 返回分析结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisQueryService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ResumeAnalysisQueryConverter resumeAnalysisQueryConverter;

    /**
     * 查询指定简历的分析结果，查不到时返回明确业务异常。
     */
    public ResumeAnalysisDTO getResumeAnalysis(Long lngResumeId) {
        log.info("开始查询简历分析结果: resumeId={}", lngResumeId);

        Optional<ResumeAnalysisEntity> optResumeAnalysisEntity = resumeAnalysisRepository.findByResumeId(lngResumeId);

        if (optResumeAnalysisEntity.isPresent()) {
            ResumeAnalysisEntity tblResumeAnalysisEntity = optResumeAnalysisEntity.get();
            log.info("查询简历分析结果成功: resumeId={}", lngResumeId);
            try {
                return resumeAnalysisQueryConverter.convertToResumeAnalysisDTO(tblResumeAnalysisEntity);
            } catch (BusinessException e) {
                log.error("简历分析结果反序列化失败: resumeId={}", lngResumeId, e);
                throw e;
            }
        } else {
            log.warn("查询简历分析结果未命中: resumeId={}", lngResumeId);
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_NOT_FOUND);
        }

    }
}
