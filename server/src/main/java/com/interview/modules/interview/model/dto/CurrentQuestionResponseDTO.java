package com.interview.modules.interview.model.dto;

import lombok.Data;

@Data
public class CurrentQuestionResponseDTO {

    private Boolean completed;
    private String message;
    private InterviewQuestionDTO question;
}