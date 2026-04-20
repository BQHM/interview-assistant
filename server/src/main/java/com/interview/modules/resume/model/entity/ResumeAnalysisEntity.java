package com.interview.modules.resume.model.entity;

import com.interview.common.annotation.FieldMeta;
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
    /**
     * 分析结果ID
     * 简历分析结果主键
     */
    @FieldMeta(name = "分析结果ID", desc = "简历分析结果主键")
    @Comment("分析结果主键")
    private Long id;

    // 关联的简历
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    /**
     * 关联简历
     * 当前分析结果对应的简历记录
     */
    @FieldMeta(name = "关联简历", desc = "当前分析结果对应的简历记录")
    @Comment("关联的简历记录")
    private ResumeEntity resume;

    // 综合评分（百分制）
    /**
     * 综合评分
     * 简历分析后的综合评分，采用百分制（0-100）
     */
    @FieldMeta(name = "综合评分", desc = "简历分析后的综合评分，采用百分制（0-100）")
    @Comment("简历综合评分，百分制（0-100）")
    private Integer overallScore;

    // 各维度评分（百分制）
    /**
     * 内容完整性评分
     * 内容完整性维度评分，采用百分制（0-100）
     */
    @FieldMeta(name = "内容完整性评分", desc = "内容完整性维度评分，采用百分制（0-100）")
    @Comment("内容完整性维度评分，百分制（0-100）")
    private Integer contentScore;

    /**
     * 结构清晰度评分
     * 结构清晰度维度评分，采用百分制（0-100）
     */
    @FieldMeta(name = "结构清晰度评分", desc = "结构清晰度维度评分，采用百分制（0-100）")
    @Comment("结构清晰度维度评分，百分制（0-100）")
    private Integer structureScore;

    /**
     * 技能匹配度评分
     * 技能匹配度维度评分，采用百分制（0-100）
     */
    @FieldMeta(name = "技能匹配度评分", desc = "技能匹配度维度评分，采用百分制（0-100）")
    @Comment("技能匹配度维度评分，百分制（0-100）")
    private Integer skillMatchScore;

    /**
     * 表达专业性评分
     * 表达专业性维度评分，采用百分制（0-100）
     */
    @FieldMeta(name = "表达专业性评分", desc = "表达专业性维度评分，采用百分制（0-100）")
    @Comment("表达专业性维度评分，百分制（0-100）")
    private Integer expressionScore;

    /**
     * 项目经验评分
     * 项目经验维度评分，采用百分制（0-100）
     */
    @FieldMeta(name = "项目经验评分", desc = "项目经验维度评分，采用百分制（0-100）")
    @Comment("项目经验维度评分，百分制（0-100）")
    private Integer projectScore;

    // 简历摘要
    @Column(columnDefinition = "TEXT")
    /**
     * 分析总结
     * AI 生成的简历总体评价
     */
    @FieldMeta(name = "分析总结", desc = "AI 生成的简历总体评价")
    @Comment("AI 生成的简历摘要")
    private String summary;

    // 优点列表 (JSON格式)
    @Column(columnDefinition = "TEXT")
    /**
     * 优点列表JSON
     * 以 JSON 字符串形式保存的简历优点列表
     */
    @FieldMeta(name = "优点列表JSON", desc = "以 JSON 字符串形式保存的简历优点列表")
    @Comment("简历优点列表，JSON 字符串格式")
    private String strengthsJson;

    // 改进建议列表 (JSON格式)
    @Column(columnDefinition = "TEXT")
    /**
     * 改进建议JSON
     * 以 JSON 字符串形式保存的简历改进建议列表
     */
    @FieldMeta(name = "改进建议JSON", desc = "以 JSON 字符串形式保存的简历改进建议列表")
    @Comment("简历改进建议列表，JSON 字符串格式")
    private String suggestionsJson;

    // 评测时间
    @Column(nullable = false)
    /**
     * 分析完成时间
     * 简历分析结果生成时间
     */
    @FieldMeta(name = "分析完成时间", desc = "简历分析结果生成时间")
    @Comment("简历分析完成时间")
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
}
