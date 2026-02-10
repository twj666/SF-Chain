const SCOPE_STORAGE_KEY = 'sf_chain_scope'

export interface ScopeContext {
  tenantId: string
  appId: string
}

export function getScopeContext(): ScopeContext | null {
  try {
    const raw = localStorage.getItem(SCOPE_STORAGE_KEY)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as Partial<ScopeContext>
    if (!parsed.tenantId || !parsed.appId) {
      return null
    }
    return {
      tenantId: String(parsed.tenantId),
      appId: String(parsed.appId)
    }
  } catch {
    return null
  }
}

export function setScopeContext(tenantId: string, appId: string): void {
  localStorage.setItem(SCOPE_STORAGE_KEY, JSON.stringify({ tenantId, appId }))
}

export function clearScopeContext(): void {
  localStorage.removeItem(SCOPE_STORAGE_KEY)
}
