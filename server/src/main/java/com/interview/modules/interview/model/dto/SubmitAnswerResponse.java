package com.interview.modules.interview.model.dto;

import lombok.Data;

@Data
public class SubmitAnswerResponse {

    private Boolean hasNextQuestion;
    private InterviewQuestionDTO nextQuestion;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
}