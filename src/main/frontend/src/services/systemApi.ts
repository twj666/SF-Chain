import { API_CONFIG } from './apiConfig'
import { apiJsonRequest } from './apiUtils'
import type { SystemOverview, ApiResponse } from '@/types/system'

export const systemApi = {
  // 获取系统概览信息
  async getSystemOverview(): Promise<SystemOverview> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/system/overview`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 创建系统配置备份
  async createBackup(): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/system/backup`, {
      method: 'POST',
      requireAuth: true
    })
  },

  // 刷新系统配置
  async refreshSystem(): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/system/refresh`, {
      method: 'POST',
      requireAuth: true
    })
  },

  // 重置系统配置
  async resetSystem(): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/system/reset`, {
      method: 'POST',
      requireAuth: true
    })
  }
}
