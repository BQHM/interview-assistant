package com.interview.modules.resume.model;

import com.interview.common.model.AsyncTaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


/**
 * 简历详情返回 DTO，用于隔离数据库实体和接口响应结构。
 */
@Getter
@Setter
public class ResumeDetailDTO {

    private Long id;

    private String originalFilename;

    private Long fileSize;

    private String contentType;

    private String storageKey;

    private String resumeText;

    private LocalDateTime uploadedAt;

    private AsyncTaskStatus analyzeStatus;
}
