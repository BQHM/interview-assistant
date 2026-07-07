import { useEffect, useState } from 'react';
import { interviewApi } from '../api/interview';
import type { InterviewSessionListItem } from '../types/interview';

export default function InterviewHistoryPage() {
  const [sessions, setSessions] = useState<InterviewSessionListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const loadHistory = async () => {
      try {
        setLoading(true);
        const data = await interviewApi.getInterviewHistory();
        setSessions(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载面试记录失败');
      } finally {
        setLoading(false);
      }
    };

    loadHistory();
  }, []);

  if (loading) {
    return <div>加载中...</div>;
  }

  if (error) {
    return <div className="text-red-600">{error}</div>;
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-2">面试记录</h1>
      <p className="text-slate-500 mb-6">查看历史模拟面试记录</p>

      {sessions.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          暂无面试记录
        </div>
      ) : (
        <div className="space-y-4">
          {sessions.map((session) => (
            <div
              key={session.sessionId}
              className="bg-white rounded-xl border border-slate-200 p-6"
            >
              <p className="font-medium text-slate-800">
                会话 ID：{session.sessionId}
              </p>
              <p className="text-sm text-slate-500 mt-2">
                简历 ID：{session.resumeId}
              </p>
              <p className="text-sm text-slate-500">
                进度：{session.currentQuestionIndex} / {session.totalQuestions}
              </p>
              <p className="text-sm text-slate-500">
                状态：{session.status}
              </p>
              <p className="text-sm text-slate-500">
                创建时间：{session.createdAt}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}