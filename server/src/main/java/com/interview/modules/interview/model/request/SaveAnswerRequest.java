package com.interview.modules.interview.model.request;

import com.interview.common.annotation.FieldMeta;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveAnswerRequest {

    /**
     * 题目索引
     * 用于指定本次回答的是第几道题，从0开始
     */
    @FieldMeta(name = "题目索引", desc = "用于指定本次回答的是第几道题，从0开始")
    @NotNull(message = "题目索引不能为空")
    @Min(value = 0, message = "题目索引不能小于0")
    private Integer questionIndex;

    /**
     * 答案内容
     * 当前用户对指定题目的回答内容
     */
    @FieldMeta(name = "答案内容", desc = "当前用户对指定题目的回答内容")
    @NotBlank(message = "答案内容不能为空")
    private String answer;
}
