import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { interviewApi } from '../api/interview';
import { resumeApi } from '../api/resume';
import type { ResumeListItem } from '../types/resume';

export default function InterviewCenterPage() {
  // 创建面试成功后，需要跳转到答题页。
  const navigate = useNavigate();

  // resumes 用来填充“选择简历”的下拉框。
  // selectedResumeId 和 questionCount 是用户创建面试前选择的参数。
  // 这里 selectedResumeId 用字符串保存，是因为 select 的 value 天然是字符串。
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [selectedResumeId, setSelectedResumeId] = useState('');
  const [questionCount, setQuestionCount] = useState(5);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');

  // 页面进入时先加载简历列表；如果有简历，默认选中第一份。
  // 这样用户进入页面后不需要手动选择，也能直接开始面试。
  useEffect(() => {
    const loadResumes = async () => {
      try {
        setLoading(true);
        const data = await resumeApi.getResumeList();
        setResumes(data);

        if (data.length > 0) {
          // select 的 value 用字符串，所以这里把 number id 转成 string。
          setSelectedResumeId(String(data[0].id));
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载简历列表失败');
      } finally {
        setLoading(false);
      }
    };

    loadResumes();
  }, []);

  // 调用后端创建面试会话，成功后跳转到 /interview/{sessionId} 答题页。
  const handleStartInterview = async () => {
    if (!selectedResumeId) {
      setError('请选择一份简历');
      return;
    }

    try {
      setStarting(true);
      setError('');

      const session = await interviewApi.createInterview({
        resumeId: Number(selectedResumeId),
        questionCount,
      });

      navigate(`/interview/${session.sessionId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建面试失败');
    } finally {
      setStarting(false);
    }
  };

  // loading 为 true 时只显示加载提示，避免简历列表还没回来就渲染空下拉框。
  if (loading) {
    return <div>加载中...</div>;
  }

  return (
    <div className="max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-2">模拟面试</h1>
      <p className="text-slate-500 mb-6">
        选择简历和题目数量，开始文字模拟面试。
      </p>

      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <div className="mb-5">
          <label className="block text-sm font-medium text-slate-700 mb-2">
            选择简历
          </label>

          {resumes.length === 0 ? (
            <div className="text-sm text-slate-500">
              暂无简历，请先上传简历。
            </div>
          ) : (
            // 这是受控表单：value 来自 selectedResumeId，onChange 负责更新 selectedResumeId。
            <select
              value={selectedResumeId}
              onChange={(e) => setSelectedResumeId(e.target.value)}
              className="w-full border border-slate-300 rounded-lg p-2 text-sm"
            >
              {/* 把后端简历列表渲染成 option 下拉选项 */}
              {resumes.map((resume) => (
                <option key={resume.id} value={resume.id}>
                  {resume.filename}
                </option>
              ))}
            </select>
          )}
        </div>

        <div className="mb-5">
          <label className="block text-sm font-medium text-slate-700 mb-2">
            题目数量
          </label>

          <select
            value={questionCount}
            // select 取出来的是字符串，所以这里要 Number(...) 转回数字。
            onChange={(e) => setQuestionCount(Number(e.target.value))}
            className="w-full border border-slate-300 rounded-lg p-2 text-sm"
          >
            <option value={3}>3 题</option>
            <option value={5}>5 题</option>
            <option value={8}>8 题</option>
          </select>
        </div>

        {error && (
          // 创建面试失败或未选择简历时，在按钮上方显示错误。
          <div className="mb-4 text-sm text-red-600">
            {error}
          </div>
        )}

        <button
          onClick={handleStartInterview}
          // 正在创建或没有简历时禁用按钮。
          disabled={starting || resumes.length === 0}
          className="px-5 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
        >
          {/* 根据 starting 切换按钮文案，告诉用户请求正在进行 */}
          {starting ? '创建中...' : '开始面试'}
        </button>
      </div>
    </div>
  );
}
