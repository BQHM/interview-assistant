package com.interview.modules.interview.model.dto;

import com.interview.common.annotation.FieldMeta;
import lombok.Data;

/**
 * 面试问题 DTO。
 * 用于表示一场面试中的单道题目快照。
 * 创建会话时会写入 questionsJson，查询会话、当前题和历史详情时也复用该结构。
 */
@Data
public class InterviewQuestionDTO {

    /**
     * 题目索引
     * 当前题目在面试会话中的顺序，从0开始
     */
    @FieldMeta(name = "题目索引", desc = "当前题目在面试会话中的顺序，从0开始")
    private Integer questionIndex;

    /**
     * 题目内容
     * 当前面试题的题干内容
     */
    @FieldMeta(name = "题目内容", desc = "当前面试题的题干内容")
    private String question;

    /**
     * 题目类型
     * 面试题的类型标识，便于系统分类和处理
     */
    @FieldMeta(name = "题目类型", desc = "面试题的类型标识，便于系统分类和处理")
    private String type;

    /**
     * 题目分类
     * 面试题的展示分类名称
     */
    @FieldMeta(name = "题目分类", desc = "面试题的展示分类名称")
    private String category;

    /**
     * 用户答案
     * 当前用户对这道面试题提交的回答内容
     */
    @FieldMeta(name = "用户答案", desc = "当前用户对这道面试题提交的回答内容")
    private String userAnswer;
}
