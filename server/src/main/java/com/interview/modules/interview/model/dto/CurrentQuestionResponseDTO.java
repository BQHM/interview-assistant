package com.interview.modules.interview.model.dto;

import lombok.Data;

/**
 * 当前题目查询接口的返回 DTO。
 * 用于 GET /api/interviews/{sessionId}/question，告诉前端当前会话是否已完成，
 * 以及如果未完成，下一道应该展示给用户作答的题目是什么。
 */
@Data
public class CurrentQuestionResponseDTO {

    private Boolean completed;
    private String message;
    private InterviewQuestionDTO question;
}
