import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { interviewApi } from '../api/interview';
import { resumeApi } from '../api/resume';
import type { ResumeDetail } from '../types/resume';

export default function ResumeDetailPage() {
  // id 来自路由 /resumes/:id，例如 /resumes/11。
  // useParams 取到的路径参数默认是 string | undefined，所以后面请求接口时要 Number(id)。
  const { id } = useParams();
  const navigate = useNavigate();

  // resume 保存详情数据；creatingInterview 用来避免重复点击“开始模拟面试”。
  // 初始值是 null，表示接口还没有把简历详情加载回来。
  const [resume, setResume] = useState<ResumeDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [creatingInterview, setCreatingInterview] = useState(false);

  // id 变化时重新加载简历详情。
  // 依赖数组 [id] 的意思是：只要路由里的 id 变化，就重新执行这个 effect。
  useEffect(() => {
    if (!id) {
      setError('简历 ID 不存在');
      setLoading(false);
      return;
    }

    const loadResume = async () => {
      try {
        setLoading(true);
        // 后端接口需要 number 类型的 id，所以这里把字符串 id 转成数字。
        const data = await resumeApi.getResumeDetail(Number(id));
        setResume(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : '加载简历详情失败');
      } finally {
        setLoading(false);
      }
    };

    loadResume();
  }, [id]);

  // 用当前简历创建面试会话，成功后跳转到答题页面。
  const handleStartInterview = async () => {
    if (!resume) {
      // 理论上走到正式页面时 resume 一定存在，这里是防御性判断。
      return;
    }

    try {
      setCreatingInterview(true);

      const session = await interviewApi.createInterview({
        // 用当前详情页这份简历创建面试。
        resumeId: resume.id,
        // 这里先固定 5 题；如果要让用户选择题数，可以从 InterviewCenterPage 发起。
        questionCount: 5,
      });

      navigate(`/interview/${session.sessionId}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : '创建面试失败');
    } finally {
      setCreatingInterview(false);
    }
  };

  // 加载中优先返回，不渲染正式详情。
  if (loading) {
    return <div>加载中...</div>;
  }

  // 有错误时显示错误信息。
  if (error) {
    return <div className="text-red-600">{error}</div>;
  }

  // 没有错误但 resume 仍为空，说明后端没有返回有效数据。
  if (!resume) {
    return <div>简历不存在</div>;
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-2">简历详情</h1>
      <p className="text-slate-500 mb-6">查看简历解析内容和分析状态</p>
      <button
        onClick={handleStartInterview}
        // 创建中禁用按钮，避免连续创建多个面试会话。
        disabled={creatingInterview}
        className="mb-6 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
      >
        {creatingInterview ? '创建中...' : '开始模拟面试'}
      </button>
      <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">基础信息</h2>
        <div className="space-y-2 text-sm text-slate-600">
          <p>文件名：{resume.originalFilename}</p>
          <p>文件大小：{resume.fileSize} 字节</p>
          <p>文件类型：{resume.contentType}</p>
          <p>分析状态：{resume.analyzeStatus}</p>
          <p>上传时间：{resume.uploadedAt}</p>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">简历正文</h2>
        {/* pre 会保留换行和空格，适合展示后端解析出的简历文本 */}
        <pre className="whitespace-pre-wrap text-sm text-slate-700 leading-6">
          {resume.resumeText}
        </pre>
      </div>
    </div>
  );
}
