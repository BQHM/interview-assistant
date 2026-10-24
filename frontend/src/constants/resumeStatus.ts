import type { AsyncTaskStatus } from '../types/resume';

export const RESUME_STATUS_TEXT: Record<AsyncTaskStatus, string> = {
  PENDING: '等待中',
  PROCESSING: '分析中',
  COMPLETED: '已完成',
  FAILED: '失败',
};


