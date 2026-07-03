package com.interview.modules.interview.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试报告接口的整体返回 DTO。
 * 用于 GET /api/interviews/{sessionId}/report，承载整场面试的完成情况、
 * 总体评价以及每道题的评分、反馈、参考答案和关键点。
 */
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
