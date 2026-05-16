package com.interview.modules.interview.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.interview.modules.interview.model.InterviewSessionStatus;

import lombok.Data;

/**
 * 面试历史详情 DTO。
 * 当前用于展示会话基础信息，以及 questionsJson 中保存的题目和答案快照。
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