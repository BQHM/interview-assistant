package com.interview.modules.interview.model;

/**
 * 文件功能说明
 * <p>保存历史面试题目的简化信息，用于生成新题时提醒 AI 避免重复。</p>
 *
 * <p>它不是一次完整面试题目的持久化实体，只是从历史会话的
 * {@code questionsJson} 中提取出来的临时数据对象。</p>
 *
 * @param question 历史题目正文
 * @param type     历史题目类型
 * @param topicSummary 知识点摘要
 * @author NobuNo
 * @date 2026-08-31
 */
public record HistoricalQuestion(String question, String type, String topicSummary) {
}
