// 面试会话状态
export type InterviewSessionStatus = 'IN_PROGRESS' | 'COMPLETED';

// 面试题
export interface InterviewQuestion {
  questionIndex: number;
  question: string;
  type: string;
  category: string;
  userAnswer?: string;
  score?: number;
  feedback?: string;
}

// 面试会话
export interface InterviewSession {
  sessionId: string;
  resumeId: number;
  totalQuestions: number;
  currentQuestionIndex: number;
  status: InterviewSessionStatus;
  createdAt: string;
  questions: InterviewQuestion[];
}

// 面试列表项
export interface InterviewSessionListItem {
  sessionId: string;
  resumeId: number;
  totalQuestions: number;
  currentQuestionIndex: number;
  status: InterviewSessionStatus;
  createdAt: string;
}

// 创建面试请求
export interface CreateInterviewRequest {
  resumeId: number;
  questionCount?: number;
}

// 提交答案请求
export interface SubmitAnswerRequest {
  sessionId: string;
  questionIndex: number;
  answer: string;
}

// 提交答案响应
export interface SubmitAnswerResponse {
  hasNextQuestion: boolean;
  nextQuestion?: InterviewQuestion;
  currentQuestionIndex: number;
  totalQuestions: number;
}

// 当前题目响应
export interface CurrentQuestionResponse {
  question?: InterviewQuestion;
  currentQuestionIndex: number;
  totalQuestions: number;
  isCompleted: boolean;
}

// 面试报告评分详情
export interface ReportScoreDetail {
  overallScore: number;
  technicalScore: number;
  communicationScore: number;
  logicScore: number;
}

// 面试报告
export interface InterviewReport {
  sessionId: string;
  scoreDetail: ReportScoreDetail;
  summary: string;
  strengths: string[];
  improvements: string[];
}
