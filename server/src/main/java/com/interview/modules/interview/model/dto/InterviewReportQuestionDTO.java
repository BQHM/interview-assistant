package com.interview.modules.interview.model.dto;


import lombok.Data;

import java.util.List;

/**
 * 面试报告中的单题报告 DTO。
 * 用于描述报告里某一道题的作答情况，包括题目、用户答案、是否作答、
 * 单题评分、点评，以及后续给用户复盘用的参考答案和关键点。
 */
@Data
public class InterviewReportQuestionDTO {

    private Integer questionIndex;// 问题索引
    private String question;// 问题
    private String category;// 类别
    private String userAnswer;// 用户答案
    private Boolean answered;// 是否回答
    private String evaluation;// 评价
    private Integer score;// 分数
    private String referenceAnswer;// 参考答案
    private List<String> keyPoints;// 关键点
}
