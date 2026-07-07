import { request } from './request';
import type { ResumeUploadResponse, ResumeListItem, ResumeDetail, ResumeAnalysis } from '../types/resume';

// 简历模块 API 封装。
// 页面不要直接写 axios，统一通过这里调用后端，方便以后改接口路径和错误处理。
export const resumeApi = {
  /**
   * 上传简历
   */
  async uploadResume(file: File): Promise<ResumeUploadResponse> {
    const formData = new FormData();
    // 后端 MultipartFile 参数名是 file，所以这里也必须叫 file。
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
