package com.interview.modules.resume.model.entity;

import com.interview.common.annotation.FieldMeta;
import com.interview.common.model.AsyncTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 简历实体，保存上传文件的元信息、解析文本和后续分析状态。
 */
@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resume_hash", columnList = "fileHash", unique = true)
})
@Comment("简历表")
@Getter
@Setter
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    /**
     * 简历ID
     * 简历主键
     */
    @FieldMeta(name = "简历ID", desc = "简历主键")
    @Comment("简历主键")
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    /**
     * 文件内容哈希
     * 基于文件原始内容计算的哈希值，用于简历去重
     */
    @FieldMeta(name = "文件内容哈希", desc = "基于文件原始内容计算的哈希值，用于简历去重")
    @Comment("文件内容哈希，用于简历去重")
    private String fileHash;

    @Column(nullable = false)
    /**
     * 原始文件名
     * 用户上传时的原始文件名
     */
    @FieldMeta(name = "原始文件名", desc = "用户上传时的原始文件名")
    @Comment("用户上传时的原始文件名")
    private String originalFilename;

    /**
     * 文件大小
     * 上传文件大小，单位字节
     */
    @FieldMeta(name = "文件大小", desc = "上传文件大小，单位字节")
    @Comment("文件大小，单位字节")
    private Long fileSize;

    /**
     * 文件类型
     * 系统识别出的真实 MIME 类型
     */
    @FieldMeta(name = "文件类型", desc = "系统识别出的真实 MIME 类型")
    @Comment("经系统识别后的真实 MIME 类型")
    private String contentType;

    @Column(length = 500)
    /**
     * 对象存储键
     * 文件在对象存储中的唯一标识
     */
    @FieldMeta(name = "对象存储键", desc = "文件在对象存储中的唯一标识")
    @Comment("对象存储中的文件键")
    private String storageKey;

    @Column(length = 1000)
    /**
     * 对象存储访问地址
     * 文件在对象存储中的访问 URL
     */
    @FieldMeta(name = "对象存储访问地址", desc = "文件在对象存储中的访问 URL")
    @Comment("对象存储访问地址")
    private String storageUrl;

    @Column(columnDefinition = "TEXT")
    /**
     * 简历正文
     * 解析并清洗后的简历文本内容
     */
    @FieldMeta(name = "简历正文", desc = "解析并清洗后的简历文本内容")
    @Comment("解析并清洗后的简历正文")
    private String resumeText;

    @Column(nullable = false)
    /**
     * 上传时间
     * 简历上传完成时间
     */
    @FieldMeta(name = "上传时间", desc = "简历上传完成时间")
    @Comment("简历上传时间")
    private LocalDateTime uploadedAt;

    /**
     * 最近访问时间
     * 最近一次访问该简历的时间
     */
    @FieldMeta(name = "最近访问时间", desc = "最近一次访问该简历的时间")
    @Comment("最近一次访问时间")
    private LocalDateTime lastAccessedAt;

    /**
     * 访问次数
     * 当前简历被访问的累计次数
     */
    @FieldMeta(name = "访问次数", desc = "当前简历被访问的累计次数")
    @Comment("访问次数")
    private Integer accessCount = 0;

    // 使用字符串持久化枚举，避免后续调整枚举顺序时污染历史数据。
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    /**
     * 分析状态
     * 简历分析任务当前状态
     */
    @FieldMeta(name = "分析状态", desc = "简历分析任务当前状态")
    @Comment("简历分析任务状态")
    private AsyncTaskStatus analyzeStatus = AsyncTaskStatus.PENDING;

    @Column(length = 500)
    /**
     * 分析失败原因
     * 简历分析失败时记录的错误信息
     */
    @FieldMeta(name = "分析失败原因", desc = "简历分析失败时记录的错误信息")
    @Comment("简历分析失败原因")
    private String analyzeError;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        accessCount = 1;
    }
}
