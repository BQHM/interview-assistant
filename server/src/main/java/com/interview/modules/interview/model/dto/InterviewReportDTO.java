package com.interview.modules.interview.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewReportDTO {

    private String sessionId;
    private Long resumeId;
    private Integer totalQuestions;
    private Integer answeredQuestions;
    private Integer unansweredQuestions;
    private Boolean completed;
    private String overallEvaluation;
    private List<InterviewReportQuestionDTO> questionReports;
    private LocalDateTime generatedAt;
}