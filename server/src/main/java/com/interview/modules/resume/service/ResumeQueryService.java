package com.interview.modules.resume.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.interview.infrastructure.redis.ResumeDetailCache;
import org.springframework.stereotype.Service;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeDetailDTO;
import com.interview.modules.resume.model.dto.ResumeListItemDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import com.interview.modules.resume.model.entity.ResumeEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import com.interview.modules.resume.repository.ResumeRepository;
import com.interview.modules.resume.service.convert.ResumeDetailConverter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件功能说明
 * <p>负责简历查询业务逻辑。</p>
 *
 * @author NobuNo
 * @date 2026-04-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeQueryService {

    private final ResumeRepository resumeRepository;
    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ResumeDetailCache resumeDetailCache;

    /**
     * 功能说明
     * <p>根据简历编号查询详情。</p>
     *
     * @param resumeId 简历编号
     * @return 简历详情
     * @author NobuNo
     * @date 2026-04-09
     */
    public ResumeDetailDTO getById(Long resumeId) {

        log.info("开始查询简历详情: resumeId={}", resumeId);
        return resumeDetailCache.getOrLoad(resumeId, () -> loadResumeDetailFromDatabase(resumeId));
    }

    private ResumeDetailDTO loadResumeDetailFromDatabase(Long resumeId) {

        Optional<ResumeEntity> optResumeEntity = resumeRepository.findById(resumeId);// 通过ID查询简历信息

        if (optResumeEntity.isPresent()) {
            ResumeEntity resumeEntity = optResumeEntity.get();// 获取简历实体
            // 按分析时间倒序查询简历分析历史，获取简历分析结果
            List<ResumeAnalysisEntity> resumeAnalysisEntities = resumeAnalysisRepository.findByResumeIdOrderByAnalyzedAtDesc(resumeId);

            // 表结果转换成复合类型
            ResumeDetailDTO resumeDetailDTO = ResumeDetailConverter.convertToResumeDetailDTO(resumeEntity);

            List<ResumeDetailDTO.AnalysisHistoryDTO> analysisHistoryDTOs = ResumeDetailConverter.convertToAnalysisHistoryDTO(resumeAnalysisEntities);
            resumeDetailDTO.setAnalyses(analysisHistoryDTOs);

            log.info("查询简历详情成功: resumeId={}, analysesCount={}", resumeId, analysisHistoryDTOs.size());
            return resumeDetailDTO;
        } else {
            log.warn("查询简历详情未命中: resumeId={}", resumeId);
            throw new BusinessException(ErrorCode.RESUME_NOT_FOUND);
        }
    }

    /**
     * 功能说明
     * <p>查询简历列表。</p>
     *
     * @return 简历列表
     * @author NobuNo
     * @date 2026-04-09
     */
    public List<ResumeListItemDTO> listResumes() {
        log.info("开始查询简历列表");

        List<ResumeEntity> resumeEntities = resumeRepository.findAllByOrderByUploadedAtDesc();
        if (resumeEntities.isEmpty()) {
            log.info("查询简历列表成功: count=0");
            return List.of();
        }

        List<Long> resumeIds = new ArrayList<>();
        for (ResumeEntity resumeEntity : resumeEntities) {
            resumeIds.add(resumeEntity.getId());
        }

        List<ResumeAnalysisEntity> resumeAnalysisEntities =
                resumeAnalysisRepository.findByResumeIdIn(resumeIds);

        Map<Long, ResumeAnalysisEntity> resumeAnalysisEntityMap = new HashMap<>();
        for (ResumeAnalysisEntity resumeAnalysisEntity : resumeAnalysisEntities) {
            resumeAnalysisEntityMap.put(resumeAnalysisEntity.getResume().getId(), resumeAnalysisEntity);
        }

        List<ResumeListItemDTO> resumeListItemDTOs = new ArrayList<>();
        for (ResumeEntity resumeEntity : resumeEntities) {
            ResumeAnalysisEntity resumeAnalysisEntity = resumeAnalysisEntityMap.get(resumeEntity.getId());
            ResumeListItemDTO resumeListItemDTO = ResumeDetailConverter.convertToResumeListItemDTO(
                    resumeEntity,
                    resumeAnalysisEntity
            );
            resumeListItemDTOs.add(resumeListItemDTO);
        }

        log.info("查询简历列表成功: count={}", resumeListItemDTOs.size());
        return resumeListItemDTOs;
    }
}
