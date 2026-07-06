import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FileText, Plus, Loader2 } from 'lucide-react';
import { resumeApi } from '../api/resume';
import type { ResumeListItem } from '../types/resume';

export default function ResumeListPage() {
  const navigate = useNavigate();
  const [resumes, setResumes] = useState<ResumeListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 加载简历列表
  useEffect(() => {
    loadResumes();
  }, []);

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

  // 格式化文件大小
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  // 格式化日期
  const formatDate = (dateStr: string): string => {
    return new Date(dateStr).toLocaleString('zh-CN');
  };

  // 获取状态标签
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
        {config.label}
      </span>
    );
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    );
  }

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
        <Link
          to="/upload"
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <Plus className="w-4 h-4" />
          上传简历
        </Link>
      </div>

      {/* 简历列表 */}
      {resumes.length === 0 ? (
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
                    {resume.analyzeError && (
                      <p className="text-sm text-red-600 mt-2">{resume.analyzeError}</p>
                    )}
                  </div>
                </div>
                <button
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
