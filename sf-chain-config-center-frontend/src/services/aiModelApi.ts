import {
  controlPlaneApi,
  type TenantModelConfig,
  type ModelConfigExportResponse,
  type ModelConfigImportResponse,
  type ModelImportMode
} from './controlPlaneApi'
import { getScopeContext } from './scopeContext'
import type { ModelConfigData, ApiResponse, ModelsResponse, TestConnectionResponse } from '@/types/system'

function requireScope() {
  const scope = getScopeContext()
  if (!scope) {
    throw new Error('请先在左侧选择租户和应用')
  }
  return scope
}

function mapToModelConfig(item: TenantModelConfig): ModelConfigData {
  const cfg = item.config || {}
  const rawItem = item as unknown as { description?: unknown }
  return {
    modelName: String(item.modelName),
    provider: String(item.provider || 'other'),
    baseUrl: String(item.baseUrl || ''),
    apiKey: String(cfg.apiKey || ''),
    description: typeof cfg.description === 'string'
      ? cfg.description
      : (typeof rawItem.description === 'string' ? rawItem.description : undefined),
    enabled: Boolean(item.active)
  }
}

function toModelsResponse(items: TenantModelConfig[]): ModelsResponse {
  const models: Record<string, ModelConfigData> = {}
  const groupedByProvider: Record<string, ModelConfigData[]> = {}

  for (const item of items) {
    const model = mapToModelConfig(item)
    models[model.modelName] = model

    const provider = model.provider || 'other'
    if (!groupedByProvider[provider]) {
      groupedByProvider[provider] = []
    }
    groupedByProvider[provider].push(model)
  }

  return {
    models,
    groupedByProvider,
    total: Object.keys(models).length
  }
}

export const aiModelApi = {
  async getAllModels(): Promise<ModelsResponse> {
    const scope = requireScope()
    const items = await controlPlaneApi.listModelConfigs(scope.tenantId, scope.appId)
    return toModelsResponse(items)
  },

  async getModel(modelName: string): Promise<ModelConfigData> {
    const all = await this.getAllModels()
    const model = all.models[modelName]
    if (!model) {
      throw new Error(`模型不存在: ${modelName}`)
    }
    return model
  },

  async saveModel(modelName: string, config: ModelConfigData): Promise<ApiResponse> {
    const scope = requireScope()
    await controlPlaneApi.upsertModelConfig(scope.tenantId, scope.appId, {
      modelName,
      provider: config.provider || 'other',
      baseUrl: config.baseUrl,
      active: config.enabled ?? true,
      config: {
        apiKey: config.apiKey,
        description: config.description
      }
    })

    return {
      success: true,
      message: '模型配置保存成功'
    }
  },

  async deleteModel(modelName: string): Promise<ApiResponse> {
    const existing = await this.getModel(modelName)
    return this.saveModel(modelName, {
      ...existing,
      enabled: false
    })
  },

  async testModel(modelName: string): Promise<TestConnectionResponse> {
    const scope = requireScope()
    const result = await controlPlaneApi.testModelConfig(scope.tenantId, scope.appId, modelName)
    return {
      success: !!result.success,
      message: result.message || (result.success ? '模型连接测试成功' : '模型连接测试失败'),
      modelName: result.modelName || modelName
    }
  },

  async exportModels(includeSecrets = false): Promise<ModelConfigExportResponse> {
    const scope = requireScope()
    return controlPlaneApi.exportModelConfigs(scope.tenantId, scope.appId, includeSecrets)
  },

  async importModels(payload: {
    mode: ModelImportMode
    dryRun: boolean
    models: Array<{
      modelName: string
      provider?: string
      baseUrl?: string
      active?: boolean
      config?: Record<string, unknown>
    }>
  }): Promise<ModelConfigImportResponse> {
    const scope = requireScope()
    return controlPlaneApi.importModelConfigs(scope.tenantId, scope.appId, payload)
  }
}
