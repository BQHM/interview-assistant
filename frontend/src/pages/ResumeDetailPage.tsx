import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { resumeApi } from '../api/resume';
import type { ResumeDetail } from '../types/resume';

export default function ResumeDetailPage() {
  const { id } = useParams();
  const [resume, setResume] = useState<ResumeDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!id) {
      setError('简历 ID 不存在');
      setLoading(false);
      return;
    }

    const loadResume = async () => {
      try {
        setLoading(true);
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

  if (loading) {
    return <div>加载中...</div>;
  }

  if (error) {
    return <div className="text-red-600">{error}</div>;
  }

  if (!resume) {
    return <div>简历不存在</div>;
  }

  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-2xl font-bold text-slate-800 mb-2">简历详情</h1>
      <p className="text-slate-500 mb-6">查看简历解析内容和分析状态</p>

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
        <pre className="whitespace-pre-wrap text-sm text-slate-700 leading-6">
          {resume.resumeText}
        </pre>
      </div>
    </div>
  );
}