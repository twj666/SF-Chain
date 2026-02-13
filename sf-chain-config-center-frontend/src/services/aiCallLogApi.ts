import { API_CONFIG } from './apiConfig'
import { apiJsonRequest } from './apiUtils'
import type { AICallLogSummary, AICallLog, LogStatistics, ApiResponse } from '@/types/system'

export interface AICallLogScope {
  tenantId: string
  appId: string
}

function buildScopeQuery(scope?: AICallLogScope): string {
  if (!scope?.tenantId || !scope?.appId) {
    return ''
  }
  const params = new URLSearchParams()
  params.set('tenantId', scope.tenantId)
  params.set('appId', scope.appId)
  return `?${params.toString()}`
}

export const aiCallLogApi = {
  // 获取所有日志摘要（轻量级）
  async getAllLogSummaries(scope?: AICallLogScope): Promise<AICallLogSummary[]> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}${buildScopeQuery(scope)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 根据调用ID获取完整日志详情
  async getFullLog(callId: string, scope?: AICallLogScope): Promise<AICallLog> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/${encodeURIComponent(callId)}${buildScopeQuery(scope)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 根据操作类型获取日志摘要
  async getLogSummariesByOperation(operationType: string, scope?: AICallLogScope): Promise<AICallLogSummary[]> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/operation/${encodeURIComponent(operationType)}${buildScopeQuery(scope)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 根据模型名称获取日志摘要
  async getLogSummariesByModel(modelName: string, scope?: AICallLogScope): Promise<AICallLogSummary[]> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/model/${encodeURIComponent(modelName)}${buildScopeQuery(scope)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 获取统计信息
  async getStatistics(scope?: AICallLogScope): Promise<LogStatistics> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}/statistics${buildScopeQuery(scope)}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 清空所有日志
  async clearLogs(_scope?: AICallLogScope): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_CALL_LOGS}`, {
      method: 'DELETE',
      requireAuth: true
    })
  }
}
