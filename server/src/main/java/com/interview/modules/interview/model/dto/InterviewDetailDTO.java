package com.interview.modules.interview.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.interview.modules.interview.model.InterviewSessionStatus;

import lombok.Data;

/**
 * 面试历史详情 DTO。
 * 用于 GET /api/interviews/{sessionId}/details，返回一场面试的基础信息、
 * 题目快照以及已从答案表聚合回来的用户答案，适合历史详情页展示。
 */
@Data
public class InterviewDetailDTO {

    private String sessionId;

    private Long resumeId;

    private Integer totalQuestions;

    private Integer currentQuestionIndex;

    private InterviewSessionStatus status;

    private LocalDateTime createdAt;

    private List<InterviewQuestionDTO> questions;
}
