package com.interview.modules.resume.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import com.interview.modules.resume.service.convert.ResumeAnalysisQueryConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 文件功能说明
 * <p>负责简历分析结果查询业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-04-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisQueryService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ObjectMapper objectMapper;

    /**
     * 功能说明
     * <p>查询简历分析结果。</p>
     *
     * @param lngResumeId 简历编号
     * @return 简历分析结果
     * @author NobuNo
     * @date 2026-04-14
     */
    public ResumeAnalysisDTO getResumeAnalysis(Long lngResumeId) {
        log.info("开始查询简历分析结果: resumeId={}", lngResumeId);

        Optional<ResumeAnalysisEntity> optResumeAnalysisEntity = resumeAnalysisRepository.findByResumeId(lngResumeId);

        if (optResumeAnalysisEntity.isPresent()) {
            ResumeAnalysisEntity tblResumeAnalysisEntity = optResumeAnalysisEntity.get();
            log.info("查询简历分析结果成功: resumeId={}", lngResumeId);
            try {
                return ResumeAnalysisQueryConverter.convertToResumeAnalysisDTO(
                        tblResumeAnalysisEntity,
                        objectMapper
                );
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
