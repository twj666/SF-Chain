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
                :class="['tab-item', { active: outerTab === 'online' }]"
                @click="outerTab = 'online'"
              >
                <div class="tab-icon">
                  <div class="icon-svg" v-html="outerIcons.online"></div>
                </div>
                <div class="tab-content">
                  <div class="tab-title">在线应用</div>
                  <div class="tab-description">实时在线实例与快速进入</div>
                </div>
                <div class="tab-indicator"></div>
              </div>

              <div
                :class="['tab-item', { active: outerTab === 'tenant' }]"
                @click="outerTab = 'tenant'"
              >
                <div class="tab-icon">
                  <div class="icon-svg" v-html="outerIcons.tenant"></div>
                </div>
                <div class="tab-content">
                  <div class="tab-title">租户管理</div>
                  <div class="tab-description">租户、应用、API Key</div>
                </div>
                <div class="tab-indicator"></div>
              </div>

              <div
                :class="['tab-item', { active: outerTab === 'database' }]"
                @click="outerTab = 'database'"
              >
                <div class="tab-icon">
                  <div class="icon-svg" v-html="outerIcons.database"></div>
                </div>
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
            <section v-if="outerTab === 'online'" class="online-workspace">
              <div class="online-workspace-head">
                <div>
                  <h2>在线应用面板</h2>
                  <p>仅展示当前在线的租户应用，点击卡片可直达内层工作台。</p>
                  <p class="online-refresh-time">
                    最近刷新：{{ onlineLastRefreshAt ? formatTime(onlineLastRefreshAt) : '未刷新' }}
                  </p>
                </div>
                <button class="btn btn-secondary" :disabled="onlineRequesting" @click="loadOnlineApps(true)">
                  {{ onlineRequesting ? '刷新中...' : '刷新' }}
                </button>
              </div>

              <div v-if="onlineApps.length === 0" class="online-workspace-empty">
                {{ onlineRequesting ? '在线列表加载中...' : '当前没有在线实例' }}
              </div>
              <div v-else class="online-workspace-grid">
                <button
                  v-for="item in onlineApps"
                  :key="`${item.tenantId}:${item.appId}`"
                  class="online-workspace-card"
                  @click="openOnlineApp(item)"
                >
                  <div class="online-workspace-top">
                    <strong>{{ item.appName }}</strong>
                    <span class="state-chip state-on">在线: {{ item.instanceCount }}</span>
                  </div>
                  <div class="online-workspace-sub">
                    <span>{{ item.tenantName }}</span>
                    <span class="online-dot">·</span>
                    <span>{{ item.tenantId }}</span>
                  </div>
                  <div class="online-workspace-appid">{{ item.appId }}</div>
                  <div class="online-heartbeat-inline">
                    最近心跳 {{ formatRelativeTime(item.lastSeenAt) }}
                  </div>
                  <div v-if="item.instances.length > 0" class="online-instance-list">
                    <div
                      v-for="instance in item.instances.slice(0, 4)"
                      :key="`${item.tenantId}:${item.appId}:${instance.instanceId}`"
                      class="online-instance-row"
                    >
                      <code :title="instance.instanceId">{{ instance.instanceId }}</code>
                      <span>{{ formatRelativeTime(instance.lastSeenAt) }}</span>
                    </div>
                    <div v-if="item.instances.length > 4" class="online-instance-extra">
                      其余 {{ item.instances.length - 4 }} 个实例已折叠
                    </div>
                  </div>
                </button>
              </div>
            </section>

            <TenantKeyManagement
              v-else-if="outerTab === 'tenant'"
              :selected-tenant-id="selectedTenantId"
              :selected-app-id="selectedAppId"
              :refresh-key="tenantPanelRefreshKey"
              @scope-change="handleScopeChange"
            />
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import HeaderBar from '@/components/HeaderBar.vue'
import ApiInfoConfig from '@/components/ApiInfoConfig.vue'
import AiNodeConfig from '@/components/AiNodeConfig.vue'
import AICallLogViewer from '@/components/AICallLogViewer.vue'
import TenantKeyManagement from '@/components/TenantKeyManagement.vue'
import DatabaseBootstrapPanel from '@/components/DatabaseBootstrapPanel.vue'
import { controlPlaneApi, type OnlineAppView } from '@/services/controlPlaneApi'
import { getAuthToken } from '@/services/apiUtils'
import { clearScopeContext, getScopeContext, setScopeContext } from '@/services/scopeContext'
import type { SystemOverview } from '@/types/system'
import { toast } from '@/utils/toast'

