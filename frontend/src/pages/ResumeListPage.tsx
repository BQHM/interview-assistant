import { FileText, Loader2, Plus } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { resumeApi } from '../api/resume';
import { EmptyState, ErrorNotice } from '../components/Feedback';
import { ResumeStatusBadge } from '../components/ResumeStatusBadge';
import type { ResumeListItem } from '../types/resume';
import { formatDateTime, formatFileSize } from '../utils/format';

export default function ResumeListPage() {
  // useNavigate 返回一个函数，调用 navigate('/xxx') 就能用代码跳转页面。
  // 它适合放在按钮点击事件里；如果只是普通链接，也可以用 Link。
  const navigate = useNavigate();

  // resumes 是后端返回的简历列表；loading/error 用来控制加载态和错误态。
  // useState 的写法可以理解成：[当前值, 修改当前值的函数]。
  // 调用 setResumes / setLoading / setError 后，React 会重新渲染页面。
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // useEffect 会在页面第一次渲染完成后执行，适合用来调用后端接口加载初始数据。
  // 第二个参数 [] 表示“只在页面首次进入时执行一次”。
  useEffect(() => {
    loadResumes();
  }, []);

  // 调用后端简历列表接口，并把结果保存到组件状态中。
  const loadResumes = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await resumeApi.getResumeList();
      setResumes(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '加载失败');
    } finally {
      setLoading(false);
    }
  };

  // loading 为 true 时，直接返回加载中页面，不继续渲染下面的列表。
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

  // error 有值时，直接返回错误页面，并提供“重试”按钮重新调用 loadResumes。
  if (error) {
    return (
      <div className="space-y-4">
        <ErrorNotice message={error} />
        <button
          onClick={loadResumes}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
        >
          重试
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto">
      {/* 页面标题和操作 */}
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">简历管理</h1>
          <p className="text-slate-500 mt-1">管理和分析您的简历</p>
        </div>
        {/* Link 是声明式跳转，点击后进入上传页，不会刷新整个浏览器页面 */}
        <Link
          to="/upload"
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-4 h-4" />
          上传简历
        </Link>
      </div>

      {/* 根据列表是否为空，分别显示空状态或简历卡片列表 */}
      {resumes.length === 0 ? (
        <EmptyState title='暂无简历' description='上传您的第一份简历开始体验' />
      ) : (
        <div className="space-y-4">
          {/* map 会把简历数组转换成多个卡片；key 帮 React 识别每一项是谁 */}
          {resumes.map((resume) => (
            <div
              key={resume.id}
              className="bg-white rounded-xl border border-slate-200 p-6 hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between">
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                    <FileText className="w-6 h-6 text-blue-600" />
                  </div>
                  <div>
                    <h3 className="font-medium text-slate-800">{resume.filename}</h3>
                    <div className="flex items-center gap-4 mt-2 text-sm text-slate-500">
                      <span>{formatFileSize(resume.fileSize)}</span>
                      <span>{formatDateTime(resume.uploadedAt)}</span>
                      <ResumeStatusBadge status={resume.analyzeStatus} />
                    </div>
                    {/* && 是条件渲染：只有 analyzeError 有值时才显示错误信息 */}
                    {resume.analyzeError && (
                      <div className="mt-3">
                        <ErrorNotice message={resume.analyzeError} />
                      </div>
                    )}
                  </div>
                </div>
                <button
                  // 点击详情按钮时，用当前简历 id 拼出详情页路径。
                  onClick={() => navigate(`/resumes/${resume.id}`)}
                  className="px-4 py-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                >
                  查看详情
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
