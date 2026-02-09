import { API_CONFIG } from './apiConfig'
import { apiJsonRequest } from './apiUtils'

export interface DatabaseBootstrapRequest {
  databaseType: 'mysql' | 'postgresql'
  jdbcUrl: string
  username: string
  password: string
}

export interface DatabaseBootstrapStatus {
  configSaved: boolean
  databaseType?: string
  jdbcUrl?: string
  savedAt?: string
}

const base = `${API_CONFIG.BASE_URL}${API_CONFIG.ENDPOINTS.AI_SYSTEM.replace('/system', '')}`

export const bootstrapApi = {
  status(): Promise<DatabaseBootstrapStatus> {
    return apiJsonRequest(`${base}/bootstrap/database/status`, { method: 'GET', requireAuth: true })
  },

  testConnection(payload: DatabaseBootstrapRequest): Promise<{ message: string }> {
    return apiJsonRequest(`${base}/bootstrap/database/test`, {
      method: 'POST',
      body: JSON.stringify(payload),
      requireAuth: true
    })
  },

  initDatabase(payload: DatabaseBootstrapRequest): Promise<{ success: boolean; message: string; restartRequired: boolean }> {
    return apiJsonRequest(`${base}/bootstrap/database/init`, {
      method: 'POST',
      body: JSON.stringify(payload),
      requireAuth: true
    })
  }
}
