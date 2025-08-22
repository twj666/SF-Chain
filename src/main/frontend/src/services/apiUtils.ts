import { toast } from '@/utils/toast'
import { getApiConfig } from './apiConfig';

// API请求工具函数
export interface RequestOptions {
  method?: string
  headers?: Record<string, string>
  body?: string
  signal?: AbortSignal
  requireAuth?: boolean
  isAIRequest?: boolean
}

// 获取存储的token
export function getAuthToken(): string | null {
  return localStorage.getItem('token')
}

// 设置token
export function setAuthToken(token: string): void {
  localStorage.setItem('token', token)
}

// 清除认证信息
function clearAuthData() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

// 处理认证失败
function handleAuthFailure() {
  clearAuthData()
  toast.error({
    title: '认证失败',
    message: '请检查您的token是否正确',
    duration: 4000
  })
}

// 处理需要登录
function handleRequireLogin() {
  toast.error({
    title: '需要认证',
    message: '请在页面顶部输入有效的token',
    duration: 4000
  })
}

// 统一的请求函数
export async function apiRequest(url: string, options: RequestOptions = {}): Promise<Response> {
  const {
    method = 'GET',
    headers = {},
    body,
    signal,
    requireAuth = true,
    isAIRequest = false
  } = options

  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers
  }

  // 判断是否是AI请求
  const isAI = isAIRequest || url.includes('/sf-chain/')

  if (isAI || requireAuth) {
    // AI接口和普通接口都统一使用Authorization token
    const token = getAuthToken()
    if (token) {
      requestHeaders['Authorization'] = `${token}`
    } else {
      handleRequireLogin()
      throw new Error('未提供认证token')
    }

    if (isAI) {
      console.log('🔍 AI请求调试信息:')
      console.log('URL:', url)
      console.log('Authorization Token:', token)
      console.log('请求头:', requestHeaders)
    }
  }

  try {
    console.log('📤 发送请求:', { url, method, headers: requestHeaders })
    const response = await fetch(url, {
      method,
      headers: requestHeaders,
      body,
      signal
    })

    if (response.status === 401) {
      if (isAI) {
        toast.error({
          title: 'AI接口认证失败',
          message: '缺少Authorization请求头',
          duration: 4000
        })
      } else {
        handleAuthFailure()
      }
      throw new Error('认证失败')
    }

    if (response.status === 403) {
      if (isAI) {
        toast.error({
          title: 'AI接口权限不足',
          message: 'Authorization验证失败',
          duration: 4000
        })
      } else {
        toast.error({
          title: '权限不足',
          message: '您没有权限执行此操作',
          duration: 4000
        })
      }
      throw new Error('权限不足')
    }

    if (response.status === 404) {
      toast.error({
        title: '资源不存在',
        message: '请求的资源不存在或已被删除',
        duration: 3000
      })
      throw new Error('资源不存在')
    }

    if (response.status >= 500) {
      toast.error({
        title: '服务器错误',
        message: '服务器暂时无法处理您的请求，请稍后重试',
        duration: 5000,
        closable: true
      })
      throw new Error('服务器错误')
    }

    return response
  } catch (error: unknown) {
    const err = error as Error
    if (err.name === 'TypeError' && err.message.includes('fetch')) {
      toast.error({
        title: '网络连接失败',
        message: '无法连接到服务器，请检查网络连接',
        duration: 5000,
        closable: true
      })
    } else if (err.name === 'AbortError') {
      console.log('请求已取消')
    } else if (!err.message.includes('认证失败') &&
               !err.message.includes('权限不足') &&
               !err.message.includes('资源不存在') &&
               !err.message.includes('未提供认证token')) {
      toast.error({
        title: '请求失败',
        message: err.message || '请求处理失败，请稍后重试',
        duration: 4000
      })
    }
    throw error
  }
}

// 便捷的JSON请求函数
export async function apiJsonRequest<T = unknown>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const response = await apiRequest(url, options)

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  try {
    return await response.json()
  } catch {
    toast.error({
      title: '数据解析失败',
      message: '服务器返回的数据格式有误',
      duration: 3000
    })
    throw new Error('数据解析失败')
  }
}

// AI接口专用请求函数
export async function aiApiRequest<T = unknown>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  return apiJsonRequest<T>(url, { ...options, isAIRequest: true })
}

export class ApiClient {
  private static instance: ApiClient;
  private baseUrl: string = '';

  private constructor() {}

  public static getInstance(): ApiClient {
    if (!ApiClient.instance) {
      ApiClient.instance = new ApiClient();
    }
    return ApiClient.instance;
  }

  public async initialize(): Promise<void> {
    const config = await getApiConfig();
    this.baseUrl = config.baseUrl;
  }

  public async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    if (!this.baseUrl) {
      await this.initialize();
    }

    const url = `${this.baseUrl}${endpoint}`;
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  public async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  public async post<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  public async put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  public async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const apiClient = ApiClient.getInstance();
