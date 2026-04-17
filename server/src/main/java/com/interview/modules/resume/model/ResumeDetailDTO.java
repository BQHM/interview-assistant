package com.interview.modules.resume.model;

import com.interview.common.annotation.FieldMeta;
import com.interview.common.model.AsyncTaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * 简历详情查询接口的返回 DTO。
 * 该对象用于返回一份简历本身的详细信息，例如文件信息、解析文本和分析状态，
 * 不负责承载具体的分析结果内容。
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
}
