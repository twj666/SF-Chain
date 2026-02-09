import { controlPlaneApi, type TenantModelConfig } from './controlPlaneApi'
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
  return {
    modelName: String(item.modelName),
    provider: String(item.provider || 'other'),
    baseUrl: String(item.baseUrl || ''),
    apiKey: String(cfg.apiKey || ''),
    defaultMaxTokens: typeof cfg.defaultMaxTokens === 'number' ? cfg.defaultMaxTokens : undefined,
    defaultTemperature: typeof cfg.defaultTemperature === 'number' ? cfg.defaultTemperature : undefined,
    supportStream: typeof cfg.supportStream === 'boolean' ? cfg.supportStream : true,
    supportJsonOutput: typeof cfg.supportJsonOutput === 'boolean' ? cfg.supportJsonOutput : true,
    supportThinking: typeof cfg.supportThinking === 'boolean' ? cfg.supportThinking : false,
    additionalHeaders: typeof cfg.additionalHeaders === 'object' && cfg.additionalHeaders !== null
      ? (cfg.additionalHeaders as Record<string, string>)
      : undefined,
    description: typeof cfg.description === 'string' ? cfg.description : undefined,
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
        defaultMaxTokens: config.defaultMaxTokens,
        defaultTemperature: config.defaultTemperature,
        supportStream: config.supportStream,
        supportJsonOutput: config.supportJsonOutput,
        supportThinking: config.supportThinking,
        additionalHeaders: config.additionalHeaders,
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
    await this.getModel(modelName)
    return {
      success: true,
      message: '控制面暂不提供在线连通性测试，配置已校验并存在',
      modelName
    }
  }
}
