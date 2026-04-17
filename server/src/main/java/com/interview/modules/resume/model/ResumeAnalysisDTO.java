package com.interview.modules.resume.model;

import com.interview.common.annotation.FieldMeta;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历分析查询接口的返回 DTO。
 * 该对象表示从数据库查询并整理后的分析结果，用于返回给前端，
 * 负责隔离数据库实体和接口响应结构。
 */
@Getter
@Setter
public class ResumeAnalysisDTO {

    /**
     * 简历ID
     * 对应的简历主键
     */
    @FieldMeta(name = "简历ID", desc = "对应的简历主键")
    private Long resumeId;

    /**
     * 综合评分
     * 简历分析后的综合评分，采用百分制（0-100）
     */
    @FieldMeta(name = "综合评分", desc = "简历分析后的综合评分，采用百分制（0-100）")
    private Integer overallScore;

    /**
     * 评分详情
     * 五个分析维度的百分制评分详情
     */
    @FieldMeta(name = "评分详情", desc = "五个分析维度的百分制评分详情")
    private ScoreDetail scoreDetail;

    /**
     * 分析总结
     * 对整份简历的总体评价
     */
    @FieldMeta(name = "分析总结", desc = "对整份简历的总体评价")
    private String summary;

    /**
     * 简历优点
     * AI 识别出的简历优势列表
     */
    @FieldMeta(name = "简历优点", desc = "AI 识别出的简历优势列表")
    private List<String> strengths;

    /**
     * 改进建议
     * AI 给出的改进建议列表
     */
    @FieldMeta(name = "改进建议", desc = "AI 给出的改进建议列表")
    private List<Suggestion> suggestions;

    /**
     * 分析完成时间
     * 简历分析结果生成时间
     */
    @FieldMeta(name = "分析完成时间", desc = "简历分析结果生成时间")
    private LocalDateTime analyzedAt;

    @Getter
    @Setter
    public static class ScoreDetail {
        /**
         * 内容完整性评分
         * 内容完整性维度评分，采用百分制（0-100）
         */
        @FieldMeta(name = "内容完整性评分", desc = "内容完整性维度评分，采用百分制（0-100）")
        private Integer contentScore;

        /**
         * 结构清晰度评分
         * 结构清晰度维度评分，采用百分制（0-100）
         */
        @FieldMeta(name = "结构清晰度评分", desc = "结构清晰度维度评分，采用百分制（0-100）")
        private Integer structureScore;

        /**
         * 技能匹配度评分
         * 技能匹配度维度评分，采用百分制（0-100）
         */
        @FieldMeta(name = "技能匹配度评分", desc = "技能匹配度维度评分，采用百分制（0-100）")
        private Integer skillMatchScore;

        /**
         * 表达专业性评分
         * 表达专业性维度评分，采用百分制（0-100）
         */
        @FieldMeta(name = "表达专业性评分", desc = "表达专业性维度评分，采用百分制（0-100）")
        private Integer expressionScore;

        /**
         * 项目经验评分
         * 项目经验维度评分，采用百分制（0-100）
         */
        @FieldMeta(name = "项目经验评分", desc = "项目经验维度评分，采用百分制（0-100）")
        private Integer projectScore;
    }

    @Getter
    @Setter
    public static class Suggestion {
        /**
         * 建议分类
         * 建议所属分类，如项目经历、技能、结构等
         */
        @FieldMeta(name = "建议分类", desc = "建议所属分类，如项目经历、技能、结构等")
        private String category;

        /**
         * 建议优先级
         * 建议处理优先级，如 high、medium、low
         */
        @FieldMeta(name = "建议优先级", desc = "建议处理优先级，如 high、medium、low")
        private String priority;

        /**
         * 问题描述
         * 当前简历存在的问题
         */
        @FieldMeta(name = "问题描述", desc = "当前简历存在的问题")
        private String issue;

        /**
         * 改进建议内容
         * 针对问题给出的具体改进方案
         */
        @FieldMeta(name = "改进建议内容", desc = "针对问题给出的具体改进方案")
        private String recommendation;
    }
}
