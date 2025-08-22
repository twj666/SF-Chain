import { API_CONFIG } from './apiConfig'
import { apiJsonRequest } from './apiUtils'
import type { ModelConfigData, ApiResponse, ModelsResponse, TestConnectionResponse } from '@/types/system'

export const aiModelApi = {
  // 获取所有模型配置
  async getAllModels(): Promise<ModelsResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/models/list`, {
      method: 'GET',
      requireAuth: true
    })
  },

  // 获取单个模型配置 - 改为POST请求
  async getModel(modelName: string): Promise<ModelConfigData> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/models/get`, {
      method: 'POST',
      body: JSON.stringify({ modelName }),
      requireAuth: true
    })
  },

  // 保存模型配置（创建或更新）
  async saveModel(modelName: string, config: ModelConfigData): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/models/save`, {
      method: 'POST',
      body: JSON.stringify({ ...config, modelName }),
      requireAuth: true
    })
  },

  // 删除模型配置 - 改为POST请求
  async deleteModel(modelName: string): Promise<ApiResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/models/delete`, {
      method: 'POST',
      body: JSON.stringify({ modelName }),
      requireAuth: true
    })
  },

  // 测试模型连接 - 改为POST请求
  async testModel(modelName: string): Promise<TestConnectionResponse> {
    return apiJsonRequest(`${API_CONFIG.BASE_URL}/sf-chain/models/test`, {
      method: 'POST',
      body: JSON.stringify({ modelName }),
      requireAuth: true
    })
  }
}
