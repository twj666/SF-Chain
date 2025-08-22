// API配置
interface ApiConfig {
  baseUrl: string;
  endpoints: {
    AI_MODELS: string;
    AI_OPERATIONS: string;
    AI_CALL_LOGS: string;
    AI_SYSTEM: string;
  };
}

let apiConfig: ApiConfig | null = null;

// 动态获取API配置
export async function getApiConfig(): Promise<ApiConfig> {
  if (apiConfig) {
    return apiConfig;
  }

  try {
    // 在生产环境中，从当前域名获取配置
    const configUrl = import.meta.env.DEV 
      ? 'http://localhost:5001/api/sf-chain/config/api-info'
      : '/sf-chain/config/api-info';
    
    const response = await fetch(configUrl);
    const config = await response.json();
    
    apiConfig = {
      baseUrl: config.baseUrl || '',
      endpoints: config.endpoints || {
        AI_MODELS: '/sf-chain/models',
        AI_OPERATIONS: '/sf-chain/operations',
        AI_CALL_LOGS: '/sf-chain/call-logs',
        AI_SYSTEM: '/sf-chain/system'
      }
    };
    
    return apiConfig;
  } catch (error) {
    console.warn('Failed to fetch API config, using defaults:', error);
    // 降级到默认配置
    apiConfig = {
      baseUrl: import.meta.env.DEV ? 'http://localhost:5001/api' : '',
      endpoints: {
        AI_MODELS: '/sf-chain/models',
        AI_OPERATIONS: '/sf-chain/operations',
        AI_CALL_LOGS: '/sf-chain/call-logs',
        AI_SYSTEM: '/sf-chain/system'
      }
    };
    return apiConfig;
  }
}

// 兼容性导出
export const API_CONFIG = {
  BASE_URL: '', // 将在运行时动态设置
  ENDPOINTS: {
    AI_MODELS: '/sf-chain/models',
    AI_OPERATIONS: '/sf-chain/operations',
    AI_CALL_LOGS: '/sf-chain/call-logs',
    AI_SYSTEM: '/sf-chain/system'
  }
};
