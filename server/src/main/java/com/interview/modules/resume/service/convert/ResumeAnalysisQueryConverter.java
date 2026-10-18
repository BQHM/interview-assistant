package com.interview.modules.resume.service.convert;

import com.interview.common.exception.BusinessException;
import com.interview.common.exception.ErrorCode;
import com.interview.modules.resume.model.dto.ResumeAnalysisDTO;
import com.interview.modules.resume.model.entity.ResumeAnalysisEntity;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 文件功能说明
 * <p>负责简历分析结果查询转换。</p>
 *
 * @author NobuNo
 * @date 2026-04-20
 */
@Slf4j
public class ResumeAnalysisQueryConverter {

    private ResumeAnalysisQueryConverter() {
    }

    /**
     * 功能说明
     * <p>转换简历分析结果。</p>
     *
     * @param resumeAnalysisEntity 简历分析实体
     * @param objectMapper JSON 转换器
     * @return 简历分析结果
     * @author NobuNo
     * @date 2026-04-20
     */
    public static ResumeAnalysisDTO convertToResumeAnalysisDTO(
            ResumeAnalysisEntity resumeAnalysisEntity,
            ObjectMapper objectMapper) {

        ResumeAnalysisDTO resumeAnalysisDTO = new ResumeAnalysisDTO();
        ResumeAnalysisDTO.ScoreDetail scoreDetail = new ResumeAnalysisDTO.ScoreDetail();

        resumeAnalysisDTO.setResumeId(resumeAnalysisEntity.getResume().getId()); // 简历ID
        resumeAnalysisDTO.setOverallScore(resumeAnalysisEntity.getOverallScore()); // 综合评分

        scoreDetail.setContentScore(resumeAnalysisEntity.getContentScore()); // 内容完整性评分
        scoreDetail.setStructureScore(resumeAnalysisEntity.getStructureScore()); // 结构清晰度评分
        scoreDetail.setSkillMatchScore(resumeAnalysisEntity.getSkillMatchScore()); // 技能匹配度评分
        scoreDetail.setExpressionScore(resumeAnalysisEntity.getExpressionScore()); // 表达专业性评分
        scoreDetail.setProjectScore(resumeAnalysisEntity.getProjectScore()); // 项目经验评分
        resumeAnalysisDTO.setScoreDetail(scoreDetail); // 评分详情

        resumeAnalysisDTO.setSummary(resumeAnalysisEntity.getSummary()); // 分析总结
        resumeAnalysisDTO.setAnalyzedAt(resumeAnalysisEntity.getAnalyzedAt()); // 分析时间

        try {
            resumeAnalysisDTO.setStrengths(parseStrengths(resumeAnalysisEntity.getStrengthsJson(), objectMapper)); // 简历优点
            resumeAnalysisDTO.setSuggestions(parseSuggestions(resumeAnalysisEntity.getSuggestionsJson(), objectMapper)); // 改进建议
        } catch (JacksonException e) {
            log.error("简历分析结果反序列化失败: strengthsJson={}, suggestionsJson={}",
                    resumeAnalysisEntity.getStrengthsJson(),
                    resumeAnalysisEntity.getSuggestionsJson(), e);
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果反序列化失败");
        }

        return resumeAnalysisDTO;
    }

    /**
     * 功能说明
     * <p>解析简历优点。</p>
     *
     * @param strengthsJson 优点 JSON 字符串
     * @param objectMapper JSON 转换器
     * @return 简历优点列表
     * @throws JacksonException 当 JSON 解析失败时抛出
     * @author NobuNo
     * @date 2026-04-20
     */
    private static List<String> parseStrengths(
            String strengthsJson,
            ObjectMapper objectMapper) throws JacksonException {

        if (strengthsJson == null || strengthsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(strengthsJson, new TypeReference<List<String>>() {
        });
    }

    /**
     * 功能说明
     * <p>解析简历修改建议。</p>
     *
     * @param suggestionsJson 建议 JSON 字符串
     * @param objectMapper JSON 转换器
     * @return 简历修改建议列表
     * @throws JacksonException 当 JSON 解析失败时抛出
     * @author NobuNo
     * @date 2026-04-20
     */
    private static List<ResumeAnalysisDTO.Suggestion> parseSuggestions(
            String suggestionsJson,
            ObjectMapper objectMapper) throws JacksonException {

        if (suggestionsJson == null || suggestionsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(suggestionsJson, new TypeReference<List<ResumeAnalysisDTO.Suggestion>>() {
        });
    }
}
