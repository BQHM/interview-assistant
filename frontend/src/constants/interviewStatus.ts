import type { InterviewSessionStatus } from '../types/interview';

export const INTERVIEW_STATUS_TEXT: Record<InterviewSessionStatus, string> = {
  CREATED: '未开始',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
};
