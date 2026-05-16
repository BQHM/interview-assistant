package com.interview.modules.interview.model.dto;

import com.interview.modules.interview.model.InterviewSessionStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

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