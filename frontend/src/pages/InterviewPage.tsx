import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { interviewApi } from '../api/interview';
import type { InterviewSession } from '../types/interview';

export default function InterviewPage() {
    const { sessionId } = useParams();
    const [session, setSession] = useState<InterviewSession | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [answer, setAnswer] = useState('');
    const [submitting, setSubmitting] = useState(false);

    useEffect(() => {
        if (!sessionId) {
            setError('面试会话 ID 不存在');
            setLoading(false);
            return;
        }

        const loadSession = async () => {
            try {
                setLoading(true);
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

    if (loading) {
        return <div>加载中...</div>;
    }

    if (error) {
        return <div className="text-red-600">{error}</div>;
    }

    if (!session) {
        return <div>面试会话不存在</div>;
    }

    const handleSubmitAnswer = async () => {
        if (!session || !answer.trim()) {
            return;
        }

        try {
            setSubmitting(true);

            const result = await interviewApi.submitAnswer({
                sessionId: session.sessionId,
                questionIndex: session.currentQuestionIndex,
                answer: answer.trim(),
            });

            setAnswer('');

            if (result.hasNextQuestion) {
                const updatedSession = await interviewApi.getInterviewSession(session.sessionId);
                setSession(updatedSession);
            } else {
                alert('面试已完成');
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : '提交答案失败');
        } finally {
            setSubmitting(false);
        }
    };

    const currentQuestion = session.questions[session.currentQuestionIndex];

    return (
        <div className="max-w-3xl mx-auto">
            <h1 className="text-2xl font-bold text-slate-800 mb-2">文字模拟面试</h1>
            <p className="text-slate-500 mb-6">
                第 {session.currentQuestionIndex + 1} / {session.totalQuestions} 题
            </p>

            <div className="bg-white rounded-xl border border-slate-200 p-6">
                <div className="mb-4">
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
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                    rows={8}
                    className="w-full border border-slate-300 rounded-lg p-3 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="请输入你的回答..."
                />

                <div className="mt-4 flex justify-end">
                    <button
                        onClick={handleSubmitAnswer}
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