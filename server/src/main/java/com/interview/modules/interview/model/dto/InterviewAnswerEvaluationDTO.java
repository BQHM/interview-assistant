package com.interview.modules.interview.model.dto;

import java.util.List;

import lombok.Data;

/**
 * 面试单题答案评估结果 DTO。
 * 用于承接 AI 对单道面试题答案的结构化评估结果。
 */
@Data
public class InterviewAnswerEvaluationDTO {

    /**
     * 单题得分。
     * 分值范围建议为 0-100。
     */
    private Integer score;

    /**
     * 单题反馈。
     * 用于说明当前答案的优点、不足和改进建议。
     */
    private String feedback;

    /**
     * 参考答案。
     * 用于给用户提供更完整的答题参考。
     */
    private String referenceAnswer;

    /**
     * 关键点列表。
     * 用于记录当前题目回答时应该覆盖的核心要点。
     */
    private List<String> keyPoints;
}