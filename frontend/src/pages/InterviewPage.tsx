import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { interviewApi } from '../api/interview';
import type { InterviewSession } from '../types/interview';

export default function InterviewPage() {
    // sessionId 来自路由 /interview/:sessionId，用来查询某一次面试会话。
    const { sessionId } = useParams();
    const navigate = useNavigate();

    // session 是整场面试的数据；answer 是当前文本框里尚未提交的回答。
    // loading/error/submitting 分别控制加载态、错误态和提交按钮状态。
    const [session, setSession] = useState<InterviewSession | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [answer, setAnswer] = useState('');
    const [submitting, setSubmitting] = useState(false);

    // 页面进入时，根据 sessionId 加载面试会话和题目快照。
    useEffect(() => {
        if (!sessionId) {
            // 路由参数不存在，说明这个页面不是通过 /interview/{sessionId} 进来的。
            setError('面试会话 ID 不存在');
            setLoading(false);
            return;
        }

        const loadSession = async () => {
            try {
                setLoading(true);
                // 根据会话编号，从后端拿整场面试详情。
                const data = await interviewApi.getInterviewSession(sessionId);
                setSession(data);
            } catch (err) {
                setError(err instanceof Error ? err.message : '加载面试会话失败');
            } finally {
                setLoading(false);
            }
        };

        loadSession();
    }, [sessionId]);

    // 下面三个 return 是典型的条件渲染：加载中、错误、无数据时不显示正式页面。
    if (loading) {
        return <div>加载中...</div>;
    }

    if (error) {
        return <div className="text-red-600">{error}</div>;
    }

    if (!session) {
        return <div>面试会话不存在</div>;
    }

    // 提交当前题答案：后端保存答案、推进题号，并返回是否还有下一题。
    const handleSubmitAnswer = async () => {
        if (!session || !answer.trim()) {
            return;
        }

        try {
            // 设置 submitting 后，按钮会禁用并显示“提交中...”。
            setSubmitting(true);

            const result = await interviewApi.submitAnswer({
                sessionId: session.sessionId,
                // 当前提交的是 currentQuestionIndex 对应的题目。
                questionIndex: session.currentQuestionIndex,
                answer: answer.trim(),
            });

            // 提交成功后清空文本框，为下一题输入做准备。
            setAnswer('');

            if (result.hasNextQuestion) {
                // 有下一题时，重新拉取会话，拿到最新 currentQuestionIndex 和题目列表。
                const updatedSession = await interviewApi.getInterviewSession(session.sessionId);
                setSession(updatedSession);
            } else {
                // 没有下一题时，直接跳到报告页，让用户立刻看到评分和反馈。
                navigate(`/interviews/${session.sessionId}/report`);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : '提交答案失败');
        } finally {
            setSubmitting(false);
        }
    };

    // 当前要展示的题目由 currentQuestionIndex 决定。
    const currentQuestion = session.questions[session.currentQuestionIndex];

    return (
        <div className="max-w-3xl mx-auto">
            <h1 className="text-2xl font-bold text-slate-800 mb-2">文字模拟面试</h1>
            <p className="text-slate-500 mb-6">
                {/* currentQuestionIndex 从 0 开始，展示给用户时 +1 更符合直觉 */}
                第 {session.currentQuestionIndex + 1} / {session.totalQuestions} 题
            </p>

            <div className="bg-white rounded-xl border border-slate-200 p-6">
                <div className="mb-4">
                    {/* category/type 是后端给题目打的分类标签 */}
                    <span className="inline-block px-2 py-1 text-xs rounded-full bg-blue-100 text-blue-700">
                        {currentQuestion.category}
                    </span>
                    <span className="inline-block ml-2 px-2 py-1 text-xs rounded-full bg-slate-100 text-slate-600">
                        {currentQuestion.type}
                    </span>
                </div>

                <h2 className="text-lg font-semibold text-slate-800 mb-4">
                    {currentQuestion.question}
                </h2>
            </div>
            <div className="bg-white rounded-xl border border-slate-200 p-6 mt-6">
                <label className="block text-sm font-medium text-slate-700 mb-2">
                    你的回答
                </label>

                <textarea
                    // 受控输入框：textarea 显示 answer，用户输入时通过 setAnswer 更新 answer。
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                    rows={8}
                    className="w-full border border-slate-300 rounded-lg p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="请输入你的回答..."
                />

                <div className="mt-4 flex justify-end">
                    <button
                        onClick={handleSubmitAnswer}
                        // 提交中或答案为空时禁用按钮。
                        disabled={submitting || !answer.trim()}
                        className="px-5 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                    >
                        {submitting ? '提交中...' : '提交答案'}
                    </button>
                </div>
            </div>
        </div>
    );
}
