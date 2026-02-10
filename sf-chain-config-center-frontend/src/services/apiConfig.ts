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
    const apiPrefix = import.meta.env.VITE_API_PREFIX || '/sf-chain';
    const serverOrigin = import.meta.env.VITE_SERVER_ORIGIN || '';

    const configUrl = `${serverOrigin}${apiPrefix}/config/api-info`;

    const response = await fetch(configUrl);
    const config = await response.json();

    apiConfig = {
      baseUrl: config.baseUrl || '',
      endpoints: config.endpoints || {
        AI_MODELS: `${apiPrefix}/models`,
        AI_OPERATIONS: `${apiPrefix}/operations`,
        AI_CALL_LOGS: `${apiPrefix}/ai-logs`,
        AI_SYSTEM: `${apiPrefix}/system`
      }
    };

    return apiConfig;
  } catch (error) {
    console.warn('Failed to fetch API config, using defaults:', error);
    const apiPrefix = import.meta.env.VITE_API_PREFIX || '/sf-chain';
    const serverOrigin = import.meta.env.VITE_SERVER_ORIGIN || '';

    apiConfig = {
      baseUrl: serverOrigin,
      endpoints: {
        AI_MODELS: `${apiPrefix}/models`,
        AI_OPERATIONS: `${apiPrefix}/operations`,
        AI_CALL_LOGS: `${apiPrefix}/ai-logs`,
        AI_SYSTEM: `${apiPrefix}/system`
      }
    };
    return apiConfig;
  }
}

// 兼容性导出
const apiPrefix = import.meta.env.VITE_API_PREFIX || '/sf-chain';
const serverOrigin = import.meta.env.VITE_SERVER_ORIGIN || '';
export const API_CONFIG = {
  BASE_URL: serverOrigin,
  ENDPOINTS: {
    AI_MODELS: `${apiPrefix}/models`,
    AI_OPERATIONS: `${apiPrefix}/operations`,
    AI_CALL_LOGS: `${apiPrefix}/ai-logs`,
    AI_SYSTEM: `${apiPrefix}/system`
  }
};
