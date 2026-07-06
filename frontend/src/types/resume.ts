// 异步任务状态
export type AsyncTaskStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

// 简历上传响应
export interface ResumeUploadResponse {
  resumeId: number;
  filename: string;
  storageKey: string;
  analyzeStatus: AsyncTaskStatus;
  duplicate: boolean;
}

// 简历列表项
export interface ResumeListItem {
  id: number;
  filename: string;
  fileSize: number;
  uploadedAt: string;
  analyzeStatus: AsyncTaskStatus;
  analyzeError?: string;
}

// 评分详情
export interface ScoreDetail {
  contentScore: number;      // 内容完整性
  structureScore: number;    // 结构清晰度
  skillMatchScore: number;   // 技能匹配度
  expressionScore: number;   // 表达专业性
  projectScore: number;      // 项目经验
}

// 改进建议
export interface Suggestion {
  category: string;
  priority: string;
  issue: string;
  recommendation: string;
}

// 简历分析结果
export interface ResumeAnalysis {
  resumeId: number;
  overallScore: number;
  scoreDetail: ScoreDetail;
  summary: string;
  strengths: string[];
  suggestions: Suggestion[];
  analyzedAt: string;
}

// 分析历史项
export interface AnalysisHistory {
  id: number;
  overallScore: number;
  contentScore: number;
  structureScore: number;
  skillMatchScore: number;
  expressionScore: number;
  projectScore: number;
  summary: string;
  analyzedAt: string;
}

// 简历详情
export interface ResumeDetail {
  id: number;
  originalFilename: string;
  fileSize: number;
  contentType: string;
  storageKey: string;
  resumeText: string;
  uploadedAt: string;
  analyzeStatus: AsyncTaskStatus;
  analyzeError?: string;
  analyses: AnalysisHistory[];
}
