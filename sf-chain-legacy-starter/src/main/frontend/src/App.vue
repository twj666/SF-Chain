<template>
  <div class="app-shell">
    <HeaderBar :on-refresh="handleHeaderRefresh" />

    <main class="app-main">
      <section v-if="page === 'portal'" class="outer-console">
        <div class="outer-main-content">
          <aside class="outer-sidebar">
            <div class="tab-header">
              <h3>外层管理</h3>
              <p>租户、应用、密钥与持久化</p>
            </div>

            <div class="tab-list">
              <div
                :class="['tab-item', { active: outerTab === 'scope' }]"
                @click="outerTab = 'scope'"
              >
                <div class="tab-icon"><i class="fa-solid fa-building"></i></div>
                <div class="tab-content">
                  <div class="tab-title">租户 / 应用</div>
                  <div class="tab-description">作用域选择与概览</div>
                </div>
                <div class="tab-indicator"></div>
              </div>

              <div
                :class="['tab-item', { active: outerTab === 'tenant' }]"
                @click="outerTab = 'tenant'"
              >
                <div class="tab-icon"><i class="fa-solid fa-key"></i></div>
                <div class="tab-content">
                  <div class="tab-title">租户与密钥</div>
                  <div class="tab-description">租户、应用、API Key</div>
                </div>
                <div class="tab-indicator"></div>
              </div>

              <div
                :class="['tab-item', { active: outerTab === 'database' }]"
                @click="outerTab = 'database'"
              >
                <div class="tab-icon"><i class="fa-solid fa-database"></i></div>
                <div class="tab-content">
                  <div class="tab-title">持久化管理</div>
                  <div class="tab-description">数据库初始化与检测</div>
                </div>
                <div class="tab-indicator"></div>
              </div>
            </div>

            <div class="scope-summary">
              <div class="summary-row"><span>租户</span><strong>{{ selectedTenantId || '-' }}</strong></div>
              <div class="summary-row"><span>应用</span><strong>{{ selectedAppId || '-' }}</strong></div>
              <button class="btn btn-primary enter-btn" @click="openWorkspace">进入内层工作台</button>
            </div>
          </aside>

          <div class="divider"></div>

          <section class="outer-content-area">
            <template v-if="outerTab === 'scope'">
              <div class="outer-head">
                <h2>租户与应用作用域</h2>
                <button class="btn btn-secondary" :disabled="loadingTenants || loadingApps" @click="refreshScope">刷新</button>
              </div>

              <div class="scope-cards">
                <section class="scope-card">
                  <div class="scope-card-head">
                    <h3>租户</h3>
                    <span>{{ tenants.length }}</span>
                  </div>
                  <div v-if="loadingTenants" class="empty-tip">加载租户中...</div>
                  <div v-else-if="tenants.length === 0" class="empty-tip">暂无租户，请先在“租户与密钥”中创建。</div>
                  <div v-else class="scope-list">
                    <button
                      v-for="tenant in tenants"
                      :key="tenant.tenantId"
                      class="scope-item"
                      :class="{ active: selectedTenantId === tenant.tenantId }"
                      @click="selectTenant(tenant.tenantId)"
                    >
                      <span class="scope-name">{{ tenant.name }}</span>
                      <span class="scope-meta">{{ tenant.tenantId }}</span>
                    </button>
                  </div>
                </section>

                <section class="scope-card">
                  <div class="scope-card-head">
                    <h3>应用</h3>
                    <span>{{ apps.length }}</span>
                  </div>
                  <div v-if="!selectedTenantId" class="empty-tip">请先选择租户。</div>
                  <div v-else-if="loadingApps" class="empty-tip">加载应用中...</div>
                  <div v-else-if="apps.length === 0" class="empty-tip">该租户下暂无应用。</div>
                  <div v-else class="scope-list">
                    <button
                      v-for="app in apps"
                      :key="app.appId"
                      class="scope-item"
                      :class="{ active: selectedAppId === app.appId }"
                      @click="selectApp(app.appId)"
                    >
                      <span class="scope-name">{{ app.appName }}</span>
                      <span class="scope-meta">{{ app.appId }}</span>
                    </button>
                  </div>
                </section>
              </div>

              <div class="scope-overview-card">
                <div class="overview-item">
                  <span>当前租户</span>
                  <strong>{{ selectedTenantId || '-' }}</strong>
                </div>
                <div class="overview-item">
                  <span>当前应用</span>
                  <strong>{{ selectedAppId || '-' }}</strong>
                </div>
                <div class="overview-item right">
                  <button class="btn btn-primary" @click="openWorkspace">进入内层工作台</button>
                </div>
              </div>
            </template>

            <TenantKeyManagement v-else-if="outerTab === 'tenant'" />
            <DatabaseBootstrapPanel v-else-if="outerTab === 'database'" />
          </section>
        </div>
      </section>

      <section v-else-if="page === 'workspace'" class="legacy-inner">
        <div class="main-content">
          <div class="sidebar">
            <div class="tab-navigation">
              <div class="tab-header">
                <h3>配置导航</h3>
                <button class="back-link" @click="page = 'portal'">返回外层</button>
              </div>
              <div class="tab-list">
                <div
                  v-for="tab in tabs"
                  :key="tab.key"
                  :class="['tab-item', { active: activeTab === tab.key }]"
                  @click="switchTab(tab.key)"
                >
                  <div class="tab-icon">
                    <div class="icon-svg" v-html="tab.icon"></div>
                  </div>
                  <div class="tab-content">
                    <div class="tab-title">{{ tab.title }}</div>
                    <div class="tab-description">{{ tab.description }}</div>
                  </div>
                  <div class="tab-indicator"></div>
                </div>
              </div>
            </div>

            <div class="status-card">
              <div class="status-header">
                <span class="status-icon">📊</span>
                <span class="status-title">系统状态</span>
              </div>
              <div class="status-content">
                <div class="status-item">
                  <span class="status-label">运行状态</span>
                  <span class="status-value running">正常运行</span>
                </div>
                <div class="status-item">
                  <span class="status-label">最后更新</span>
                  <span class="status-value">{{ formatTime(systemOverview.lastUpdate) }}</span>
                </div>
              </div>
            </div>

            <div class="stats-card">
              <div class="stats-header">
                <span class="stats-icon">📈</span>
                <span class="stats-title">统计信息</span>
              </div>
              <div class="stats-grid">
                <div class="stat-item">
                  <span class="stat-value">{{ systemOverview.totalModels || 0 }}</span>
                  <span class="stat-label">总模型</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value">{{ systemOverview.enabledModels || 0 }}</span>
                  <span class="stat-label">已启用</span>
                </div>
                <div class="stat-item">
                  <span class="stat-value">{{ systemOverview.configuredOperations || 0 }}</span>
                  <span class="stat-label">已配置</span>
                </div>
              </div>
            </div>
          </div>

          <div class="divider"></div>

          <div class="content-area">
            <ApiInfoConfig v-if="activeTab === 'api'" :key="innerViewKey" />
            <AiNodeConfig v-if="activeTab === 'operations'" :key="innerViewKey" />
            <AICallLogViewer v-if="activeTab === 'logs'" :key="innerViewKey" />
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import HeaderBar from '@/components/HeaderBar.vue'
import ApiInfoConfig from '@/components/ApiInfoConfig.vue'
import AiNodeConfig from '@/components/AiNodeConfig.vue'
import AICallLogViewer from '@/components/AICallLogViewer.vue'
import TenantKeyManagement from '@/components/TenantKeyManagement.vue'
import DatabaseBootstrapPanel from '@/components/DatabaseBootstrapPanel.vue'
import { controlPlaneApi, type AppView, type TenantView } from '@/services/controlPlaneApi'
import { getAuthToken } from '@/services/apiUtils'
import { clearScopeContext, getScopeContext, setScopeContext } from '@/services/scopeContext'
import { systemApi } from '@/services/systemApi'
import type { SystemOverview } from '@/types/system'
import { toast } from '@/utils/toast'

