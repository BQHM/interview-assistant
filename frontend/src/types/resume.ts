// 异步任务状态。
// 这里用联合类型限制取值，避免页面里随手写出不存在的状态字符串。
export type AsyncTaskStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

// 简历上传响应，对应后端上传接口返回的数据。
// 上传成功后，页面会用 resumeId 跳转到简历详情页。
export interface ResumeUploadResponse {
  // 简历主键编号。
  resumeId: number;
  // 文件名，用于页面展示。
  filename: string;
  // 对象存储里的文件 key，前端一般只展示或传递，不直接读取文件。
  storageKey: string;
  // 简历分析状态，告诉页面分析是否完成。
  analyzeStatus: AsyncTaskStatus;
  // 是否命中重复文件，true 表示后端复用了已有简历。
  duplicate: boolean;
}

// 简历列表项，只放列表页需要展示的轻量字段。
// 不包含 resumeText 这类大字段，避免列表接口太重。
export interface ResumeListItem {
  // 简历编号，用来跳转 /resumes/{id}。
  id: number;
  // 列表页展示的文件名。
  filename: string;
  // 文件大小，单位是字节。
  fileSize: number;
  // 上传时间，后端通常用 ISO 字符串返回。
  uploadedAt: string;
  // 分析状态，用来显示“分析中 / 已完成 / 失败”等标签。
  analyzeStatus: AsyncTaskStatus;
  // 分析失败时的错误信息；问号表示这个字段可能不存在。
  analyzeError?: string;
}

// 简历评分详情，表示 AI 或规则分析拆出来的多个维度分。
export interface ScoreDetail {
  contentScore: number;      // 内容完整性
  structureScore: number;    // 结构清晰度
  skillMatchScore: number;   // 技能匹配度
  expressionScore: number;   // 表达专业性
  projectScore: number;      // 项目经验
}

// 改进建议，一条 suggestion 对应一个可改进点。
export interface Suggestion {
  // 建议分类，例如“项目经历”“技能描述”。
  category: string;
  // 优先级，例如 HIGH / MEDIUM / LOW。
  priority: string;
  // 当前存在的问题。
  issue: string;
  // 推荐修改方式。
  recommendation: string;
}

// 简历分析结果，对应“查看分析”类接口。
export interface ResumeAnalysis {
  // 被分析的简历编号。
  resumeId: number;
  // 总分。
  overallScore: number;
  // 多维度评分。
  scoreDetail: ScoreDetail;
  // 总结说明。
  summary: string;
  // 简历亮点列表。
  strengths: string[];
  // 改进建议列表。
  suggestions: Suggestion[];
  // 分析完成时间。
  analyzedAt: string;
}

// 分析历史项，放在简历详情里，表示这份简历曾经产生过的分析记录。
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

// 简历详情，比列表项更完整，包含简历正文和分析历史。
export interface ResumeDetail {
  // 简历编号。
  id: number;
  // 原始文件名。
  originalFilename: string;
  // 文件大小，单位是字节。
  fileSize: number;
  // 文件 MIME 类型，例如 application/pdf。
  contentType: string;
  // 对象存储 key。
  storageKey: string;
  // 后端解析出的简历纯文本。
  resumeText: string;
  // 上传时间。
  uploadedAt: string;
  // 当前分析状态。
  analyzeStatus: AsyncTaskStatus;
  // 分析失败原因，只有失败时才可能有。
  analyzeError?: string;
  // 分析历史列表。
  analyses: AnalysisHistory[];
}
