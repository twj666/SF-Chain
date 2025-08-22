// AI提供商相关工具函数
export interface AIProvider {
  key: string
  name: string
  icon: string
  // 新增：用于表单显示的配置
  displayOrder: number
  category?: string
  description?: string
  // 新增：默认配置建议
  defaultConfig?: {
    baseUrl?: string
    maxTokens?: number
    temperature?: number
    supportStream?: boolean
    supportJsonOutput?: boolean
    supportThinking?: boolean
  }
}

// AI提供商配置 - 增强版
const AI_PROVIDERS: Record<string, AIProvider> = {
  openai: {
    key: 'openai',
    name: 'OpenAI',
    icon: '/icons/openai.svg',
    displayOrder: 1,
    category: 'commercial',
    description: 'OpenAI GPT系列模型',
    defaultConfig: {
      baseUrl: 'https://api.openai.com/v1',
      maxTokens: 4096,
      temperature: 0.7,
      supportStream: true,
      supportJsonOutput: true,
      supportThinking: false
    }
  },
  anthropic: {
    key: 'anthropic',
    name: 'Anthropic',
    icon: '/icons/anthropic.svg',
    displayOrder: 2,
    category: 'commercial',
    description: 'Anthropic Claude系列模型',
    defaultConfig: {
      baseUrl: 'https://api.anthropic.com/v1',
      maxTokens: 4096,
      temperature: 0.7,
      supportStream: true,
      supportJsonOutput: true,
      supportThinking: true
    }
  },
  google: {
    key: 'google',
    name: 'Google',
    icon: '/icons/google.svg',
    displayOrder: 3,
    category: 'commercial',
    description: 'Google Gemini系列模型',
    defaultConfig: {
      baseUrl: 'https://generativelanguage.googleapis.com/v1',
      maxTokens: 4096,
      temperature: 0.7,
      supportStream: true,
      supportJsonOutput: true,
      supportThinking: false
    }
  },
  deepseek: {
    key: 'deepseek',
    name: 'DeepSeek',
    icon: '/icons/deepseek.svg',
    displayOrder: 4,
    category: 'domestic',
    description: 'DeepSeek系列模型',
    defaultConfig: {
      baseUrl: 'https://api.deepseek.com/v1',
      maxTokens: 4096,
      temperature: 0.7,
      supportStream: true,
      supportJsonOutput: true,
      supportThinking: true
    }
  },
  doubao: {
    key: 'doubao',
    name: '豆包',
    icon: '/icons/doubao.svg',
    displayOrder: 5,
    category: 'domestic',
    description: '字节跳动豆包系列模型',
    defaultConfig: {
      baseUrl: 'https://ark.cn-beijing.volces.com/api/v3',
      maxTokens: 4096,
      temperature: 0.7,
      supportStream: true,
      supportJsonOutput: true,
      supportThinking: false
    }
  },
  qianwen: {
    key: 'qianwen',
    name: '千问',
    icon: '/icons/qianwen.svg',
    displayOrder: 6,
    category: 'domestic',
    description: '阿里云千问系列模型',
    defaultConfig: {
      baseUrl: 'https://dashscope.aliyuncs.com/api/v1',
      maxTokens: 4096,
      temperature: 0.7,
      supportStream: true,
      supportJsonOutput: true,
      supportThinking: false
    }
  },
  other: {
    key: 'other',
    name: '其他',
    icon: '/icons/default.svg',
    displayOrder: 999,
    category: 'custom',
    description: '自定义或其他提供商',
    defaultConfig: {
      baseUrl: '',
      maxTokens: 4096,
      temperature: 0.7,
      supportStream: true,
      supportJsonOutput: false,
      supportThinking: false
    }
  }
}

/**
 * 获取提供商名称
 */
export const getProviderName = (provider: string): string => {
  return AI_PROVIDERS[provider]?.name || AI_PROVIDERS.other.name
}

/**
 * 获取提供商图标路径
 */
export const getProviderIcon = (provider: string): string => {
  return AI_PROVIDERS[provider]?.icon || AI_PROVIDERS.other.icon
}

/**
 * 根据模型名称推断提供商
 */
export const getProviderFromModel = (modelName: string): string => {
  const model = modelName.toLowerCase()
  if (model.includes('gpt') || model.includes('openai')) return 'openai'
  if (model.includes('claude') || model.includes('anthropic')) return 'anthropic'
  if (model.includes('gemini') || model.includes('google')) return 'google'
  if (model.includes('deepseek')) return 'deepseek'
  if (model.includes('doubao') || model.includes('豆包')) return 'doubao'
  if (model.includes('qwen') || model.includes('千问') || model.includes('qianwen')) return 'qianwen'
  return 'other'
}

/**
 * 获取所有提供商列表（按显示顺序排序）
 */
export const getAllProviders = (): AIProvider[] => {
  return Object.values(AI_PROVIDERS).sort((a, b) => a.displayOrder - b.displayOrder)
}

/**
 * 按分类获取提供商
 */
export const getProvidersByCategory = (): Record<string, AIProvider[]> => {
  const categories: Record<string, AIProvider[]> = {}
  const providers = getAllProviders()
  
  providers.forEach(provider => {
    const category = provider.category || 'other'
    if (!categories[category]) {
      categories[category] = []
    }
    categories[category].push(provider)
  })
  
  return categories
}

/**
 * 获取提供商的默认配置
 */
export const getProviderDefaultConfig = (provider: string) => {
  return AI_PROVIDERS[provider]?.defaultConfig || AI_PROVIDERS.other.defaultConfig
}

/**
 * 获取分类显示名称
 */
export const getCategoryName = (category: string): string => {
  const categoryNames: Record<string, string> = {
    commercial: '商业模型',
    domestic: '国产模型',
    custom: '自定义',
    other: '其他'
  }
  return categoryNames[category] || category
}

/**
 * 提供商顺序（用于排序显示）
 */
export const getProviderOrder = (): string[] => {
  return getAllProviders().map(provider => provider.key)
}