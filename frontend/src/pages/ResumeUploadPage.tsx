import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Upload, Loader2, FileText, AlertCircle, CheckCircle } from 'lucide-react';
import { resumeApi } from '../api/resume';
import { getErrorMessage } from '../api/request';

export default function ResumeUploadPage() {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [uploadSuccess, setUploadSuccess] = useState(false);

  // 处理文件选择
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    setSelectedFile(file);
    setError(null);
    setUploadSuccess(false);
  };

  // 处理文件拖放
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0] || null;
    setSelectedFile(file);
    setError(null);
    setUploadSuccess(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  // 上传简历
  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setUploading(true);
      setError(null);

      const result = await resumeApi.uploadResume(selectedFile);

      if (result.duplicate) {
        setUploadSuccess(true);
        setTimeout(() => {
          navigate(`/resumes/${result.resumeId}`);
        }, 1500);
      } else {
        navigate(`/resumes/${result.resumeId}`);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto">
      {/* 页面标题 */}
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-800">上传简历</h1>
        <p className="text-slate-500 mt-1">上传您的简历，AI 将为您进行分析</p>
      </div>

      {/* 上传区域 */}
      <div
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        className="bg-white rounded-xl border-2 border-dashed border-slate-300 p-12 text-center hover:border-blue-400 transition-colors"
      >
        {!selectedFile ? (
          <div>
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Upload className="w-8 h-8 text-blue-600" />
            </div>
            <h3 className="text-lg font-medium text-slate-700 mb-2">拖拽文件到这里</h3>
            <p className="text-slate-500 mb-4">或者点击选择文件</p>
            <label className="inline-flex items-center justify-center px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 cursor-pointer transition-colors">
              <Upload className="w-4 h-4 mr-2" />
              选择文件
              <input
                type="file"
                className="hidden"
                accept=".pdf,.doc,.docx,.txt"
                onChange={handleFileSelect}
                disabled={uploading}
              />
            </label>
            <p className="text-xs text-slate-400 mt-4">支持 PDF、DOC、DOCX、TXT 格式</p>
          </div>
        ) : (
          <div>
            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <FileText className="w-8 h-8 text-blue-600" />
            </div>
            <h3 className="text-lg font-medium text-slate-700 mb-1">{selectedFile.name}</h3>
            <p className="text-slate-500 mb-4">
              {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
            </p>

            {uploadSuccess ? (
              <div className="flex items-center justify-center gap-2 text-green-600 mb-4">
                <CheckCircle className="w-5 h-5" />
                <span>该简历已存在，正在跳转到详情页...</span>
              </div>
            ) : (
              <div className="flex items-center justify-center gap-4">
                <button
                  onClick={() => setSelectedFile(null)}
                  className="px-4 py-2 text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                  disabled={uploading}
                >
                  重新选择
                </button>
                <button
                  onClick={handleUpload}
                  disabled={uploading}
                  className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {uploading ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      上传中...
                    </>
                  ) : (
                    <>
                      <Upload className="w-4 h-4" />
                      上传
                    </>
                  )}
                </button>
              </div>
            )}
          </div>
        )}
      </div>

      {/* 错误提示 */}
      {error && (
        <div className="mt-6 p-4 bg-red-50 border border-red-200 rounded-lg flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0" />
          <p className="text-red-600">{error}</p>
        </div>
      )}
    </div>
  );
}
