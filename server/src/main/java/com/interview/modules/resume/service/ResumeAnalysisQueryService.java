package com.interview.modules.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.ResumeAnalysisDTO;
import com.interview.modules.resume.model.ResumeAnalysisEntity;
import com.interview.modules.resume.repository.ResumeAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 简历分析结果查询服务，负责按 resumeId 返回分析结果。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisQueryService {

    private final ResumeAnalysisRepository resumeAnalysisRepository;
    private final ObjectMapper objectMapper;

    /**
     * 查询指定简历的分析结果，查不到时返回明确业务异常。
     */
    public ResumeAnalysisDTO getResumeAnalysis(Long lngResumeId) {
        log.info("开始查询简历分析结果: resumeId={}", lngResumeId);

        Optional<ResumeAnalysisEntity> optResumeAnalysisEntity = resumeAnalysisRepository.findByResumeId(lngResumeId);

        if (optResumeAnalysisEntity.isPresent()) {
            ResumeAnalysisEntity tblResumeAnalysisEntity = optResumeAnalysisEntity.get();
            log.info("查询简历分析结果成功: resumeId={}", lngResumeId);
            return convert(tblResumeAnalysisEntity);
        } else {
            log.warn("查询简历分析结果未命中: resumeId={}", lngResumeId);
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_NOT_FOUND);
        }

    }

    /**
     * 将分析结果实体转换为接口返回 DTO。
     */
    private ResumeAnalysisDTO convert(ResumeAnalysisEntity tblResumeAnalysisEntity) {
        ResumeAnalysisDTO cplResumeAnalysisDTO = new ResumeAnalysisDTO();
        ResumeAnalysisDTO.ScoreDetail cplScoreDetail = new ResumeAnalysisDTO.ScoreDetail();

        cplResumeAnalysisDTO.setResumeId(tblResumeAnalysisEntity.getResume().getId());
        cplResumeAnalysisDTO.setOverallScore(tblResumeAnalysisEntity.getOverallScore());

        cplScoreDetail.setContentScore(tblResumeAnalysisEntity.getContentScore());
        cplScoreDetail.setStructureScore(tblResumeAnalysisEntity.getStructureScore());
        cplScoreDetail.setSkillMatchScore(tblResumeAnalysisEntity.getSkillMatchScore());
        cplScoreDetail.setExpressionScore(tblResumeAnalysisEntity.getExpressionScore());
        cplScoreDetail.setProjectScore(tblResumeAnalysisEntity.getProjectScore());
        cplResumeAnalysisDTO.setScoreDetail(cplScoreDetail);

        cplResumeAnalysisDTO.setSummary(tblResumeAnalysisEntity.getSummary());
        cplResumeAnalysisDTO.setAnalyzedAt(tblResumeAnalysisEntity.getAnalyzedAt());

        try {
            cplResumeAnalysisDTO.setStrengths(parseStrengths(tblResumeAnalysisEntity.getStrengthsJson()));
            cplResumeAnalysisDTO.setSuggestions(parseSuggestions(tblResumeAnalysisEntity.getSuggestionsJson()));
        } catch (JsonProcessingException e) {
            log.error("简历分析结果反序列化失败: resumeId={}", tblResumeAnalysisEntity.getResume().getId(), e);
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果反序列化失败");
        }

        return cplResumeAnalysisDTO;
    }

    /**
     * 将数据库中的优点 JSON 反序列化为字符串列表。
     */
    private List<String> parseStrengths(String strStrengthsJson) throws JsonProcessingException {
        if (strStrengthsJson == null || strStrengthsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(strStrengthsJson, new TypeReference<List<String>>() {});
    }

    /**
     * 将数据库中的建议 JSON 反序列化为结构化建议列表。
     */
    private List<ResumeAnalysisDTO.Suggestion> parseSuggestions(String strSuggestionsJson) throws JsonProcessingException {
        if (strSuggestionsJson == null || strSuggestionsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(strSuggestionsJson, new TypeReference<List<ResumeAnalysisDTO.Suggestion>>() {});
    }
}
