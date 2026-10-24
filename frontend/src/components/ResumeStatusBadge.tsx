import { RESUME_STATUS_TEXT } from '../constants/resumeStatus';
import type { AsyncTaskStatus } from '../types/resume';

interface ResumeStatusBadgeProps {
  status: AsyncTaskStatus;
}

// 配色属于展示细节，跟着组件走；文案属于业务数据，放在 constants 里。
const STATUS_CLASS: Record<AsyncTaskStatus, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  PROCESSING: 'bg-blue-100 text-blue-800',
  COMPLETED: 'bg-green-100 text-green-800',
  FAILED: 'bg-red-100 text-red-800',
};

export function ResumeStatusBadge({ status }: ResumeStatusBadgeProps) {
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${STATUS_CLASS[status]}`}>
      {RESUME_STATUS_TEXT[status]}
    </span>
  );
}