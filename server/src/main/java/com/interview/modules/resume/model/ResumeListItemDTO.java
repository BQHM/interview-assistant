package com.interview.modules.resume.model;

import com.interview.common.annotation.FieldMeta;
import com.interview.common.model.AsyncTaskStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 简历列表查询接口的返回 DTO。
 * 该对象用于承载简历列表页展示所需的摘要信息。
 */
@Getter
@Setter
public class ResumeListItemDTO {

    /**
     * 简历ID
     * 简历记录主键
     */
    @FieldMeta(name = "简历ID", desc = "简历记录主键")
    private Long id;

    /**
     * 文件名
     * 上传文件名或命中重复时返回的原文件名
     */
    @FieldMeta(name = "文件名", desc = "上传文件名或命中重复时返回的原文件名")
    private String filename;

    /**
     * 文件大小
     * 上传文件大小，单位字节
     */
    @FieldMeta(name = "文件大小", desc = "上传文件大小，单位字节")
    private Long fileSize;

    /**
     * 上传时间
     * 简历上传完成时间
     */
    @FieldMeta(name = "上传时间", desc = "简历上传完成时间")
    private LocalDateTime uploadedAt;

    /**
     * 分析状态
     * 当前简历分析任务状态
     */
    @FieldMeta(name = "分析状态", desc = "当前简历分析任务状态")
    private AsyncTaskStatus analyzeStatus;

    /**
     * 分析失败原因
     * 简历分析失败时记录的错误信息
     */
    @FieldMeta(name = "分析失败原因", desc = "简历分析失败时记录的错误信息")
    private String analyzeError;

    /**
     * 最近一次分析得分
     * 最近一次简历分析生成的综合评分，采用百分制（0-100）
     */
    @FieldMeta(name = "最近一次分析得分", desc = "最近一次简历分析生成的综合评分，采用百分制（0-100）")
    private Integer latestScore;

    /**
     * 最近一次分析时间
     * 最近一次简历分析结果生成时间
     */
    @FieldMeta(name = "最近一次分析时间", desc = "最近一次简历分析结果生成时间")
    private LocalDateTime lastAnalyzedAt;

}
