import {
  controlPlaneApi,
  type TenantOperationConfig,
  type PromptTemplatePreviewResponse
} from './controlPlaneApi'
import { getScopeContext } from './scopeContext'
import type { OperationConfigData, OperationsResponse, OperationDetailResponse, ApiResponse } from '@/types/system'

function requireScope() {
  const scope = getScopeContext()
  if (!scope) {
    throw new Error('请先在左侧选择租户和应用')
  }
  return scope
}

function mapToOperationConfig(item: TenantOperationConfig): OperationConfigData {
  const cfg = item.config || {}
  const catalog = (cfg._catalog as Record<string, unknown>) || {}
  const promptMode = cfg.promptMode === 'TEMPLATE_OVERRIDE' ? 'TEMPLATE_OVERRIDE' : 'LOCAL_ONLY'

  return {
    operationType: String(item.operationType),
    modelName: String(item.modelName || ''),
    enabled: Boolean(item.active),
    description:
      typeof cfg.description === 'string'
        ? cfg.description
        : (typeof catalog.description === 'string' ? catalog.description : ''),
    maxTokens:
      typeof cfg.maxTokens === 'number'
        ? cfg.maxTokens
        : (typeof catalog.defaultMaxTokens === 'number' ? catalog.defaultMaxTokens : 4096),
    temperature:
      typeof cfg.temperature === 'number'
        ? cfg.temperature
        : (typeof catalog.defaultTemperature === 'number' ? catalog.defaultTemperature : 0.7),
    jsonOutput:
      typeof cfg.jsonOutput === 'boolean'
        ? cfg.jsonOutput
        : Boolean(catalog.requireJsonOutput),
    thinkingMode:
      typeof cfg.thinkingMode === 'boolean'
        ? cfg.thinkingMode
        : Boolean(catalog.supportThinking),
    streamOutput: typeof cfg.streamOutput === 'boolean' ? cfg.streamOutput : undefined,
    timeout: typeof cfg.timeout === 'number' ? cfg.timeout : undefined,
    retryCount: typeof cfg.retryCount === 'number' ? cfg.retryCount : undefined,
    promptMode,
    promptTemplate: typeof cfg.promptTemplate === 'string' ? cfg.promptTemplate : undefined,
    localPromptTemplate: typeof catalog.localPromptTemplate === 'string' ? catalog.localPromptTemplate : undefined,
    promptStrictRender: typeof cfg.promptStrictRender === 'boolean' ? cfg.promptStrictRender : false,
    outputFormat: typeof cfg.outputFormat === 'string' ? cfg.outputFormat : undefined,
    customParams: typeof cfg.customParams === 'object' && cfg.customParams !== null
      ? (cfg.customParams as Record<string, unknown>)
      : {}
  }
}

function toConfigPayload(config: OperationConfigData): Record<string, unknown> {
  const payload: Record<string, unknown> = {
    description: config.description,
    maxTokens: config.maxTokens,
    temperature: config.temperature,
    jsonOutput: config.jsonOutput,
    thinkingMode: config.thinkingMode,
    streamOutput: config.streamOutput,
    timeout: config.timeout,
    retryCount: config.retryCount,
    promptMode: config.promptMode || 'LOCAL_ONLY',
    promptTemplate: config.promptMode === 'TEMPLATE_OVERRIDE' ? config.promptTemplate : undefined,
    promptStrictRender: config.promptStrictRender ?? false,
    outputFormat: config.outputFormat,
    customParams: config.customParams || {}
  }

  Object.keys(payload).forEach((key) => {
    if (payload[key] === undefined) {
      delete payload[key]
    }
  })

  return payload
}

export const aiOperationApi = {
  async getAllOperations(): Promise<OperationsResponse> {
    const scope = requireScope()
    const items = await controlPlaneApi.listOperationConfigs(scope.tenantId, scope.appId)

    const configs: Record<string, OperationConfigData> = {}
    const mappings: Record<string, string> = {}

    for (const item of items) {
      const mapped = mapToOperationConfig(item)
      configs[mapped.operationType] = mapped
      mappings[mapped.operationType] = mapped.modelName || ''
    }

    return {
      mappings,
      configs,
      totalOperations: Object.keys(configs).length,
      configuredOperations: Object.values(configs).filter((it) => !!it.modelName).length
    }
  },

  async getOperation(operationType: string): Promise<OperationDetailResponse> {
    const all = await this.getAllOperations()
    const operation = all.configs[operationType]
    if (!operation) {
      throw new Error(`操作节点不存在: ${operationType}`)
    }
    return { operation }
  },

  async saveOperationConfig(operationType: string, config: OperationConfigData): Promise<ApiResponse> {
    const scope = requireScope()
    await controlPlaneApi.upsertOperationConfig(scope.tenantId, scope.appId, {
      operationType,
      modelName: config.modelName || '',
      active: config.enabled ?? true,
      config: toConfigPayload({ ...config, operationType })
    })

    return {
      success: true,
      message: '操作配置保存成功'
    }
  },

  async setOperationMapping(operationType: string, modelName: string): Promise<ApiResponse> {
    const existing = await this.getOperation(operationType)
    return this.saveOperationConfig(operationType, {
      ...existing.operation,
      modelName
    })
  },

  async setOperationMappings(mappings: Record<string, string>): Promise<ApiResponse> {
    const entries = Object.entries(mappings)
    for (const [operationType, modelName] of entries) {
      await this.setOperationMapping(operationType, modelName)
    }

    return {
      success: true,
      message: '批量映射更新成功'
    }
  },

  async previewPromptTemplate(payload: {
    operationType?: string
    template: string
    strictRender: boolean
    input?: Record<string, unknown>
    localPrompt?: string
  }): Promise<PromptTemplatePreviewResponse> {
    return controlPlaneApi.previewPromptTemplate(payload)
  }
}
