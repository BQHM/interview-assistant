package com.interview.modules.interview.model.dto;

import com.interview.common.annotation.FieldMeta;
import com.interview.modules.interview.model.InterviewSessionStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试会话查询接口的返回 DTO。
 * 用于创建会话、查询会话和查询未完成会话等场景，返回一场面试的当前状态、
 * 作答进度以及题目列表，是前端进入文字面试流程时最常用的会话快照。
 */
@Data
public class InterviewSessionDTO {

    /**
     * 会话ID
     * 当前面试会话的业务唯一标识
     */
    @FieldMeta(name = "会话ID", desc = "当前面试会话的业务唯一标识")
    private String sessionId;

    /**
     * 简历ID
     * 当前面试会话关联的简历主键
     */
    @FieldMeta(name = "简历ID", desc = "当前面试会话关联的简历主键")
    private Long resumeId;

    /**
     * 题目总数
     * 本次面试生成的题目总数量
     */
    @FieldMeta(name = "题目总数", desc = "本次面试生成的题目总数量")
    private Integer totalQuestions;

    /**
     * 当前题目索引
     * 当前进行到的题目索引，从0开始
     */
    @FieldMeta(name = "当前题目索引", desc = "当前进行到的题目索引，从0开始")
    private Integer currentQuestionIndex;

    /**
     * 题目列表
     * 当前面试会话包含的题目列表
     */
    @FieldMeta(name = "题目列表", desc = "当前面试会话包含的题目列表")
    private List<InterviewQuestionDTO> questions;

    /**
     * 会话状态
     * 当前面试会话的状态
     */
    @FieldMeta(name = "会话状态", desc = "当前面试会话的状态")
    private InterviewSessionStatus status;

    /**
     * 创建时间
     * 面试会话创建时间
     */
    @FieldMeta(name = "创建时间", desc = "面试会话创建时间")
    private LocalDateTime createdAt;

    /**
     * 面试方向编号
     * 当前面试会话使用的 Skill 编号
     */
    @FieldMeta(name = "面试方向编号", desc = "当前面试会话使用的 Skill 编号")
    private String skillId;
}
