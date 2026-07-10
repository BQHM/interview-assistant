import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { interviewApi } from '../api/interview';
import type { InterviewSessionListItem } from '../types/interview';

export default function InterviewHistoryPage() {
  // useLocation 可以读取上一个页面跳转时传过来的 state。
  // 这里用于判断是否刚从“完成面试”跳转过来。
  const location = useLocation();
  // location.state 是 unknown 结构，所以这里用类型断言告诉 TS：里面可能有 completedSessionId。
  const completedSessionId = (location.state as { completedSessionId?: string } | null)?.completedSessionId;

  // sessions 保存历史面试列表；loading/error 控制加载态和错误态。
  // 历史页现在只展示列表，后续可以在每张卡片上加“继续面试 / 查看报告”按钮。
  const [sessions, setSessions] = useState<InterviewSessionListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');


  const getStatusText = (status: string) => {
    if (status === 'IN_PROGRESS') {
      return '进行中';
    }

    if (status === 'COMPLETED') {
      return '已完成';
    }

    return status;
  };
  // 页面首次进入时加载历史记录。
  useEffect(() => {
    const loadHistory = async () => {
      try {
        setLoading(true);
        // 调用 GET /api/interviews，获取所有面试会话的轻量列表。
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

  // 加载中直接返回加载提示。
  if (loading) {
    return <div>加载中...</div>;
  }

  // 有错误时直接返回错误提示。
  if (error) {
    return <div className="text-red-600">{error}</div>;
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-2">面试记录</h1>
      <p className="text-slate-500 mb-6">查看历史模拟面试记录</p>

      {/* 从答题页完成后跳转过来时，显示一次完成提示 */}
      {completedSessionId && (
        <div className="mb-6 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
          面试已完成，记录已保存。
        </div>
      )}

      {/* 空数组显示空状态；有数据时渲染每一条历史面试卡片 */}
      {sessions.length === 0 ? (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          暂无面试记录
        </div>
      ) : (
        <div className="space-y-4">
          {/* map 把 sessions 数组渲染成多张历史记录卡片 */}
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
                已完成题数：{Math.min(session.currentQuestionIndex, session.totalQuestions)} / {session.totalQuestions}
              </p>
              <p className="text-sm text-slate-500">
                状态：{getStatusText(session.status)}
              </p>
              <p className="text-sm text-slate-500">
                创建时间：{session.createdAt}
              </p>

              <div className="mt-4">
                {session.status === 'IN_PROGRESS' ? (
                  <Link
                    to={`/interview/${session.sessionId}`}
                    className="text-sm text-blue-600 hover:text-blue-700"
                  >
                    继续面试
                  </Link>
                ) : (
                  <Link
                    to={`/interviews/${session.sessionId}/report`}
                    className="text-sm text-blue-600 hover:text-blue-700"
                  >
                    查看报告
                  </Link>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
