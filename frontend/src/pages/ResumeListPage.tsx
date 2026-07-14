import { FileText, Loader2, Plus } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { resumeApi } from '../api/resume';
import type { ResumeListItem } from '../types/resume';

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

  // 把后端返回的字节数转换成更适合用户阅读的单位。
  const formatFileSize = (bytes: number): string => {
    // 小于 1024 字节，直接显示 B。
    if (bytes < 1024) return bytes + ' B';
    // 小于 1MB，显示 KB。
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    // 更大的文件显示 MB。
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  // 把后端返回的时间字符串转换成本地中文时间格式。
  const formatDate = (dateStr: string): string => {
    return new Date(dateStr).toLocaleString('zh-CN');
  };

  // 根据后端状态返回一个彩色标签。
  // Record<string, ...> 可以理解成“一个字符串 key 到配置对象的映射表”。
  const getStatusBadge = (status: string) => {
    const statusMap: Record<string, { label: string; className: string }> = {
      PENDING: { label: '等待中', className: 'bg-yellow-100 text-yellow-800' },
      PROCESSING: { label: '分析中', className: 'bg-blue-100 text-blue-800' },
      COMPLETED: { label: '已完成', className: 'bg-green-100 text-green-800' },
      FAILED: { label: '失败', className: 'bg-red-100 text-red-800' },
    };
    const config = statusMap[status] || { label: status, className: 'bg-gray-100 text-gray-800' };

    return (
      <span className={`px-2 py-1 rounded-full text-xs font-medium ${config.className}`}>
        {/* 这里显示中文状态文字，例如“已完成” */}
        {config.label}
      </span>
    );
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
      <div className="text-center py-10">
        <p className="text-red-600 mb-4">{error}</p>
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
        // 空状态：没有简历时引导用户先上传。
        <div className="text-center py-16 bg-white rounded-xl border border-slate-200">
          <FileText className="w-16 h-16 text-slate-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-slate-700 mb-2">暂无简历</h3>
          <p className="text-slate-500 mb-6">上传您的第一份简历开始体验</p>
          <Link
            to="/upload"
            className="inline-flex items-center gap-2 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4" />
            上传简历
          </Link>
        </div>
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
                      <span>{formatDate(resume.uploadedAt)}</span>
                      {getStatusBadge(resume.analyzeStatus)}
                    </div>
                    {/* && 是条件渲染：只有 analyzeError 有值时才显示错误信息 */}
                    {resume.analyzeError && (
                      <p className="text-sm text-red-600 mt-2">{resume.analyzeError}</p>
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
