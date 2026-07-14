import { AlertCircle, CheckCircle, FileText, Loader2, Upload } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getErrorMessage } from '../api/request';
import { resumeApi } from '../api/resume';

export default function ResumeUploadPage() {
  // 上传成功后需要跳转到详情页，所以这里引入 navigate。
  const navigate = useNavigate();

  // selectedFile 保存用户当前选择的文件。
  // uploading/error/uploadSuccess 分别控制上传按钮、错误提示和重复文件提示。
  // File | null 表示：可能有文件，也可能还没选择文件。
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [uploadSuccess, setUploadSuccess] = useState(false);

  // 处理普通文件选择，也就是点击“选择文件”后的 input change 事件。
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    // e.target.files 是浏览器提供的文件列表；?. 表示安全访问，避免空值报错。
    const file = e.target.files?.[0] || null;
    setSelectedFile(file);
    // 重新选择文件时，清空旧的错误和成功提示。
    setError(null);
    setUploadSuccess(false);
  };

  // 处理拖拽上传，浏览器默认会打开文件，所以要先 preventDefault。
  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    // 拖进来的文件也从 dataTransfer.files 里取第一份。
    const file = e.dataTransfer.files?.[0] || null;
    setSelectedFile(file);
    setError(null);
    setUploadSuccess(false);
  };

  const handleDragOver = (e: React.DragEvent) => {
    // 必须阻止默认行为，浏览器才允许触发 drop。
    e.preventDefault();
  };

  // 上传成功后跳转到简历详情页，让用户直接看到解析结果。
  const handleUpload = async () => {
    // 没有选择文件时不允许上传。
    if (!selectedFile) return;

    try {
      // 设置 uploading 后，按钮会显示“上传中...”并禁用重复点击。
      setUploading(true);
      setError(null);

      const result = await resumeApi.uploadResume(selectedFile);

      if (result.duplicate) {
        // 后端判断文件重复时，不重新创建简历，直接跳到已有简历详情。
        setUploadSuccess(true);
        // 给用户 1.5 秒看到提示，再跳转。
        setTimeout(() => {
          navigate(`/resumes/${result.resumeId}`);
        }, 1500);
      } else {
        navigate(`/resumes/${result.resumeId}`);
      }
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      // finally 无论成功失败都会执行，用来恢复按钮状态。
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
        // 拖拽文件松手时触发。
        onDrop={handleDrop}
        // 拖拽经过区域时触发，用来允许 drop。
        onDragOver={handleDragOver}
        className="bg-white rounded-xl border-2 border-dashed border-slate-300 p-12 text-center hover:border-blue-400 transition-colors"
      >
        {/* 三元表达式：没有选文件显示“选择文件”，选中文件后显示文件信息和上传按钮 */}
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
              {/* input 被 hidden 隐藏，外层 label 负责展示漂亮按钮；点 label 会触发文件选择 */}
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

            {/* 上传成功且命中重复文件时，显示绿色提示；否则显示操作按钮 */}
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
                  // 上传中禁用按钮，避免连续点两次发出重复请求。
                  disabled={uploading}
                  className="flex items-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  {/* 根据 uploading 切换按钮里的图标和文字 */}
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
