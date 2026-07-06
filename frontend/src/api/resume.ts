import { request } from './request';
import type { ResumeUploadResponse, ResumeListItem, ResumeDetail, ResumeAnalysis } from '../types/resume';

export const resumeApi = {
  /**
   * 上传简历
   */
  async uploadResume(file: File): Promise<ResumeUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return request.upload<ResumeUploadResponse>('/api/resumes/upload', formData);
  },

  /**
   * 获取简历列表
   */
  async getResumeList(): Promise<ResumeListItem[]> {
    return request.get<ResumeListItem[]>('/api/resumes');
  },

  /**
   * 获取简历详情
   */
  async getResumeDetail(id: number): Promise<ResumeDetail> {
    return request.get<ResumeDetail>(`/api/resumes/${id}`);
  },

  /**
   * 获取简历分析结果
   */
  async getResumeAnalysis(id: number): Promise<ResumeAnalysis> {
    return request.get<ResumeAnalysis>(`/api/resumes/${id}/analysis`);
  },
};
