package com.interview.modules.resume.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 简历分析结果返回 DTO，用于隔离数据库实体和接口响应结构。
 */
@Getter
@Setter
public class ResumeAnalysisDTO {
    private Long resumeId;
    private Integer overallScore;
    private Integer contentScore;
    private Integer structureScore;
    private Integer skillMatchScore;
    private Integer expressionScore;
    private Integer projectScore;
    private String summary;
    private String strengthsJson;
    private String suggestionsJson;
    private LocalDateTime analyzedAt;
}
