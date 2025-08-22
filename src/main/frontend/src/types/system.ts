export interface SystemOverview {
  totalModels: number
  enabledModels: number
  totalOperations: number
  enabledOperations: number  // 添加这个字段
  configuredOperations: number
  lastUpdate: number
}

export interface ApiResponse<T = unknown> {
  success: boolean
  message: string
  data?: T
  timestamp?: number
  code?: number
}

export interface ModelConfigData {
  modelName: string
  baseUrl: string
  apiKey: string
  defaultMaxTokens?: number
  defaultTemperature?: number
  supportStream?: boolean
  supportJsonOutput?: boolean
  supportThinking?: boolean
  additionalHeaders?: Record<string, string>
  description?: string
  provider?: string
  enabled?: boolean
}

export interface ApiResponse<T = unknown> {
  success: boolean
  message: string
  data?: T
  modelName?: string
  validated?: boolean
  error?: string
}

export interface ModelsResponse {
  models: Record<string, ModelConfigData>
  groupedByProvider: Record<string, ModelConfigData[]>
  total: number
}

export interface TestConnectionResponse {
  success: boolean
  message: string
  modelName: string
}

export interface ModelListResponse {
  models: Record<string, ModelConfigData>
  groupedByProvider: Record<string, ModelConfigData[]>
  total: number
}

export interface OperationConfigData {
  operationType: string
  description?: string
  enabled?: boolean
  maxTokens?: number
  temperature?: number
  timeout?: number
  retryCount?: number
  jsonOutput?: boolean
  streamOutput?: boolean
  thinkingMode?: boolean
  promptPrefix?: string
  promptSuffix?: string
  systemPrompt?: string
  outputFormat?: string
  customParams?: Record<string, unknown>
  modelName?: string
}

export interface OperationsResponse {
  mappings: Record<string, string>
  configs: Record<string, OperationConfigData>
  totalOperations: number
  configuredOperations: number
}

export interface OperationDetailResponse {
  operation: OperationConfigData
  associatedModel?: ModelConfigData
}

// AI调用日志相关类型定义
// 更新 AICallLogSummary 接口
export interface AICallLogSummary {
  callId: string
  operationType: string
  modelName: string
  callTime: string  // 后端返回LocalDateTime转换为字符串
  duration: number
  status: string    // 后端返回CallStatus枚举转换为字符串
  errorMessage: string | null
  frequency: number
  lastAccessTime: string
  requestParams?: {
    maxTokens: number
    temperature: number
    jsonOutput: boolean
    thinking: boolean
  }
}

// 新增 AICallLog 接口
export interface AICallLog extends AICallLogSummary {
  input?: Record<string, unknown>
  prompt?: string
  rawResponse?: string
  output?: Record<string, unknown>
}

export interface LogStatistics {
  totalCalls: number
  successfulCalls: number
  failedCalls: number
  totalTokensUsed: number
  averageResponseTime: number
  callsByOperation: Record<string, number>
  callsByModel: Record<string, number>
}
