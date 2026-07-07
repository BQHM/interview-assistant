// 面试会话状态。
// IN_PROGRESS 表示还在答题，COMPLETED 表示整场面试已经完成。
export type InterviewSessionStatus = 'IN_PROGRESS' | 'COMPLETED';

// 面试题，对应后端生成的一道题目快照。
export interface InterviewQuestion {
  // 题目序号，从 0 开始，用来和 currentQuestionIndex 对齐。
  questionIndex: number;
  // 题目正文。
  question: string;
  // 题目类型，例如“项目经验”“技术深度”。
  type: string;
  // 题目分类，例如“数据库与 ORM”“系统设计”。
  category: string;
  // 用户答案，未回答时可能为空。
  userAnswer?: string;
  // 单题评分，评估完成后才有。
  score?: number;
  // 单题反馈，评估完成后才有。
  feedback?: string;
}

// 面试会话详情，答题页主要依赖这个类型。
export interface InterviewSession {
  // 会话编号，是字符串 UUID，用来标识某一次面试。
  sessionId: string;
  // 这场面试基于哪份简历创建。
  resumeId: number;
  // 总题目数。
  totalQuestions: number;
  // 当前题目下标，从 0 开始；如果等于 totalQuestions，说明已经答完。
  currentQuestionIndex: number;
  // 当前会话状态。
  status: InterviewSessionStatus;
  // 创建时间。
  createdAt: string;
  // 题目快照列表。
  questions: InterviewQuestion[];
}

// 面试列表项，只放历史列表需要展示的轻量字段。
export interface InterviewSessionListItem {
  sessionId: string;
  resumeId: number;
  totalQuestions: number;
  currentQuestionIndex: number;
  status: InterviewSessionStatus;
  createdAt: string;
}

// 创建面试请求，对应 POST /api/interviews 的请求体。
export interface CreateInterviewRequest {
  // 用哪份简历创建面试。
  resumeId: number;
  // 希望生成几道题；问号表示不传时由后端使用默认值。
  questionCount?: number;
}

// 提交答案请求，对应提交当前题答案。
export interface SubmitAnswerRequest {
  // 当前面试会话编号。
  sessionId: string;
  // 正在提交第几题。
  questionIndex: number;
  // 用户输入的答案文本。
  answer: string;
}

// 提交答案响应，后端会告诉前端是否还有下一题。
export interface SubmitAnswerResponse {
  // true 表示还有下一题，false 表示已经答完。
  hasNextQuestion: boolean;
  // 下一题信息，只有 hasNextQuestion 为 true 时才可能有。
  nextQuestion?: InterviewQuestion;
  // 提交后最新的当前题下标。
  currentQuestionIndex: number;
  // 总题目数。
  totalQuestions: number;
}

// 当前题目响应，如果后续改成“只拉当前题”，会用到这个类型。
export interface CurrentQuestionResponse {
  question?: InterviewQuestion;
  currentQuestionIndex: number;
  totalQuestions: number;
  isCompleted: boolean;
}

// 面试单题报告，对应后端 InterviewReportQuestionDTO。
export interface InterviewReportQuestion {
  // 题目序号，从 0 开始。
  questionIndex: number;
  // 题目正文。
  question: string;
  // 题目分类。
  category: string;
  // 用户答案；未作答时可能为空。
  userAnswer?: string;
  // 是否已经作答。
  answered: boolean;
  // 单题评价。
  evaluation: string;
  // 单题得分。
  score: number;
  // 参考答案；AI 评估失败或未作答时可能为空。
  referenceAnswer?: string;
  // 回答该题时应该覆盖的关键点。
  keyPoints: string[];
}

// 面试报告，对应后端 InterviewReportDTO。
export interface InterviewReport {
  // 面试会话编号。
  sessionId: string;
  // 关联的简历编号。
  resumeId: number;
  // 题目总数。
  totalQuestions: number;
  // 已回答题数。
  answeredQuestions: number;
  // 未回答题数。
  unansweredQuestions: number;
  // 面试是否已完成。
  completed: boolean;
  // 整体评价。
  overallEvaluation: string;
  // 每一道题的报告明细。
  questionReports: InterviewReportQuestion[];
  // 报告生成时间。
  generatedAt: string;
}
