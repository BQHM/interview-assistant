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
 * @since 2026-04-20
 */
@Slf4j
public class ResumeAnalysisQueryConverter {

    private ResumeAnalysisQueryConverter() {
    }

    /**
     * 功能说明
     * <p>转换简历分析结果。</p>
     *
     * @param tblResumeAnalysisEntity 简历分析实体
     * @param objectMapper JSON 转换器
     * @return 简历分析结果
     * @author NobuNo
     * @since 2026-04-20
     */
    public static ResumeAnalysisDTO convertToResumeAnalysisDTO(
            ResumeAnalysisEntity tblResumeAnalysisEntity,
            ObjectMapper objectMapper) {

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
            cplResumeAnalysisDTO.setStrengths(parseStrengths(tblResumeAnalysisEntity.getStrengthsJson(), objectMapper)); // 简历优点
            cplResumeAnalysisDTO.setSuggestions(parseSuggestions(tblResumeAnalysisEntity.getSuggestionsJson(), objectMapper)); // 改进建议
        } catch (JacksonException e) {
            log.error("简历分析结果反序列化失败: strengthsJson={}, suggestionsJson={}",
                    tblResumeAnalysisEntity.getStrengthsJson(),
                    tblResumeAnalysisEntity.getSuggestionsJson(), e);
            throw new BusinessException(ErrorCode.RESUME_ANALYSIS_FAILED, "简历分析结果反序列化失败");
        }

        return cplResumeAnalysisDTO;
    }

    /**
     * 功能说明
     * <p>解析简历优点。</p>
     *
     * @param strStrengthsJson 优点 JSON 字符串
     * @param objectMapper JSON 转换器
     * @return 简历优点列表
     * @throws JacksonException 当 JSON 解析失败时抛出
     * @author NobuNo
     * @since 2026-04-20
     */
    private static List<String> parseStrengths(
            String strStrengthsJson,
            ObjectMapper objectMapper) throws JacksonException {

        if (strStrengthsJson == null || strStrengthsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(strStrengthsJson, new TypeReference<List<String>>() {
        });
    }

    /**
     * 功能说明
     * <p>解析简历修改建议。</p>
     *
     * @param strSuggestionsJson 建议 JSON 字符串
     * @param objectMapper JSON 转换器
     * @return 简历修改建议列表
     * @throws JacksonException 当 JSON 解析失败时抛出
     * @author NobuNo
     * @since 2026-04-20
     */
    private static List<ResumeAnalysisDTO.Suggestion> parseSuggestions(
            String strSuggestionsJson,
            ObjectMapper objectMapper) throws JacksonException {

        if (strSuggestionsJson == null || strSuggestionsJson.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(strSuggestionsJson, new TypeReference<List<ResumeAnalysisDTO.Suggestion>>() {
        });
    }
}
