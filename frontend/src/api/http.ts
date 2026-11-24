import axios, { type AxiosRequestConfig } from 'axios'

/** 与后端 ApiResponse 保持一致 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const TOKEN_KEY = 'youhu_token'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown> | undefined
    if (body && typeof body.code === 'number' && body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const message: string =
      error?.response?.data?.message ?? error?.message ?? '网络异常，请稍后重试'
    return Promise.reject(new Error(message))
  },
)

/** 发起请求并直接返回 data，便于组件内书写 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}

export default http
