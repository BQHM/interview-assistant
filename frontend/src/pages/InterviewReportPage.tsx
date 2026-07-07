import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { interviewApi } from '../api/interview';
import type { InterviewReport } from '../types/interview';

export default function InterviewReportPage() {
  // sessionId 来自路由 /interviews/:sessionId/report。
  const { sessionId } = useParams();

  // report 保存后端返回的整场面试报告。
  // loading/error 分别控制加载态和错误态。
  const [report, setReport] = useState<InterviewReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // 页面进入时，根据 sessionId 加载报告。
  useEffect(() => {
    if (!sessionId) {
      setError('面试会话 ID 不存在');
      setLoading(false);
      return;
    }

    const loadReport = async () => {
      try {
        setLoading(true);
        const data = await interviewApi.getInterviewReport(sessionId);
        setReport(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载面试报告失败');
      } finally {
        setLoading(false);
      }
    };

    loadReport();
  }, [sessionId]);

  if (loading) {
    return <div>加载中...</div>;
  }

  if (error) {
    return <div className="text-red-600">{error}</div>;
  }

  if (!report) {
    return <div>报告不存在</div>;
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-800 mb-2">面试报告</h1>
          <p className="text-slate-500">会话 ID：{report.sessionId}</p>
        </div>

        <Link
          to="/interviews"
          className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
        >
          返回记录
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <p className="text-sm text-slate-500">题目总数</p>
          <p className="mt-2 text-2xl font-bold text-slate-800">{report.totalQuestions}</p>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <p className="text-sm text-slate-500">已回答</p>
          <p className="mt-2 text-2xl font-bold text-green-600">{report.answeredQuestions}</p>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <p className="text-sm text-slate-500">未回答</p>
          <p className="mt-2 text-2xl font-bold text-orange-600">{report.unansweredQuestions}</p>
        </div>
        <div className="bg-white rounded-xl border border-slate-200 p-5">
          <p className="text-sm text-slate-500">完成状态</p>
          <p className="mt-2 text-lg font-semibold text-slate-800">
            {report.completed ? '已完成' : '未完成'}
          </p>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">整体评价</h2>
        <p className="text-slate-700 leading-7">{report.overallEvaluation}</p>
        <p className="mt-4 text-xs text-slate-400">生成时间：{report.generatedAt}</p>
      </div>

      <div className="space-y-5">
        {/* questionReports 是后端返回的单题报告列表，用来逐题复盘 */}
        {report.questionReports.map((questionReport) => (
          <section
            key={questionReport.questionIndex}
            className="bg-white rounded-xl border border-slate-200 p-6"
          >
            <div className="mb-4 flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-medium text-blue-700">
                第 {questionReport.questionIndex + 1} 题
              </span>
              <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                {questionReport.category}
              </span>
              <span className="rounded-full bg-green-50 px-3 py-1 text-xs font-medium text-green-700">
                {questionReport.score} 分
              </span>
              <span
                className={`rounded-full px-3 py-1 text-xs font-medium ${
                  questionReport.answered
                    ? 'bg-emerald-50 text-emerald-700'
                    : 'bg-orange-50 text-orange-700'
                }`}
              >
                {questionReport.answered ? '已回答' : '未回答'}
              </span>
            </div>

            <h3 className="text-base font-semibold text-slate-800 mb-4">
              {questionReport.question}
            </h3>

            <div className="space-y-4 text-sm">
              <div>
                <p className="mb-2 font-medium text-slate-700">你的回答</p>
                <div className="rounded-lg bg-slate-50 p-4 text-slate-700 leading-6">
                  {questionReport.userAnswer || '未作答'}
                </div>
              </div>

              <div>
                <p className="mb-2 font-medium text-slate-700">评价反馈</p>
                <div className="rounded-lg bg-blue-50 p-4 text-blue-900 leading-6">
                  {questionReport.evaluation}
                </div>
              </div>

              {questionReport.referenceAnswer && (
                <div>
                  <p className="mb-2 font-medium text-slate-700">参考答案</p>
                  <div className="rounded-lg bg-green-50 p-4 text-green-900 leading-6">
                    {questionReport.referenceAnswer}
                  </div>
                </div>
              )}

              {questionReport.keyPoints.length > 0 && (
                <div>
                  <p className="mb-2 font-medium text-slate-700">关键点</p>
                  <ul className="list-disc space-y-1 pl-5 text-slate-700">
                    {questionReport.keyPoints.map((keyPoint, index) => (
                      <li key={index}>{keyPoint}</li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
