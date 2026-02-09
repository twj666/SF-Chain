<template>
  <div class="console-root">
    <HeaderBar :on-refresh="refreshOverview" />

    <main class="console-layout">
      <section class="overview page-shell">
        <div class="overview-head">
          <div>
            <h2 class="section-title">SF-Chain 配置中心</h2>
            <p class="section-sub">卡片式控制台，按租户和应用管理模型、Operation、密钥与持久化。</p>
          </div>
          <button class="btn btn-secondary" @click="refreshOverview">刷新概览</button>
        </div>
        <div class="kv-grid">
          <div class="kv-card">
            <div class="kv-label">模型总数</div>
            <div class="kv-value">{{ systemOverview.totalModels }}</div>
          </div>
          <div class="kv-card">
            <div class="kv-label">启用模型</div>
            <div class="kv-value">{{ systemOverview.enabledModels }}</div>
          </div>
          <div class="kv-card">
            <div class="kv-label">Operation总数</div>
            <div class="kv-value">{{ systemOverview.totalOperations }}</div>
          </div>
          <div class="kv-card">
            <div class="kv-label">已配置Operation</div>
            <div class="kv-value">{{ systemOverview.configuredOperations }}</div>
          </div>
        </div>
      </section>

      <section class="tabs-grid">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-card"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <div class="tab-title">{{ tab.title }}</div>
          <div class="tab-desc">{{ tab.desc }}</div>
        </button>
      </section>

      <section class="workspace page-shell">
        <ScopedModelConfig v-if="activeTab === 'models'" />
        <ScopedOperationConfig v-if="activeTab === 'operations'" />
        <TenantKeyManagement v-if="activeTab === 'tenant'" />
        <DatabaseBootstrapPanel v-if="activeTab === 'bootstrap'" />

        <AICallLogViewer
          v-if="activeTab === 'logs'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />

        <SystemManagement
          v-if="activeTab === 'system'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import HeaderBar from '@/components/HeaderBar.vue'
import ScopedModelConfig from '@/components/ScopedModelConfig.vue'
import ScopedOperationConfig from '@/components/ScopedOperationConfig.vue'
import AICallLogViewer from '@/components/AICallLogViewer.vue'
import SystemManagement from '@/components/SystemManagement.vue'
import TenantKeyManagement from '@/components/TenantKeyManagement.vue'
import DatabaseBootstrapPanel from '@/components/DatabaseBootstrapPanel.vue'
import { systemApi } from '@/services/systemApi'
import { getAuthToken } from '@/services/apiUtils'
import type { SystemOverview } from '@/types/system'

const tabs = [
  { key: 'models', title: '模型配置', desc: '租户/应用模型池' },
  { key: 'operations', title: 'Operation配置', desc: '节点映射与策略' },
  { key: 'tenant', title: '租户与密钥', desc: '租户/应用/API Key' },
  { key: 'bootstrap', title: '数据库', desc: '保存配置与初始化' },
  { key: 'logs', title: '调用日志', desc: '日志与统计分析' },
  { key: 'system', title: '系统管理', desc: '重置与维护操作' }
]

const activeTab = ref('models')

const systemOverview = ref<SystemOverview>({
  totalModels: 0,
  enabledModels: 0,
  configuredOperations: 0,
  totalOperations: 0,
  enabledOperations: 0,
  lastUpdate: Date.now()
})

async function refreshOverview() {
  try {
    const data = await systemApi.getSystemOverview()
    systemOverview.value = data
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : '获取系统概览失败'
    console.error(message)
  }
}

function handleOverviewUpdate(overview: SystemOverview) {
  systemOverview.value = { ...systemOverview.value, ...overview }
}

onMounted(() => {
  if (getAuthToken()) {
    refreshOverview()
  }
})
</script>

<style scoped>
.console-root {
  min-height: 100vh;
  padding: 1rem;
}

.console-layout {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.overview,
.workspace {
  padding: 1rem;
}

.overview-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
  margin-bottom: 0.95rem;
}

.tabs-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 0.65rem;
}

.tab-card {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: #f7fbfc;
  text-align: left;
  padding: 0.72rem;
  cursor: pointer;
  transition: transform 0.16s ease, border-color 0.16s ease, background-color 0.16s ease;
}

.tab-card:hover {
  transform: translateY(-1px);
  border-color: #8cc4d2;
}

.tab-card.active {
  border-color: #0f766e;
  background: linear-gradient(180deg, #edfffc 0%, #f4fbff 100%);
}

.tab-title {
  font-size: 0.95rem;
  font-weight: 800;
}

.tab-desc {
  margin-top: 0.25rem;
  font-size: 0.8rem;
  color: var(--text-sub);
}

@media (max-width: 1200px) {
  .tabs-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .console-root {
    padding: 0.7rem;
  }

  .tabs-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .overview-head {
    flex-direction: column;
  }
}
</style>
