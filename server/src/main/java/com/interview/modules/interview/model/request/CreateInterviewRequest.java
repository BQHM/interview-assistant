package com.interview.modules.interview.model.request;

import com.interview.common.annotation.FieldMeta;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class CreateInterviewRequest {

    /**
     * 简历ID
     * 用于指定本次面试基于哪份简历生成题目
     */
    @FieldMeta(name = "简历ID", desc = "用于指定本次面试基于哪份简历生成题目")
    @NotNull(message = "简历ID不能为空")
    private Long resumeId;

    /**
     * 题目数量
     * 本次面试希望生成的题目数量
     */
    @FieldMeta(name = "题目数量", desc = "本次面试希望生成的题目数量")
    @NotNull(message = "题目数量不能为空")
    @Min(value = 3, message = "题目数量最少为3")
    @Max(value = 10, message = "题目数量最多为10")
    private Integer questionCount;

}
