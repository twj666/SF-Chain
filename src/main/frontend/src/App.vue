<template>
  <div class="api-config">
    <!-- 使用新的HeaderBar组件 -->
    <HeaderBar :on-refresh="fetchSystemOverview" />

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧Tab导航栏 (15%) -->
      <div class="sidebar">
        <div class="tab-navigation">
          <div class="tab-header">
            <h3>配置导航</h3>
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

        <!-- 系统状态卡片 -->
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

        <!-- 紧凑的统计信息卡片 -->
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

      <!-- 分隔线 -->
      <div class="divider"></div>

      <!-- 右侧内容区域 (85%) -->
      <div class="content-area">
        <!-- API信息配置组件 -->
        <ApiInfoConfig
          v-if="activeTab === 'api'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />

        <!-- AI节点配置组件 -->
        <AiNodeConfig
          v-if="activeTab === 'operations'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />

        <!-- AI调用日志组件 -->
        <AICallLogViewer
          v-if="activeTab === 'logs'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />

        <!-- 系统管理组件 -->
        <SystemManagement
          v-if="activeTab === 'system'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import HeaderBar from '@/components/HeaderBar.vue'
import ApiInfoConfig from '@/components/ApiInfoConfig.vue'
import AiNodeConfig from '@/components/AiNodeConfig.vue'
import SystemManagement from '@/components/SystemManagement.vue'
import AICallLogViewer from '@/components/AICallLogViewer.vue'
import { systemApi } from '@/services/systemApi'
import { getAuthToken } from '@/services/apiUtils'
import type { SystemOverview } from '@/types/system'

// Tab配置 - 优化后的图标
const tabs = [
  {
    key: 'api',
    title: 'AI模型',
    description: '配置AI模型API',
    icon: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 2L2 7L12 12L22 7L12 2Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
      <path d="M2 17L12 22L22 17" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
      <path d="M2 12L12 17L22 12" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
      <circle cx="12" cy="7" r="2" fill="currentColor" opacity="0.3"/>
      <circle cx="12" cy="12" r="1.5" fill="currentColor" opacity="0.5"/>
      <circle cx="12" cy="17" r="1" fill="currentColor" opacity="0.7"/>
    </svg>`
  },
  {
    key: 'operations',
    title: 'AI节点',
    description: '管理AI节点映射',
    icon: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
      <circle cx="12" cy="12" r="1" fill="currentColor"/>
      <path d="M12 1V6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <path d="M12 18V23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <path d="M4.22 4.22L7.76 7.76" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <path d="M16.24 16.24L19.78 19.78" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <path d="M1 12H6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <path d="M18 12H23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <path d="M4.22 19.78L7.76 16.24" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <path d="M16.24 7.76L19.78 4.22" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <circle cx="6" cy="6" r="1" fill="currentColor" opacity="0.6"/>
      <circle cx="18" cy="6" r="1" fill="currentColor" opacity="0.6"/>
      <circle cx="6" cy="18" r="1" fill="currentColor" opacity="0.6"/>
      <circle cx="18" cy="18" r="1" fill="currentColor" opacity="0.6"/>
    </svg>`
  },
  {
    key: 'logs',
    title: 'AI日志',
    description: '查看AI调用日志',
    icon: `<svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
      <polyline points="14,2 14,8 20,8" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
      <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <polyline points="10,9 9,9 8,9" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      <circle cx="12" cy="13" r="1" fill="currentColor" opacity="0.6"/>
      <circle cx="12" cy="17" r="1" fill="currentColor" opacity="0.6"/>
    </svg>`
  }
]

// 当前激活的Tab
const activeTab = ref('api')

// 系统概览数据
const systemOverview = ref<SystemOverview>({
  totalModels: 0,
  enabledModels: 0,
  configuredOperations: 0,
  totalOperations: 0,
  enabledOperations: 0,
  lastUpdate: Date.now()
})

// Tab切换
const switchTab = (tabKey: string) => {
  activeTab.value = tabKey
}

// 时间格式化
const formatTime = (timestamp: number) => {
  if (!timestamp) return '未知'
  return new Date(timestamp).toLocaleString()
}

// 获取系统概览
const fetchSystemOverview = async () => {
  try {
    const data = await systemApi.getSystemOverview()
    systemOverview.value = data
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '获取系统概览失败'
    console.error('Failed to fetch system overview:', errorMessage)
  }
}

// 处理概览数据更新
const handleOverviewUpdate = (overview: SystemOverview) => {
  systemOverview.value = { ...systemOverview.value, ...overview }
}

// 组件挂载时加载数据
onMounted(() => {
  const currentToken = getAuthToken()
  if (currentToken) {
    fetchSystemOverview()
  }
})
</script>

<style scoped>
.api-config {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 1rem;
}



/* 主要内容区域 */
.main-content {
  display: flex;
  gap: 0;
  height: calc(100vh - 160px);
}

/* ... existing code ... */

/* 响应式设计 */
@media (max-width: 768px) {
  .main-content {
    flex-direction: column;
    height: auto;
  }

  .sidebar {
    width: 100%;
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.2);
    margin-bottom: 1rem;
  }

  .divider {
    display: none;
  }

  .content-area {
    width: 100%;
    border-radius: 16px;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }

  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 0.25rem;
  }

  .stat-item {
    flex-direction: column;
    text-align: center;
    padding: 0.5rem;
  }

  .stat-value {
    font-size: 1rem;
    margin-bottom: 0.25rem;
  }

  .stat-label {
    font-size: 0.65rem;
  }
}

/* 保留原有的其他样式 */
/* 左侧导航栏 */
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

.icon-svg svg {
  width: 100%;
  height: 100%;
  transition: all 0.3s ease;
}

.tab-item:hover .icon-svg {
  color: #667eea;
  transform: scale(1.1);
}

.tab-item.active .icon-svg {
  color: #667eea;
  transform: scale(1.15);
}

.tab-item.active .icon-svg svg {
  filter: drop-shadow(0 2px 4px rgba(102, 126, 234, 0.3));
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

/* 系统状态卡片 */
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

/* 统计信息卡片 */
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

/* 分隔线 */
.divider {
  width: 1px;
  background: linear-gradient(to bottom,
    transparent 0%,
    rgba(226, 232, 240, 0.8) 20%,
    rgba(226, 232, 240, 0.8) 80%,
    transparent 100%);
  margin: 0;
}

/* 右侧内容区域 */
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
</style>
