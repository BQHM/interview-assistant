package com.interview.modules.resume.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_analyses")
@Comment("简历分析结果表")
@Getter
@Setter
public class ResumeAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("分析结果主键")
    private Long id;

    // 关联的简历
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    @Comment("关联的简历记录")
    private ResumeEntity resume;

    // 总分 (0-100)
    @Comment("简历综合评分")
    private Integer overallScore;

    // 各维度评分
    @Comment("内容完整性评分")
    private Integer contentScore;      // 内容完整性 (0-25)

    @Comment("结构清晰度评分")
    private Integer structureScore;    // 结构清晰度 (0-20)

    @Comment("技能匹配度评分")
    private Integer skillMatchScore;   // 技能匹配度 (0-25)

    @Comment("表达专业性评分")
    private Integer expressionScore;   // 表达专业性 (0-15)

    @Comment("项目经验评分")
    private Integer projectScore;      // 项目经验 (0-15)

    // 简历摘要
    @Column(columnDefinition = "TEXT")
    @Comment("AI 生成的简历摘要")
    private String summary;

    // 优点列表 (JSON格式)
    @Column(columnDefinition = "TEXT")
    @Comment("简历优点列表，JSON 字符串格式")
    private String strengthsJson;

    // 改进建议列表 (JSON格式)
    @Column(columnDefinition = "TEXT")
    @Comment("简历改进建议列表，JSON 字符串格式")
    private String suggestionsJson;

    // 评测时间
    @Column(nullable = false)
    @Comment("简历分析完成时间")
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
}
