package com.interview.modules.interview.model.dto;

import com.interview.modules.interview.model.InterviewSessionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 面试历史列表项 DTO。
 * 用于列表页展示会话摘要，不包含完整题目和答案内容。
 */
@Getter
@Setter
public class InterviewSessionListItemDTO {

    private String sessionId;

    private Long resumeId;

    private Integer totalQuestions;

    private Integer currentQuestionIndex;

    private InterviewSessionStatus status;

    private LocalDateTime createdAt;
}