import axios from 'axios';

// 统一的 axios 实例。
// baseURL 为空时，请求会先发到当前前端服务，再由 vite.config.ts 代理到后端。
const instance = axios.create({
  baseURL: '',
  timeout: 60000,
});

// 后端统一返回 Result<T>，真实业务数据在 response.data.data 里。
// 页面层只拿到 T，可以少写很多重复的拆包代码。
export const request = {
  // GET 通常用于查询，例如查询简历列表、查询面试记录。
  get<T>(url: string): Promise<T> {
    // <T> 是 TypeScript 泛型，表示调用方可以指定返回数据的类型。
    return instance.get(url).then(response => response.data.data);
  },

  // POST 通常用于创建或提交，例如上传答案、创建面试。
  post<T>(url: string, data?: unknown): Promise<T> {
    return instance.post(url, data).then(response => response.data.data);
  },

  // 文件上传单独封装，因为它需要 multipart/form-data。
  upload<T>(url: string, formData: FormData): Promise<T> {
    return instance.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }).then(response => response.data.data);
  },
};

// 把未知异常转换成页面可以展示的错误文案。
export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return '未知错误';
}
