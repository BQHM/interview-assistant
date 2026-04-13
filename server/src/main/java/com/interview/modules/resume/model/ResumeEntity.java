package com.interview.modules.resume.model;

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
    @Comment("简历主键")
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    @Comment("文件内容哈希，用于简历去重")
    private String fileHash;

    @Column(nullable = false)
    @Comment("用户上传时的原始文件名")
    private String originalFilename;

    @Comment("文件大小，单位字节")
    private Long fileSize;

    @Comment("经系统识别后的真实 MIME 类型")
    private String contentType;

    @Column(length = 500)
    @Comment("对象存储中的文件键")
    private String storageKey;

    @Column(length = 1000)
    @Comment("对象存储访问地址")
    private String storageUrl;

    @Column(columnDefinition = "TEXT")
    @Comment("解析并清洗后的简历正文")
    private String resumeText;

    @Column(nullable = false)
    @Comment("简历上传时间")
    private LocalDateTime uploadedAt;

    @Comment("最近一次访问时间")
    private LocalDateTime lastAccessedAt;

    @Comment("访问次数")
    private Integer accessCount = 0;

    // 使用字符串持久化枚举，避免后续调整枚举顺序时污染历史数据。
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Comment("简历分析任务状态")
    private AsyncTaskStatus analyzeStatus = AsyncTaskStatus.PENDING;

    @Column(length = 500)
    @Comment("简历分析失败原因")
    private String analyzeError;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        accessCount = 1;
    }
}
