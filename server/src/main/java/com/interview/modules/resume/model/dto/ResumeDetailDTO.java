package com.interview.modules.resume.model.dto;

import com.interview.common.annotation.FieldMeta;
import com.interview.common.model.AsyncTaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 简历详情查询接口的返回 DTO。
 * 该对象用于返回简历详情页需要的聚合信息，
 * 包含简历基础信息、分析状态与分析历史摘要。
 */
@Getter
@Setter
public class ResumeDetailDTO {

    /**
     * 简历ID
     * 简历记录主键
     */
    @FieldMeta(name = "简历ID", desc = "简历记录主键")
    private Long id;

    /**
     * 原始文件名
     * 用户上传时的原始文件名
     */
    @FieldMeta(name = "原始文件名", desc = "用户上传时的原始文件名")
    private String originalFilename;

    /**
     * 文件大小
     * 上传文件大小，单位字节
     */
    @FieldMeta(name = "文件大小", desc = "上传文件大小，单位字节")
    private Long fileSize;

    /**
     * 文件类型
     * 系统识别出的真实 MIME 类型
     */
    @FieldMeta(name = "文件类型", desc = "系统识别出的真实 MIME 类型")
    private String contentType;

    /**
     * 对象存储键
     * 文件在对象存储中的唯一标识
     */
    @FieldMeta(name = "对象存储键", desc = "文件在对象存储中的唯一标识")
    private String storageKey;

    /**
     * 简历正文
     * 解析并清洗后的简历文本内容
     */
    @FieldMeta(name = "简历正文", desc = "解析并清洗后的简历文本内容")
    private String resumeText;

    /**
     * 上传时间
     * 简历上传完成时间
     */
    @FieldMeta(name = "上传时间", desc = "简历上传完成时间")
    private LocalDateTime uploadedAt;

    /**
     * 分析状态
     * 简历分析任务当前状态
     */
    @FieldMeta(name = "分析状态", desc = "简历分析任务当前状态")
    private AsyncTaskStatus analyzeStatus;

    /**
     * 分析失败原因
     * 简历分析失败时记录的错误信息
     */
    @FieldMeta(name = "分析失败原因", desc = "简历分析失败时记录的错误信息")
    private String analyzeError;

    /**
     * 分析历史列表
     * 当前简历的历史分析结果摘要，按分析时间倒序排列
     */
    @FieldMeta(name = "分析历史列表", desc = "当前简历的历史分析结果摘要，按分析时间倒序排列")
    private List<AnalysisHistoryDTO> analyses;

    @Getter
    @Setter
    public static class AnalysisHistoryDTO {

        /**
         * 分析记录ID
         * 分析历史记录主键
         */
        @FieldMeta(name = "分析记录ID", desc = "分析历史记录主键")
        private Long id;

        /**
         * 综合评分
         * 简历分析后的综合评分，采用百分制（0-100）
         */
        @FieldMeta(name = "综合评分", desc = "简历分析后的综合评分，采用百分制（0-100）")
        private Integer overallScore;

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

        /**
         * 分析总结
         * 对本次简历分析的总体评价
         */
        @FieldMeta(name = "分析总结", desc = "对本次简历分析的总体评价")
        private String summary;

        /**
         * 分析时间
         * 本次简历分析结果生成时间
         */
        @FieldMeta(name = "分析时间", desc = "本次简历分析结果生成时间")
        private LocalDateTime analyzedAt;
    }
}
