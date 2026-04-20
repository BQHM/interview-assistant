package com.interview.modules.interview.model.entity;

import com.interview.common.annotation.FieldMeta;
import com.interview.modules.interview.model.InterviewSessionStatus;
import com.interview.modules.resume.model.entity.ResumeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 面试会话实体。
 * 当前版本用于保存基于简历创建的模拟面试会话基础信息。
 */
@Entity
@Table(
        name = "interview_sessions",
        indexes = {
                @jakarta.persistence.Index(name = "idx_interview_session_session_id", columnList = "sessionId")
        }
)
@Comment("面试会话表")
@Getter
@Setter
public class InterviewSessionEntity {

    /**
     * 主键ID
     * 面试会话记录主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldMeta(name = "主键ID", desc = "面试会话记录主键")
    @Comment("面试会话记录主键")
    private Long id;

    /**
     * 会话ID
     * 当前面试会话的业务唯一标识
     */
    @Column(nullable = false, unique = true, length = 36)
    @FieldMeta(name = "会话ID", desc = "当前面试会话的业务唯一标识")
    @Comment("面试会话业务唯一标识")
    private String sessionId;

    /**
     * 关联简历
     * 当前面试会话基于哪份简历创建
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    @FieldMeta(name = "关联简历", desc = "当前面试会话基于哪份简历创建")
    @Comment("关联简历")
    private ResumeEntity resume;

    /**
     * 题目总数
     * 本次面试生成的题目总数量
     */
    @Column(nullable = false)
    @FieldMeta(name = "题目总数", desc = "本次面试生成的题目总数量")
    @Comment("本次面试生成的题目总数量")
    private Integer totalQuestions;

    /**
     * 当前题目索引
     * 当前进行到的题目索引，从0开始
     */
    @Column(nullable = false)
    @FieldMeta(name = "当前题目索引", desc = "当前进行到的题目索引，从0开始")
    @Comment("当前进行到的题目索引")
    private Integer currentQuestionIndex;

    /**
     * 会话状态
     * 当前面试会话状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @FieldMeta(name = "会话状态", desc = "当前面试会话状态")
    @Comment("当前面试会话状态")
    private InterviewSessionStatus status;

    /**
     * 题目列表JSON
     * 当前面试生成的题目列表，使用JSON字符串存储
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    @FieldMeta(name = "题目列表JSON", desc = "当前面试生成的题目列表，使用JSON字符串存储")
    @Comment("面试题目列表JSON")
    private String questionsJson;

    /**
     * 创建时间
     * 面试会话创建时间
     */
    @Column(nullable = false)
    @FieldMeta(name = "创建时间", desc = "面试会话创建时间")
    @Comment("面试会话创建时间")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (currentQuestionIndex == null) {
            currentQuestionIndex = 0;
        }
        if (status == null) {
            status = InterviewSessionStatus.CREATED;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
