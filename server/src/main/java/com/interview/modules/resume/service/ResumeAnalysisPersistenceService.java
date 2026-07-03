package com.interview.modules.resume.service;

import org.springframework.stereotype.Service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisResultDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import com.interview.modules.resume.service.convert.ResumeAnalysisPersistenceConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * 文件功能说明
 * <p>负责简历分析结果保存业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-04-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisPersistenceService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ObjectMapper objectMapper;

    /**
     * 功能说明
     * <p>保存简历分析结果。</p>
     *
     * @param tblResumeEntity 简历实体
     * @param cplResumeAnalysisResultDTO 简历分析结果
     * @author NobuNo
     * @date 2026-04-17
     */
    public void saveAnalysis(ResumeEntity tblResumeEntity, ResumeAnalysisResultDTO cplResumeAnalysisResultDTO) {
        log.info("开始保存简历分析结果: resumeId={}", tblResumeEntity.getId());

        if (cplResumeAnalysisResultDTO == null || cplResumeAnalysisResultDTO.getScoreDetail() == null) {
            log.error("简历分析结果不完整，无法落库: resumeId={}", tblResumeEntity.getId());
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果不完整，无法落库");
        }

        ResumeAnalysisEntity tblResumeAnalysisEntity = ResumeAnalysisPersistenceConverter.convertToResumeAnalysisEntity(
                tblResumeEntity,
                cplResumeAnalysisResultDTO,
                objectMapper
        );

        resumeAnalysisRepository.save(tblResumeAnalysisEntity);
        log.info("保存简历分析结果成功: resumeId={}", tblResumeEntity.getId());
    }
}