type Page = 'portal' | 'workspace'
type OuterTab = 'online' | 'tenant' | 'database'

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
const outerTab = ref<OuterTab>('online')
const outerIcons = {
  online: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/><path d="M7 12h10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><path d="M12 7v10" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="12" cy="12" r="2" fill="currentColor"/></svg>`,
  tenant: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><rect x="4" y="10" width="16" height="10" rx="2" stroke="currentColor" stroke-width="2"/><path d="M8 10V7a4 4 0 118 0v3" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><circle cx="12" cy="15" r="1.5" fill="currentColor"/></svg>`,
  database: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"><ellipse cx="12" cy="6" rx="8" ry="3" stroke="currentColor" stroke-width="2"/><path d="M4 6V18C4 19.66 7.58 21 12 21C16.42 21 20 19.66 20 18V6" stroke="currentColor" stroke-width="2"/><path d="M4 12C4 13.66 7.58 15 12 15C16.42 15 20 13.66 20 12" stroke="currentColor" stroke-width="2"/></svg>`
}

const selectedTenantId = ref('')
const selectedAppId = ref('')
const tenantPanelRefreshKey = ref(0)
const onlineApps = ref<OnlineAppView[]>([])
const onlineRequesting = ref(false)
const onlineLastRefreshAt = ref<number | null>(null)
let onlinePollTimer: number | null = null

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

const formatRelativeTime = (timestamp?: string) => {
  if (!timestamp) return '无'
  const deltaSeconds = Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000)
  if (deltaSeconds < 0) return '刚刚'
  if (deltaSeconds < 60) return `${deltaSeconds}s 前`
  const minutes = Math.floor(deltaSeconds / 60)
  if (minutes < 60) return `${minutes}m 前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h 前`
  return `${Math.floor(hours / 24)}d 前`
}

