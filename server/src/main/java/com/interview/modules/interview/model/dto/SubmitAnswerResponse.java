package com.interview.modules.interview.model.dto;

import lombok.Data;

/**
 * 提交答案接口的动作结果 DTO。
 * 用于 POST /api/interviews/answer，告诉前端本次提交后是否还有下一题、
 * 下一题内容是什么，以及当前会话的最新作答进度。
 */
@Data
public class SubmitAnswerResponse {

    private Boolean hasNextQuestion;
    private InterviewQuestionDTO nextQuestion;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
}
