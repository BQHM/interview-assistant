package com.interview.modules.resume.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * 功能说明
     * <p>根据简历编号查询详情。</p>
     *
     * @param lngResumeId 简历编号
     * @return 简历详情
     * @author NobuNo
     * @date 2026-04-09
     */
    public ResumeDetailDTO getById(Long lngResumeId) {

        log.info("开始查询简历详情: resumeId={}", lngResumeId);
        Optional<ResumeEntity> optResumeEntity = resumeRepository.findById(lngResumeId);

        if (optResumeEntity.isPresent()) {
            ResumeEntity tblResumeEntity = optResumeEntity.get();
            List<ResumeAnalysisEntity> lstResumeAnalysisEntity =
                    resumeAnalysisRepository.findByResumeIdOrderByAnalyzedAtDesc(lngResumeId);

            ResumeDetailDTO cplResumeDetailDTO = ResumeDetailConverter.convertToResumeDetailDTO(tblResumeEntity);
            List<ResumeDetailDTO.AnalysisHistoryDTO> lstAnalysisHistoryDTO =
                    ResumeDetailConverter.convertToAnalysisHistoryDTO(lstResumeAnalysisEntity);
            cplResumeDetailDTO.setAnalyses(lstAnalysisHistoryDTO);

            log.info("查询简历详情成功: resumeId={}, analysesCount={}", lngResumeId, lstAnalysisHistoryDTO.size());
            return cplResumeDetailDTO;
        } else {
            log.warn("查询简历详情未命中: resumeId={}", lngResumeId);
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

        List<ResumeEntity> lstResumeEntity = resumeRepository.findAllByOrderByUploadedAtDesc();
        if (lstResumeEntity.isEmpty()) {
            log.info("查询简历列表成功: count=0");
            return List.of();
        }

        List<Long> lstResumeId = new ArrayList<>();
        for (ResumeEntity tblResumeEntity : lstResumeEntity) {
            lstResumeId.add(tblResumeEntity.getId());
        }

        List<ResumeAnalysisEntity> lstResumeAnalysisEntity =
                resumeAnalysisRepository.findByResumeIdIn(lstResumeId);

        Map<Long, ResumeAnalysisEntity> mapResumeAnalysisEntity = new HashMap<>();
        for (ResumeAnalysisEntity tblResumeAnalysisEntity : lstResumeAnalysisEntity) {
            mapResumeAnalysisEntity.put(tblResumeAnalysisEntity.getResume().getId(), tblResumeAnalysisEntity);
        }

        List<ResumeListItemDTO> lstResumeListItemDTO = new ArrayList<>();
        for (ResumeEntity tblResumeEntity : lstResumeEntity) {
            ResumeAnalysisEntity tblResumeAnalysisEntity = mapResumeAnalysisEntity.get(tblResumeEntity.getId());
            ResumeListItemDTO cplResumeListItemDTO = ResumeDetailConverter.convertToResumeListItemDTO(
                    tblResumeEntity,
                    tblResumeAnalysisEntity
            );
            lstResumeListItemDTO.add(cplResumeListItemDTO);
        }

        log.info("查询简历列表成功: count={}", lstResumeListItemDTO.size());
        return lstResumeListItemDTO;
    }
}