async function fetchSystemOverview() {
  if (!selectedTenantId.value || !selectedAppId.value) {
    systemOverview.value = {
      totalModels: 0,
      enabledModels: 0,
      configuredOperations: 0,
      totalOperations: 0,
      enabledOperations: 0,
      lastUpdate: Date.now()
    }
    return
  }

  try {
    const [models, operations] = await Promise.all([
      controlPlaneApi.listModelConfigs(selectedTenantId.value, selectedAppId.value),
      controlPlaneApi.listOperationConfigs(selectedTenantId.value, selectedAppId.value)
    ])

    const totalModels = models.length
    const enabledModels = models.filter(item => item.active).length
    const totalOperations = operations.length
    const enabledOperations = operations.filter(item => item.active).length
    const configuredOperations = operations.filter(item => !!item.modelName && item.modelName.trim().length > 0).length

    systemOverview.value = {
      totalModels,
      enabledModels,
      configuredOperations,
      totalOperations,
      enabledOperations,
      lastUpdate: Date.now()
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '获取系统概览失败'
    console.error('Failed to fetch system overview:', errorMessage)
  }
}

async function handleHeaderRefresh() {
  tenantPanelRefreshKey.value += 1
  await loadOnlineApps(true)
  if (page.value === 'workspace') {
    await fetchSystemOverview()
  }
}

function openWorkspace() {
  if (!hasScope.value) {
    toast.error('请先在左侧选择租户和应用')
    return
  }
  setScopeContext(selectedTenantId.value, selectedAppId.value)
  page.value = 'workspace'
  fetchSystemOverview()
}

function openOnlineApp(item: OnlineAppView) {
  selectedTenantId.value = item.tenantId
  selectedAppId.value = item.appId
  setScopeContext(item.tenantId, item.appId)
  page.value = 'workspace'
  fetchSystemOverview()
}

async function loadOnlineApps(showErrorToast = false) {
  if (onlineRequesting.value) {
    return
  }
  if (!getAuthToken()) {
    onlineApps.value = []
    return
  }
  onlineRequesting.value = true
  try {
    const list = await controlPlaneApi.listOnlineApps(45)
    onlineApps.value = list.map(item => ({
      ...item,
      instances: Array.isArray(item.instances) ? item.instances : []
    }))
    onlineLastRefreshAt.value = Date.now()
  } catch (error) {
    if (showErrorToast) {
      toast.error(error instanceof Error ? error.message : '加载在线应用失败')
    } else {
      console.warn('load online apps failed', error)
    }
  } finally {
    onlineRequesting.value = false
  }
}

function startOnlinePolling() {
  stopOnlinePolling()
  onlinePollTimer = window.setInterval(() => {
    loadOnlineApps()
  }, 10000)
}

function stopOnlinePolling() {
  if (onlinePollTimer != null) {
    window.clearInterval(onlinePollTimer)
    onlinePollTimer = null
  }
}

function restoreScopeFromStorage() {
  const stored = getScopeContext()
  if (!stored) return
  selectedTenantId.value = stored.tenantId
  selectedAppId.value = stored.appId
}

function handleScopeChange(payload: { tenantId: string; appId: string }) {
  selectedTenantId.value = payload.tenantId
  selectedAppId.value = payload.appId
  if (payload.tenantId && payload.appId) {
    setScopeContext(payload.tenantId, payload.appId)
  } else {
    clearScopeContext()
  }
}

watch([selectedTenantId, selectedAppId], ([tenant, app]) => {
  if (tenant && app && page.value === 'workspace') {
    fetchSystemOverview()
  }
})

onMounted(async () => {
  if (!getAuthToken()) {
    clearScopeContext()
    return
  }
  restoreScopeFromStorage()
  await loadOnlineApps(true)
  startOnlinePolling()
  if (selectedTenantId.value && selectedAppId.value) {
    await fetchSystemOverview()
  }
})

onBeforeUnmount(() => {
  stopOnlinePolling()
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

.icon-svg {
  width: 24px;
  height: 24px;
  color: #718096;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tab-item:hover .icon-svg {
  color: #667eea;
  transform: scale(1.08);
}

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

.online-workspace {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
}

.online-workspace-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
}

.online-workspace-head h2 {
  margin: 0;
  font-size: 1.3rem;
}

.online-workspace-head p {
  margin: 0.3rem 0 0;
  color: #697a95;
  font-size: 0.84rem;
}

.online-refresh-time {
  margin-top: 0.35rem;
  color: #6a7a95;
  font-size: 0.76rem;
}

.online-workspace-empty {
  border: 1px dashed #ccd7eb;
  border-radius: 12px;
  background: #fbfcff;
  padding: 1rem;
  color: #6e7d97;
}

.online-workspace-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 420px));
  justify-content: flex-start;
  gap: 1rem;
}

.online-workspace-card {
  border: 1px solid #dde6f5;
  border-radius: 14px;
  background: #fff;
  padding: 0.95rem;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.online-workspace-card:hover {
  border-color: #a9bde8;
}

.online-workspace-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.online-workspace-top strong {
  font-size: 1.02rem;
  color: #1f2d46;
}

.state-chip {
  border-radius: 999px;
  padding: 0.15rem 0.5rem;
  font-size: 0.72rem;
  font-weight: 700;
}

.state-chip.state-on {
  background: #e9f8ef;
  color: #238e5d;
}

.online-workspace-sub {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  color: #647390;
  font-size: 0.82rem;
}

.online-dot {
  color: #9aaccb;
}

.online-workspace-appid {
  color: #3d5276;
  font-size: 0.82rem;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.online-heartbeat-inline {
  margin-top: 0.1rem;
  color: #5f6f8c;
  font-size: 0.8rem;
}

.online-instance-list {
  margin-top: 0.25rem;
  display: flex;
  flex-direction: column;
  gap: 0.32rem;
}

.online-instance-row {
  border: 1px solid #e2e9f8;
  border-radius: 9px;
  background: #fcfdff;
  padding: 0.28rem 0.45rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
}

.online-instance-row code {
  margin: 0;
  color: #35507f;
  font-size: 0.72rem;
  background: transparent;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  flex: 1;
}

.online-instance-row span {
  color: #5f6f8c;
  font-size: 0.72rem;
  flex-shrink: 0;
}

.online-instance-extra {
  color: #7f8da8;
  font-size: 0.72rem;
  padding-left: 0.2rem;
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

  .online-workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
