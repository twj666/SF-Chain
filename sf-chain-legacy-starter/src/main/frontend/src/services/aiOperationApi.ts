import { API_CONFIG } from './apiConfig'
import { apiJsonRequest } from './apiUtils'
import type { OperationConfigData, OperationsResponse, OperationDetailResponse, ApiResponse } from '@/types/system'

export const aiOperationApi = {
  // 获取所有操作配置
  async getAllOperations(): Promise<OperationsResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_OPERATIONS}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 获取单个操作配置 - 改为POST请求
  async getOperation(operationType: string): Promise<OperationDetailResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_OPERATIONS}/get`, {
      method: 'POST',
      body: JSON.stringify({ operationType }),
      requireAuth: true
    })
  },

  // 保存操作配置 - 修正数据结构
  async saveOperationConfig(operationType: string, config: OperationConfigData): Promise<ApiResponse> {
    // 确保operationType设置在config对象中
    const configWithType = {
      ...config,
      operationType: operationType
    }

    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_OPERATIONS}/save`, {
      method: 'POST',
      body: JSON.stringify(configWithType),
      requireAuth: true
    })
  },

  // 设置操作模型映射
  async setOperationMapping(operationType: string, modelName: string): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_OPERATIONS}/mapping`, {
      method: 'POST',
      body: JSON.stringify({ operationType, modelName }),
      requireAuth: true
    })
  },

  // 批量设置操作模型映射
  async setOperationMappings(mappings: Record<string, string>): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_OPERATIONS}/mappings`, {
      method: 'POST',
      body: JSON.stringify(mappings),
      requireAuth: true
    })
  }
}
