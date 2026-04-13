package com.interview.modules.resume.model;


import com.interview.common.model.AsyncTaskStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 简历上传成功后的返回 DTO。
 */
@Getter
@Setter
public class ResumeUploadResponseDTO {

    private Long resumeId;

    private String filename;

    private String storageKey;

    private AsyncTaskStatus analyzeStatus;

    private Boolean duplicate;

}
