package com.interview.modules.interview.model.dto;

import com.interview.common.annotation.FieldMeta;
import lombok.Getter;
import lombok.Setter;

/**
 * 面试问题 DTO。
 * 当前版本用于表示一条生成后的基础面试题信息。
 */
@Getter
@Setter
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
}
