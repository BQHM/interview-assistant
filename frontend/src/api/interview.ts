import { request } from './request';
import type {
  InterviewSession,
  InterviewSessionListItem,
  CreateInterviewRequest,
  SubmitAnswerRequest,
  SubmitAnswerResponse,
  CurrentQuestionResponse,
  InterviewReport,
} from '../types/interview';

export const interviewApi = {
  /**
   * 创建面试会话
   */
  async createInterview(data: CreateInterviewRequest): Promise<InterviewSession> {
    return request.post<InterviewSession>('/api/interviews', data);
  },

  /**
   * 获取面试会话详情
   */
  async getInterviewSession(sessionId: string): Promise<InterviewSession> {
    return request.get<InterviewSession>(`/api/interviews/${sessionId}`);
  },

  /**
   * 获取当前面试题
   */
  async getCurrentQuestion(sessionId: string): Promise<CurrentQuestionResponse> {
    return request.get<CurrentQuestionResponse>(`/api/interviews/${sessionId}/question`);
  },

  /**
   * 提交面试答案
   */
  async submitAnswer(data: SubmitAnswerRequest): Promise<SubmitAnswerResponse> {
    return request.post<SubmitAnswerResponse>('/api/interviews/answer', data);
  },

  /**
   * 完成面试
   */
  async completeInterview(sessionId: string): Promise<void> {
    return request.post<void>(`/api/interviews/${sessionId}/complete`);
  },

  /**
   * 获取面试历史列表
   */
  async getInterviewHistory(): Promise<InterviewSessionListItem[]> {
    return request.get<InterviewSessionListItem[]>('/api/interviews');
  },

  /**
   * 获取面试报告
   */
  async getInterviewReport(sessionId: string): Promise<InterviewReport> {
    return request.get<InterviewReport>(`/api/interviews/${sessionId}/report`);
  },
};
