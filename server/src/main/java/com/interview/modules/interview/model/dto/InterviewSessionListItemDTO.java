package com.interview.modules.interview.model.dto;

import java.time.LocalDateTime;

import com.interview.modules.interview.model.InterviewSessionStatus;
import lombok.Data;

/**
 * 面试历史列表项 DTO。
 * 用于 GET /api/interviews 的列表响应，只承载历史列表需要的摘要字段，
 * 不返回完整题目 JSON 和答案内容，避免列表接口过重。
 */
@Data
public class InterviewSessionListItemDTO {

    private String sessionId;

    private Long resumeId;

    private Integer totalQuestions;

    private Integer currentQuestionIndex;

    private InterviewSessionStatus status;

    private LocalDateTime createdAt;
}
