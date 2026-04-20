package com.interview.modules.resume.model.dto;


import com.interview.common.annotation.FieldMeta;
import com.interview.common.model.AsyncTaskStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 简历上传接口的返回 DTO。
 * 该对象用于告诉前端本次上传是否成功、是否命中重复上传、生成的简历记录 id 是什么，
 * 以及当前分析任务状态，不承载完整简历详情或分析详情。
 */
@Getter
@Setter
public class ResumeUploadResponseDTO {

    /**
     * 简历ID
     * 本次上传对应的简历记录主键
     */
    @FieldMeta(name = "简历ID", desc = "本次上传对应的简历记录主键")
    private Long resumeId;

    /**
     * 文件名
     * 上传文件名或命中重复时返回的原文件名
     */
    @FieldMeta(name = "文件名", desc = "上传文件名或命中重复时返回的原文件名")
    private String filename;

    /**
     * 对象存储键
     * 文件在对象存储中的唯一标识
     */
    @FieldMeta(name = "对象存储键", desc = "文件在对象存储中的唯一标识")
    private String storageKey;

    /**
     * 分析状态
     * 当前简历分析任务状态
     */
    @FieldMeta(name = "分析状态", desc = "当前简历分析任务状态")
    private AsyncTaskStatus analyzeStatus;

    /**
     * 是否重复
     * 当前上传是否命中了已存在的相同简历
     */
    @FieldMeta(name = "是否重复", desc = "当前上传是否命中了已存在的相同简历")
    private Boolean duplicate;

}
