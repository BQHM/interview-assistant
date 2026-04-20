package com.interview.modules.resume.service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisResultDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import com.interview.modules.resume.service.convert.ResumeAnalysisPersistenceConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 简历分析结果持久化服务，负责把分析结果落到数据库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisPersistenceService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ResumeAnalysisPersistenceConverter resumeAnalysisPersistenceConverter;

    /**
     * 保存一份简历的分析结果。
     * 当前负责把结构化分析结果转换为数据库实体，并完成 JSON 字段序列化。
     */
    public void saveAnalysis(ResumeEntity tblResumeEntity, ResumeAnalysisResultDTO cplResumeAnalysisResultDTO) {
        log.info("开始保存简历分析结果: resumeId={}", tblResumeEntity.getId());

        if (cplResumeAnalysisResultDTO == null || cplResumeAnalysisResultDTO.getScoreDetail() == null) {
            log.error("简历分析结果不完整，无法落库: resumeId={}", tblResumeEntity.getId());
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果不完整，无法落库");
        }

        ResumeAnalysisEntity tblResumeAnalysisEntity = resumeAnalysisPersistenceConverter.convertToResumeAnalysisEntity(
                tblResumeEntity,
                cplResumeAnalysisResultDTO
        );

        resumeAnalysisRepository.save(tblResumeAnalysisEntity);
        log.info("保存简历分析结果成功: resumeId={}", tblResumeEntity.getId());
    }
}
