package com.interview.modules.resume.service.convert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 简历分析结果查询转换器，负责实体到分析结果 DTO 的转换。
 */
@Component
@RequiredArgsConstructor
public class ResumeAnalysisQueryConverter {

    private final ObjectMapper objectMapper;

    /**
     * 将分析结果实体转换为接口返回 DTO。
     */
    public ResumeAnalysisDTO convertToResumeAnalysisDTO(ResumeAnalysisEntity tblResumeAnalysisEntity) {
        ResumeAnalysisDTO cplResumeAnalysisDTO = new ResumeAnalysisDTO();
        ResumeAnalysisDTO.ScoreDetail cplScoreDetail = new ResumeAnalysisDTO.ScoreDetail();

        cplResumeAnalysisDTO.setResumeId(tblResumeAnalysisEntity.getResume().getId()); // 简历ID
        cplResumeAnalysisDTO.setOverallScore(tblResumeAnalysisEntity.getOverallScore()); // 综合评分

        cplScoreDetail.setContentScore(tblResumeAnalysisEntity.getContentScore()); // 内容完整性评分
        cplScoreDetail.setStructureScore(tblResumeAnalysisEntity.getStructureScore()); // 结构清晰度评分
        cplScoreDetail.setSkillMatchScore(tblResumeAnalysisEntity.getSkillMatchScore()); // 技能匹配度评分
        cplScoreDetail.setExpressionScore(tblResumeAnalysisEntity.getExpressionScore()); // 表达专业性评分
        cplScoreDetail.setProjectScore(tblResumeAnalysisEntity.getProjectScore()); // 项目经验评分
        cplResumeAnalysisDTO.setScoreDetail(cplScoreDetail); // 评分详情

        cplResumeAnalysisDTO.setSummary(tblResumeAnalysisEntity.getSummary()); // 分析总结
        cplResumeAnalysisDTO.setAnalyzedAt(tblResumeAnalysisEntity.getAnalyzedAt()); // 分析时间

        try {
            cplResumeAnalysisDTO.setStrengths(parseStrengths(tblResumeAnalysisEntity.getStrengthsJson())); // 简历优点
            cplResumeAnalysisDTO.setSuggestions(parseSuggestions(tblResumeAnalysisEntity.getSuggestionsJson())); // 改进建议
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果反序列化失败");
        }

        return cplResumeAnalysisDTO;
    }

    private List<String> parseStrengths(String strStrengthsJson) throws JsonProcessingException {
        if (strStrengthsJson == null || strStrengthsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(strStrengthsJson, new TypeReference<List<String>>() {});
    }

    private List<ResumeAnalysisDTO.Suggestion> parseSuggestions(String strSuggestionsJson) throws JsonProcessingException {
        if (strSuggestionsJson == null || strSuggestionsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(strSuggestionsJson, new TypeReference<List<ResumeAnalysisDTO.Suggestion>>() {});
    }
}
