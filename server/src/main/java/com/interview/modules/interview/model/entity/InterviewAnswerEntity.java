package com.interview.modules.interview.model.entity;

import java.time.LocalDateTime;

import com.interview.common.annotation.FieldMeta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * 面试答案实体。
 * 用于独立保存某场面试中每一道题的用户答案和后续评估结果。
 */
@Entity
@Table(
        name = "interview_answers",
        uniqueConstraints = {@UniqueConstraint(name = "uk_interview_answer_session_question", columnNames = {"session_id", "question_index"})},
        indexes = {@Index(name = "idx_interview_answer_session_question", columnList = "session_id,question_index")}
)
@Getter
@Setter
public class InterviewAnswerEntity {

    /**
     * 主键ID
     * 面试答案记录主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldMeta(name = "主键ID", desc = "面试答案记录主键")
    private Long id;

    /**
     * 关联面试会话
     * 当前答案属于哪一场面试
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @FieldMeta(name = "关联面试会话", desc = "当前答案属于哪一场面试")
    private InterviewSessionEntity session;

    /**
     * 题目索引
     * 当前答案对应第几道题，从0开始
     */
    @Column(name = "question_index", nullable = false)
    @FieldMeta(name = "题目索引", desc = "当前答案对应第几道题，从0开始")
    private Integer questionIndex;

    /**
     * 题目内容
     * 保存答题时对应的题干快照，避免后续题目变更影响历史记录
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    @FieldMeta(name = "题目内容", desc = "保存答题时对应的题干快照")
    private String question;

    /**
     * 题目分类
     * 例如 Java、Spring Boot、Redis、项目经验等
     */
    @Column(length = 100)
    @FieldMeta(name = "题目分类", desc = "当前题目的分类")
    private String category;

    /**
     * 用户答案
     * 用户对当前题目的回答内容
     */
    @Column(columnDefinition = "TEXT")
    @FieldMeta(name = "用户答案", desc = "用户对当前题目的回答内容")
    private String userAnswer;

    /**
     * 单题得分
     * 后续 AI 评估时写入，当前阶段先预留
     */
    @FieldMeta(name = "单题得分", desc = "后续 AI 评估生成的单题得分")
    private Integer score;

    /**
     * 单题反馈
     * 后续 AI 评估时写入，当前阶段先预留
     */
    @Column(columnDefinition = "TEXT")
    @FieldMeta(name = "单题反馈", desc = "后续 AI 评估生成的单题反馈")
    private String feedback;

    /**
     * 参考答案
     * 后续 AI 出题或评估时写入，当前阶段先预留
     */
    @Column(columnDefinition = "TEXT")
    @FieldMeta(name = "参考答案", desc = "当前题目的参考答案")
    private String referenceAnswer;

    /**
     * 关键点JSON
     * 后续保存参考答案关键点列表，当前阶段先使用 JSON 字符串预留
     */
    @Column(columnDefinition = "TEXT")
    @FieldMeta(name = "关键点JSON", desc = "当前题目的关键点列表JSON")
    private String keyPointsJson;

    /**
     * 作答时间
     * 当前答案第一次保存的时间
     */
    @Column(nullable = false)
    @FieldMeta(name = "作答时间", desc = "当前答案第一次保存的时间")
    private LocalDateTime answeredAt;

    @PrePersist
    protected void onCreate() {
        if (answeredAt == null) {
            answeredAt = LocalDateTime.now();
        }
    }
}