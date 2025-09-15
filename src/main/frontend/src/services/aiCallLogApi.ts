import { API_CONFIG } from './apiConfig'
import { apiJsonRequest } from './apiUtils'
import type { AICallLogSummary, AICallLog, LogStatistics, ApiResponse } from '@/types/system'

export const aiCallLogApi = {
  // 获取所有日志摘要（轻量级）
  async getAllLogSummaries(): Promise<AICallLogSummary[]> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 根据调用ID获取完整日志详情
  async getFullLog(callId: string): Promise<AICallLog> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/${encodeURIComponent(callId)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 根据操作类型获取日志摘要
  async getLogSummariesByOperation(operationType: string): Promise<AICallLogSummary[]> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/operation/${encodeURIComponent(operationType)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 根据模型名称获取日志摘要
  async getLogSummariesByModel(modelName: string): Promise<AICallLogSummary[]> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/model/${encodeURIComponent(modelName)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 获取统计信息
  async getStatistics(): Promise<LogStatistics> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/statistics`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 清空所有日志
  async clearLogs(): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}`, {
      method: 'DELETE',
      requireAuth: true
    })
  }
}
