// 导入统一请求工具。
import { request } from './request';

// 导入 Skill 的 TypeScript 类型。
import type { InterviewSkill } from '../types/skill';

// Skill 相关接口。
export const skillApi = {
  // 查询全部面试方向。
  // 返回值是一个 InterviewSkill 数组。
  async listSkills(): Promise<InterviewSkill[]> {
    return request.get<InterviewSkill[]>('/api/interview/skills');
  },
};