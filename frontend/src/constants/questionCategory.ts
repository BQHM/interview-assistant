// 面试题分类的中文显示。
// key 与后端 skills/*/skill.meta.yml 的 category key 保持一致，
// value 是对用户展示的 label。
// 注意：后端 type/category 也可能是 AI 生成的自由文本，
// 所以这里用 Record<string, string>，并且调用方必须保留兜底。
export const QUESTION_CATEGORY_TEXT: Record<string, string> = {
  JAVA: 'Java 基础',
  SPRING_BOOT: 'Spring Boot',
  MYSQL: 'MySQL',
  REDIS: 'Redis',
  PROJECT: '项目经验',
  SYSTEM_DESIGN_SCENARIO: '系统设计场景',
  HIGH_AVAILABILITY: '高可用',
  DISTRIBUTED: '分布式',
  CACHE: '缓存设计',
  DB_DESIGN: '数据库设计',
  GENERAL: '综合',
};

// 后端字段可能是受控 key，也可能是已经中文化的文本，未知值原样返回。
export function formatQuestionLabel(value: string): string {
  return QUESTION_CATEGORY_TEXT[value] ?? value;
}