type Page = 'portal' | 'workspace'
type OuterTab = 'scope' | 'tenant' | 'database'

const tabs = [
  {
    key: 'api',
    title: 'AI模型',
    description: '配置AI模型API',
    icon: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><circle cx="12" cy="7" r="2" fill="currentColor" opacity="0.3"/><circle cx="12" cy="12" r="1.5" fill="currentColor" opacity="0.5"/><circle cx="12" cy="17" r="1" fill="currentColor" opacity="0.7"/></svg>`
  },
  {
    key: 'operations',
    title: 'AI节点',
    description: '管理AI节点映射',
    icon: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="1" fill="currentColor"/><path d="M12 1V6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M12 18V23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M4.22 4.22L7.76 7.76" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M16.24 16.24L19.78 19.78" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M1 12H6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M18 12H23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M4.22 19.78L7.76 16.24" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M16.24 7.76L19.78 4.22" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="6" cy="6" r="1" fill="currentColor" opacity="0.6"/><circle cx="18" cy="6" r="1" fill="currentColor" opacity="0.6"/><circle cx="6" cy="18" r="1" fill="currentColor" opacity="0.6"/><circle cx="18" cy="18" r="1" fill="currentColor" opacity="0.6"/></svg>`
  },
  {
    key: 'logs',
    title: 'AI日志',
    description: '查看AI调用日志',
    icon: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><polyline points="14,2 14,8 20,8" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/><line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><polyline points="10,9 9,9 8,9" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="12" cy="13" r="1" fill="currentColor" opacity="0.6"/><circle cx="12" cy="17" r="1" fill="currentColor" opacity="0.6"/></svg>`
  }
]

const page = ref<Page>('portal')
const activeTab = ref('api')
const outerTab = ref<OuterTab>('scope')

const tenants = ref<TenantView[]>([])
const apps = ref<AppView[]>([])
const selectedTenantId = ref('')
const selectedAppId = ref('')
const loadingTenants = ref(false)
const loadingApps = ref(false)

const systemOverview = ref<SystemOverview>({
  totalModels: 0,
  enabledModels: 0,
  configuredOperations: 0,
  totalOperations: 0,
  enabledOperations: 0,
  lastUpdate: Date.now()
})

const hasScope = computed(() => !!selectedTenantId.value && !!selectedAppId.value)
const innerViewKey = computed(() => `${selectedTenantId.value}:${selectedAppId.value}:${activeTab.value}`)

const switchTab = (tabKey: string) => {
  activeTab.value = tabKey
}

const formatTime = (timestamp: number) => {
  if (!timestamp) return '未知'
  return new Date(timestamp).toLocaleString()
}

async function fetchSystemOverview() {
  try {
    const data = await systemApi.getSystemOverview()
    systemOverview.value = data
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '获取系统概览失败'
    console.error('Failed to fetch system overview:', errorMessage)
  }
}

async function handleHeaderRefresh() {
  await refreshScope()
  if (page.value === 'workspace') {
    await fetchSystemOverview()
  }
}

function openWorkspace() {
  if (!hasScope.value) {
    toast.error('请先在左侧选择租户和应用')
    return
  }
  page.value = 'workspace'
  fetchSystemOverview()
}

async function refreshScope() {
  if (!getAuthToken()) {
    tenants.value = []
    apps.value = []
    selectedTenantId.value = ''
    selectedAppId.value = ''
    clearScopeContext()
    page.value = 'portal'
    return
  }

  loadingTenants.value = true
  try {
    tenants.value = await controlPlaneApi.listTenants()

    if (selectedTenantId.value && !tenants.value.some(item => item.tenantId === selectedTenantId.value)) {
      selectedTenantId.value = ''
      selectedAppId.value = ''
      apps.value = []
      clearScopeContext()
      page.value = 'portal'
    }

    if (selectedTenantId.value) {
      await loadApps(selectedTenantId.value)
    }
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '加载租户失败')
  } finally {
    loadingTenants.value = false
  }
}

async function loadApps(tenantId: string) {
  loadingApps.value = true
  try {
    apps.value = await controlPlaneApi.listApps(tenantId)
    if (selectedAppId.value && !apps.value.some(item => item.appId === selectedAppId.value)) {
      selectedAppId.value = ''
      clearScopeContext()
      page.value = 'portal'
    }
  } catch (error) {
    toast.error(error instanceof Error ? error.message : '加载应用失败')
  } finally {
    loadingApps.value = false
  }
}

async function selectTenant(tenantId: string) {
  if (tenantId === selectedTenantId.value) {
    return
  }
  selectedTenantId.value = tenantId
  selectedAppId.value = ''
  apps.value = []
  clearScopeContext()
  await loadApps(tenantId)
}

function selectApp(appId: string) {
  selectedAppId.value = appId
  if (selectedTenantId.value && appId) {
    setScopeContext(selectedTenantId.value, appId)
  }
}

async function restoreScopeFromStorage() {
  const stored = getScopeContext()
  if (!stored) {
    return
  }

  const tenantExists = tenants.value.some(item => item.tenantId === stored.tenantId)
  if (!tenantExists) {
    clearScopeContext()
    return
  }

  selectedTenantId.value = stored.tenantId
  await loadApps(stored.tenantId)

  const appExists = apps.value.some(item => item.appId === stored.appId)
  if (!appExists) {
    clearScopeContext()
    return
  }

  selectedAppId.value = stored.appId
}

watch([selectedTenantId, selectedAppId], ([tenant, app]) => {
  if (tenant && app && page.value === 'workspace') {
    fetchSystemOverview()
  }
})

onMounted(async () => {
  if (getAuthToken()) {
    await refreshScope()
    await restoreScopeFromStorage()
    if (selectedTenantId.value && selectedAppId.value) {
      await fetchSystemOverview()
    }
  }
})
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  padding: 1rem;
  background: #f6f7fb;
}

.app-main {
  display: grid;
  gap: 1rem;
}

.outer-console {
  border-radius: 16px;
}

.outer-main-content {
  display: flex;
  gap: 0;
  min-height: calc(100vh - 160px);
}

.outer-sidebar {
  width: 300px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px 0 0 16px;
  padding: 1.4rem 1rem;
  border: 1px solid #eceff6;
  border-right: none;
  display: flex;
  flex-direction: column;
}

.outer-sidebar .tab-header {
  margin-bottom: 1rem;
}

.outer-sidebar .tab-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: #2d3748;
}

.outer-sidebar .tab-header p {
  margin: 0.35rem 0 0;
  color: #718096;
  font-size: 0.82rem;
}

.tab-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.tab-item {
  position: relative;
  padding: 1rem;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.tab-item:hover {
  background: rgba(102, 126, 234, 0.05);
  border-color: rgba(102, 126, 234, 0.2);
}

.tab-item.active {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
  border-color: #667eea;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.tab-icon {
  min-width: 2rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-icon i,
.icon-svg {
  width: 24px;
  height: 24px;
  color: #718096;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-item:hover .tab-icon i,
.tab-item:hover .icon-svg {
  color: #667eea;
  transform: scale(1.08);
}

.tab-item.active .tab-icon i,
.tab-item.active .icon-svg {
  color: #667eea;
}

.tab-content {
  flex: 1;
}

.tab-title {
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 0.25rem;
  font-size: 0.9rem;
}

.tab-description {
  font-size: 0.75rem;
  color: #718096;
  line-height: 1.3;
}

.tab-indicator {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: #667eea;
  border-radius: 2px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.tab-item.active .tab-indicator {
  opacity: 1;
}

.scope-summary {
  margin-top: 1rem;
  border: 1px solid #e9edf5;
  border-radius: 12px;
  padding: 0.75rem;
  background: #fff;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  font-size: 0.84rem;
}

.summary-row:last-of-type {
  margin-bottom: 0.75rem;
}

.summary-row strong {
  max-width: 65%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.enter-btn {
  width: 100%;
}

.divider {
  width: 1px;
  background: linear-gradient(to bottom, transparent 0%, rgba(226, 232, 240, 0.8) 20%, rgba(226, 232, 240, 0.8) 80%, transparent 100%);
}

.outer-content-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 0 16px 16px 0;
  padding: 1.5rem;
  border: 1px solid #eceff6;
  border-left: none;
  overflow-y: auto;
}

.outer-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.outer-head h2 {
  margin: 0;
  font-size: 1.35rem;
}

.scope-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.scope-card {
  border: 1px solid #e8ecf4;
  border-radius: 14px;
  background: #fff;
  padding: 0.9rem;
}

.scope-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.6rem;
}

.scope-card-head h3 {
  margin: 0;
  font-size: 1rem;
}

.scope-card-head span {
  color: #718096;
  font-size: 0.78rem;
}

.empty-tip {
  color: #718096;
  font-size: 0.84rem;
}

.scope-list {
  display: grid;
  gap: 0.55rem;
}

.scope-item {
  border: 1px solid #dfe6f3;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  padding: 0.65rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.scope-item:hover {
  border-color: #b7c8ec;
}

.scope-item.active {
  border-color: #667eea;
  background: #edf2ff;
}

.scope-name {
  display: block;
  font-weight: 700;
  font-size: 0.9rem;
}

.scope-meta {
  display: block;
  margin-top: 2px;
  color: #718096;
  font-size: 0.76rem;
}

.scope-overview-card {
  margin-top: 1rem;
  border: 1px solid #e8ecf4;
  border-radius: 12px;
  background: #fff;
  padding: 0.9rem;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.7rem;
  align-items: center;
}

.overview-item {
  display: grid;
  gap: 0.2rem;
}

.overview-item span {
  color: #718096;
  font-size: 0.76rem;
}

.overview-item strong {
  font-size: 0.92rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-item.right {
  display: flex;
  justify-content: flex-end;
}

.legacy-inner {
  min-height: auto;
  background: transparent;
  padding: 0;
  border-radius: 0;
}

.main-content {
  display: flex;
  gap: 0;
  min-height: calc(100vh - 140px);
}

.sidebar {
  width: 15%;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px 0 0 16px;
  padding: 2rem 1.5rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-right: none;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.tab-navigation {
  flex: 1;
}

.tab-header {
  margin-bottom: 1.5rem;
}

.tab-header h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #2d3748;
  margin: 0;
}

.back-link {
  margin-top: 0.5rem;
  border: none;
  background: transparent;
  color: #667eea;
  cursor: pointer;
  padding: 0;
}

.icon-svg svg {
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
}

.tab-item.active .icon-svg svg {
  filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.3));
}

.status-card {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  padding: 1rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: 0.75rem;
}

.status-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.status-icon {
  font-size: 1rem;
}

.status-title {
  font-weight: 600;
  color: #2d3748;
  font-size: 0.85rem;
}

.status-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-label {
  font-size: 0.75rem;
  color: #718096;
}

.status-value {
  font-size: 0.75rem;
  font-weight: 600;
  color: #2d3748;
}

.status-value.running {
  color: #22c55e;
}

.stats-card {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  padding: 1rem;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.stats-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}

.stats-icon {
  font-size: 1rem;
}

.stats-title {
  font-weight: 600;
  color: #2d3748;
  font-size: 0.85rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 0.5rem;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
}

.stat-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #667eea;
}

.stat-label {
  font-size: 0.7rem;
  color: #718096;
  font-weight: 500;
}

.content-area {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 0 16px 16px 0;
  padding: 2rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-left: none;
  overflow-y: auto;
}

@media (max-width: 1200px) {
  .outer-main-content,
  .main-content {
    flex-direction: column;
  }

  .outer-sidebar,
  .sidebar {
    width: 100%;
    border-radius: 16px;
    border-right: 1px solid #eceff6;
  }

  .outer-content-area,
  .content-area {
    border-radius: 16px;
    border-left: 1px solid #eceff6;
  }

  .divider {
    display: none;
  }

  .scope-cards {
    grid-template-columns: 1fr;
  }

  .scope-overview-card {
    grid-template-columns: 1fr;
  }

  .overview-item.right {
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .app-shell {
    padding: 0.75rem;
  }

  .legacy-inner .stats-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 0.25rem;
  }

  .legacy-inner .stat-item {
    flex-direction: column;
    text-align: center;
    padding: 0.5rem;
  }

  .legacy-inner .stat-value {
    font-size: 1rem;
    margin-bottom: 0.25rem;
  }

  .legacy-inner .stat-label {
    font-size: 0.65rem;
  }
}
</style>
