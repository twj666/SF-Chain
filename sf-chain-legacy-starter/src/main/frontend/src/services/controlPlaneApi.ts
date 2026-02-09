import { API_CONFIG } from './apiConfig'
import { apiJsonRequest } from './apiUtils'

export interface TenantView {
  tenantId: string
  name: string
  description?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ApiKeyView {
  id: number
  tenantId: string
  appId: string
  keyName: string
  keyPrefix: string
  active: boolean
  createdAt: string
  updatedAt: string
  lastUsedAt?: string
}

export interface ApiKeyCreateResponse {
  id: number
  tenantId: string
  appId: string
  keyName: string
  apiKey: string
  keyPrefix: string
  createdAt: string
}

export interface AppView {
  id: number
  tenantId: string
  appId: string
  appName: string
  description?: string
  active: boolean
  createdAt: string
  updatedAt: string
}

const base = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_SYSTEM.replace('/system', '')}`

export const controlPlaneApi = {
  async listTenants(): Promise<TenantView[]> {
    return apiJsonRequest(`${base}/control/tenants`, {
      method: 'GET',
      requireAuth: true
    })
  },

  async createTenant(payload: { tenantId: string; name: string; description?: string }): Promise<TenantView> {
    return apiJsonRequest(`${base}/control/tenants`, {
      method: 'POST',
      body: JSON.stringify(payload),
      requireAuth: true
    })
  },

  async updateTenantStatus(tenantId: string, active: boolean): Promise<TenantView> {
    return apiJsonRequest(`${base}/control/tenants/${encodeURIComponent(tenantId)}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
      requireAuth: true
    })
  },

  async listApps(tenantId: string): Promise<AppView[]> {
    return apiJsonRequest(`${base}/control/tenants/${encodeURIComponent(tenantId)}/apps`, {
      method: 'GET',
      requireAuth: true
    })
  },

  async createApp(tenantId: string, payload: { appId: string; appName: string; description?: string }): Promise<AppView> {
    return apiJsonRequest(`${base}/control/tenants/${encodeURIComponent(tenantId)}/apps`, {
      method: 'POST',
      body: JSON.stringify(payload),
      requireAuth: true
    })
  },

  async updateAppStatus(tenantId: string, appId: string, active: boolean): Promise<AppView> {
    return apiJsonRequest(`${base}/control/tenants/${encodeURIComponent(tenantId)}/apps/${encodeURIComponent(appId)}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
      requireAuth: true
    })
  },

  async listApiKeys(tenantId: string, appId?: string): Promise<ApiKeyView[]> {
    const appQuery = appId ? `?appId=${encodeURIComponent(appId)}` : ''
    return apiJsonRequest(`${base}/control/tenants/${encodeURIComponent(tenantId)}/api-keys${appQuery}`, {
      method: 'GET',
      requireAuth: true
    })
  },

  async createApiKey(tenantId: string, payload: { appId: string; keyName: string }): Promise<ApiKeyCreateResponse> {
    return apiJsonRequest(`${base}/control/tenants/${encodeURIComponent(tenantId)}/api-keys`, {
      method: 'POST',
      body: JSON.stringify(payload),
      requireAuth: true
    })
  },

  async revokeApiKey(id: number): Promise<ApiKeyView> {
    return apiJsonRequest(`${base}/control/api-keys/${id}/revoke`, {
      method: 'PATCH',
      requireAuth: true
    })
  }
}
