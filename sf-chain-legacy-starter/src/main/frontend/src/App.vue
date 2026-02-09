<template>
  <div class="console-root">
    <HeaderBar :on-refresh="refreshOverview" />

    <div class="console-main">
      <aside class="sidebar">
        <h3 class="side-title">控制台</h3>
        <button
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-btn"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <span>{{ tab.title }}</span>
          <small>{{ tab.desc }}</small>
        </button>
      </aside>

      <section class="content">
        <ApiInfoConfig
          v-if="activeTab === 'models'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />

        <AiNodeConfig
          v-if="activeTab === 'operations'"
          :system-overview="systemOverview"
          @update-overview="handleOverviewUpdate"
        />

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

        <TenantKeyManagement v-if="activeTab === 'tenant'" />

        <DatabaseBootstrapPanel v-if="activeTab === 'bootstrap'" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import HeaderBar from '@/components/HeaderBar.vue'
import ApiInfoConfig from '@/components/ApiInfoConfig.vue'
import AiNodeConfig from '@/components/AiNodeConfig.vue'
import AICallLogViewer from '@/components/AICallLogViewer.vue'
import SystemManagement from '@/components/SystemManagement.vue'
import TenantKeyManagement from '@/components/TenantKeyManagement.vue'
import DatabaseBootstrapPanel from '@/components/DatabaseBootstrapPanel.vue'
import { systemApi } from '@/services/systemApi'
import { getAuthToken } from '@/services/apiUtils'
import type { SystemOverview } from '@/types/system'

const tabs = [
  { key: 'models', title: '模型配置', desc: '模型与参数' },
  { key: 'operations', title: 'Operation配置', desc: '节点映射' },
  { key: 'tenant', title: '租户与密钥', desc: '租户 + API Key' },
  { key: 'bootstrap', title: '数据库初始化', desc: '一键持久化' },
  { key: 'logs', title: '调用日志', desc: '日志与统计' },
  { key: 'system', title: '系统管理', desc: '重置与维护' }
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
  background: linear-gradient(180deg, #eef2ff 0%, #f8fafc 100%);
}

.console-main {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 1rem;
  padding: 1rem;
}

.sidebar {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 0.9rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.side-title {
  margin: 0 0 0.2rem;
  font-size: 1rem;
  color: #334155;
}

.tab-btn {
  text-align: left;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 10px;
  padding: 0.65rem 0.75rem;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.tab-btn small {
  color: #64748b;
}

.tab-btn.active {
  border-color: #3b82f6;
  background: #eff6ff;
}

.content {
  min-width: 0;
}

@media (max-width: 960px) {
  .console-main {
    grid-template-columns: 1fr;
  }
}
</style>
