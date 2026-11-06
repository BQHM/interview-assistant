package com.interview.modules.interview.model.request;

import com.interview.common.annotation.FieldMeta;
import jakarta.validation.constraints.NotBlank;
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

    /**
     * 面试方向编号
     * 用于指定本次面试采用的 Skill
     */
    @FieldMeta(name = "面试方向编号", desc = "用于指定本次面试采用的 Skill")
    @NotBlank(message = "面试方向编号不能为空")
    private String skillId = "java-backend";

    /**
     * 本次面试使用的 LLM Provider 标识，不填时使用配置里的默认模型。
     */
    @FieldMeta(name = "LLM Provider", desc = "本次面试使用的模型标识，不填则使用默认模型")
    private String provider;
}
