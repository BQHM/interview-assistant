import axios from 'axios';

const instance = axios.create({
  baseURL: '',
  timeout: 60000,
});

export const request = {
  get<T>(url: string): Promise<T> {
    return instance.get(url).then(response => response.data.data);
  },

  post<T>(url: string, data?: unknown): Promise<T> {
    return instance.post(url, data).then(response => response.data.data);
  },

  upload<T>(url: string, formData: FormData): Promise<T> {
    return instance.post(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }).then(response => response.data.data);
  },
};

export function getErrorMessage(error: unknown): string {
  if (error instanceof Error) {
    return error.message;
  }
  return '未知错误';
}