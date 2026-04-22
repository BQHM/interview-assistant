package com.interview.modules.interview.model.dto;


import lombok.Data;

@Data
public class InterviewReportQuestionDTO {

    private Integer questionIndex;
    private String question;
    private String category;
    private String userAnswer;
    private Boolean answered;
    private String evaluation;
    private Integer score;
}