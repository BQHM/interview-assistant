package com.interview.modules.resume.model;

import com.interview.common.model.AsyncTaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 简历实体，保存上传文件的元信息、解析文本和后续分析状态。
 */
@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resume_hash", columnList = "fileHash", unique = true)
})
@Getter
@Setter
public class ResumeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String fileHash;

    @Column(nullable = false)
    private String originalFilename;

    private Long fileSize;
    private String contentType;

    @Column(length = 500)
    private String storageKey;

    @Column(length = 1000)
    private String storageUrl;

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime lastAccessedAt;
    private Integer accessCount = 0;

    // 使用字符串持久化枚举，避免后续调整枚举顺序时污染历史数据。
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AsyncTaskStatus analyzeStatus = AsyncTaskStatus.PENDING;

    @Column(length = 500)
    private String analyzeError;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        accessCount = 1;
    }
}
