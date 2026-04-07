package com.interview.modules.resume.model;

import com.interview.common.model.AsyncTaskStatus;  // 导入异步状态枚举
import jakarta.persistence.*;  // 导入 JPA 的所有注解（* 代表全部）
import lombok.Getter;  // ← 添加这两个 import
import lombok.Setter;

import java.time.LocalDateTime;  // 导入日期时间类

@Entity
@Table(name = "resumes", indexes = {
        @Index(name = "idx_resume_hash", columnList = "fileHash", unique = true)
})
@Getter      // ← 加这个注解，自动生成所有 getter
@Setter      // ← 加这个注解，自动生成所有 setter
public class ResumeEntity {

    @Id  // ← 告诉 JPA："这是主键"
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ← 主键值由数据库自动生成（比如自增）
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String fileHash;

    @Column(nullable = false)
    private String originalFilename;  // 原始文件名，不能为空

    private Long fileSize;  // 文件大小，可以为空
    private String contentType;  // 文件类型（PDF、Word 等）

    @Column(length = 500)
    private String storageKey;  // 存到 S3/RustFS 的文件路径

    @Column(length = 1000)
    private String storageUrl;  // 文件的访问 URL

    @Column(columnDefinition = "TEXT")
    private String resumeText;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;  // 上传时间，必须有

    private LocalDateTime lastAccessedAt;  // 最后访问时间
    private Integer accessCount = 0;  // 访问次数，默认 0

    @Enumerated(EnumType.STRING)  // ← 枚举存成字符串（不是数字）
    @Column(length = 20)
    private AsyncTaskStatus analyzeStatus = AsyncTaskStatus.PENDING;  // 默认待处理

    @Column(length = 500)
    private String analyzeError;  // 分析失败时的错误信息

    @PrePersist  // ← 在对象第一次保存到数据库之前，自动调用这个方法
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();      // 设置上传时间
        lastAccessedAt = LocalDateTime.now(); // 设置最后访问时间
        accessCount = 1;                       // 访问次数设为 1
    }
}
