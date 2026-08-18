// Skill 分类的优先级。
// 后端会根据这个优先级分配题目数量。
export type SkillPriority = 'CORE' | 'NORMAL' | 'ALWAYS_ONE';

// 一个 Skill 下的题目分类。
// 例如：Java 基础、系统设计、高可用。
export interface SkillCategory {
  // 分类编号，例如 JAVA、HIGH_AVAILABILITY。
  key: string;

  // 展示给用户看的分类名称。
  label: string;

  // 分类优先级。
  priority: SkillPriority;
}

// 面试方向 Skill。
// 对应后端 InterviewSkillDTO。
export interface InterviewSkill {
  // Skill 编号，用来创建面试时提交给后端。
  // 例如 java-backend、system-design。
  id: string;

  // Skill 展示名称。
  // 例如 Java 后端、系统设计。
  name: string;

  // Skill 简要说明。
  description: string;

  // AI 出题时使用的面试官角色和出题规则。
  // 当前前端可以接收，但暂时不直接展示。
  persona: string;

  // 当前 Skill 包含的分类列表。
  categories: SkillCategory[];
}