<template>
  <div class="tenant-shell">
    <header class="panel-head">
      <div>
        <h2 class="section-title">租户管理</h2>
        <p class="section-sub">按顺序完成：选择租户 -> 选择应用 -> 管理 API Key。</p>
      </div>
      <div class="context" v-if="selectedTenantId">
        <span class="pill pill-ok">租户 {{ selectedTenantId }}</span>
        <span class="pill" :class="selectedAppId ? 'pill-ok' : 'pill-off'">应用 {{ selectedAppId || '未选择' }}</span>
      </div>
    </header>

    <div class="board-grid">
      <section class="lane soft-card">
        <div class="lane-head">
          <h3>租户</h3>
          <span class="count-badge">{{ tenants.length }}</span>
        </div>
        <p class="lane-sub">先选择租户，再进入应用管理。</p>

        <div v-if="loadingTenants" class="lane-empty">租户加载中...</div>
        <div v-else class="entity-grid">
          <article
            v-for="tenant in tenants"
            :key="tenant.tenantId"
            class="entity-card"
            :class="{ active: selectedTenantId === tenant.tenantId }"
            @click="selectTenant(tenant.tenantId)"
          >
            <div class="entity-top">
              <div class="entity-title">{{ tenant.name }}</div>
              <span class="state-pill" :class="tenant.active ? 'state-on' : 'state-off'">{{ tenant.active ? '启用' : '禁用' }}</span>
            </div>
            <div class="entity-meta">{{ tenant.tenantId }}</div>
            <div class="entity-desc">{{ tenant.description || '无描述' }}</div>
            <div class="entity-actions">
              <button class="btn btn-secondary" @click.stop="toggleTenant(tenant)">{{ tenant.active ? '禁用' : '启用' }}</button>
            </div>
          </article>

          <article class="entity-card create-card">
            <div class="create-title">+ 创建租户</div>
            <div class="create-form">
              <input v-model="tenantForm.tenantId" class="input" placeholder="tenantId（poet-prod）" />
              <input v-model="tenantForm.name" class="input" placeholder="租户名称" />
              <input v-model="tenantForm.description" class="input" placeholder="描述（可选）" />
              <button class="btn btn-primary" :disabled="creatingTenant" @click="createTenant">
                {{ creatingTenant ? '创建中...' : '创建租户' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="lane soft-card" :class="{ muted: !selectedTenantId }">
        <div class="lane-head">
          <h3>应用</h3>
          <span class="count-badge">{{ apps.length }}</span>
        </div>
        <p class="lane-sub">在当前租户下管理应用。</p>

        <div v-if="!selectedTenantId" class="lane-empty">请先选择租户</div>
        <div v-else-if="loadingApps" class="lane-empty">应用加载中...</div>
        <div v-else class="entity-grid">
          <article
            v-for="app in apps"
            :key="app.appId"
            class="entity-card"
            :class="{ active: selectedAppId === app.appId }"
            @click="selectApp(app.appId)"
          >
            <div class="entity-top">
              <div class="entity-title">{{ app.appName }}</div>
              <span class="state-pill" :class="app.active ? 'state-on' : 'state-off'">{{ app.active ? '启用' : '禁用' }}</span>
            </div>
            <div class="entity-meta">{{ app.appId }}</div>
            <div class="entity-desc">{{ app.description || '无描述' }}</div>
            <div class="entity-actions">
              <button class="btn btn-secondary" @click.stop="toggleApp(app)">{{ app.active ? '禁用' : '启用' }}</button>
            </div>
          </article>

          <article class="entity-card create-card">
            <div class="create-title">+ 创建应用</div>
            <div class="create-form">
              <input v-model="appForm.appId" class="input" placeholder="appId（poet-agent）" :disabled="!selectedTenantId" />
              <input v-model="appForm.appName" class="input" placeholder="应用名称" :disabled="!selectedTenantId" />
              <input v-model="appForm.description" class="input" placeholder="描述（可选）" :disabled="!selectedTenantId" />
              <button class="btn btn-primary" :disabled="creatingApp || !selectedTenantId" @click="createApp">
                {{ creatingApp ? '创建中...' : '创建应用' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="lane soft-card" :class="{ muted: !selectedAppId }">
        <div class="lane-head">
          <h3>API Key</h3>
          <span class="count-badge">{{ keys.length }}</span>
        </div>
        <p class="lane-sub">仅展示前缀；新申请 Key 只显示一次。</p>

        <div v-if="createdKey" class="new-key soft-card">
          <div class="new-key-title">新 Key（请立即保存）</div>
          <code>{{ createdKey }}</code>
        </div>

        <div v-if="!selectedAppId" class="lane-empty">请先选择应用</div>
        <div v-else-if="loadingKeys" class="lane-empty">Key 加载中...</div>
        <div v-else class="entity-grid">
          <article v-for="item in keys" :key="item.id" class="entity-card key-card">
            <div class="entity-top">
              <div class="entity-title">{{ item.keyName }}</div>
              <span class="state-pill" :class="item.active ? 'state-on' : 'state-off'">{{ item.active ? '启用' : '禁用' }}</span>
            </div>
            <div class="entity-meta">{{ item.keyPrefix }}</div>
            <div class="entity-desc">创建时间：{{ formatDate(item.createdAt) }}</div>
            <div class="entity-actions">
              <button class="btn btn-danger" :disabled="!item.active" @click="revoke(item.id)">吊销</button>
            </div>
          </article>

          <article class="entity-card create-card">
            <div class="create-title">+ 申请 Key</div>
            <div class="create-form one-line">
              <input v-model="keyForm.keyName" class="input" placeholder="key 名称（prod-key）" :disabled="!selectedAppId" />
              <button class="btn btn-primary" :disabled="creatingKey || !selectedAppId" @click="createKey">
                {{ creatingKey ? '申请中...' : '申请 Key' }}
              </button>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { controlPlaneApi, type ApiKeyView, type AppView, type TenantView } from '@/services/controlPlaneApi'
import { toast } from '@/utils/toast'

const props = withDefaults(defineProps<{
  selectedTenantId?: string
  selectedAppId?: string
  refreshKey?: number
}>(), {
  selectedTenantId: '',
  selectedAppId: '',
  refreshKey: 0
})

const emit = defineEmits<{
  (e: 'scope-change', payload: { tenantId: string; appId: string }): void
}>()

const tenants = ref<TenantView[]>([])
const apps = ref<AppView[]>([])
const keys = ref<ApiKeyView[]>([])
const selectedTenantId = ref(props.selectedTenantId)
const selectedAppId = ref(props.selectedAppId)
const loadingTenants = ref(false)
const loadingApps = ref(false)
const loadingKeys = ref(false)
const creatingTenant = ref(false)
const creatingApp = ref(false)
const creatingKey = ref(false)
const createdKey = ref('')

const tenantForm = ref({ tenantId: '', name: '', description: '' })
const appForm = ref({ appId: '', appName: '', description: '' })
const keyForm = ref({ keyName: '' })

function formatDate(value?: string | number | null) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

async function loadTenants() {
  loadingTenants.value = true
  try {
    tenants.value = await controlPlaneApi.listTenants()
    if (selectedTenantId.value && !tenants.value.some(item => item.tenantId === selectedTenantId.value)) {
      selectedTenantId.value = ''
      selectedAppId.value = ''
      apps.value = []
      keys.value = []
      createdKey.value = ''
      emit('scope-change', { tenantId: '', appId: '' })
    }
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载租户失败')
  } finally {
    loadingTenants.value = false
  }
}

async function createTenant() {
  if (!tenantForm.value.tenantId || !tenantForm.value.name) {
    toast.error('tenantId 和 name 必填')
    return
  }
  creatingTenant.value = true
  try {
    const createdTenantId = tenantForm.value.tenantId
    await controlPlaneApi.createTenant(tenantForm.value)
    tenantForm.value = { tenantId: '', name: '', description: '' }
    toast.success('租户创建成功')
    await loadTenants()
    selectTenant(createdTenantId)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '创建租户失败')
  } finally {
    creatingTenant.value = false
  }
}

async function toggleTenant(tenant: TenantView) {
  try {
    await controlPlaneApi.updateTenantStatus(tenant.tenantId, !tenant.active)
    toast.success('租户状态已更新')
    await loadTenants()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '更新租户失败')
  }
}

async function loadApps(tenantId: string) {
  loadingApps.value = true
  try {
    apps.value = await controlPlaneApi.listApps(tenantId)
    if (selectedAppId.value && !apps.value.some(item => item.appId === selectedAppId.value)) {
      selectedAppId.value = ''
      keys.value = []
      createdKey.value = ''
      emit('scope-change', { tenantId, appId: '' })
    }
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载应用失败')
  } finally {
    loadingApps.value = false
  }
}

async function createApp() {
  if (!selectedTenantId.value) {
    toast.error('请先选择租户')
    return
  }
  if (!appForm.value.appId || !appForm.value.appName) {
    toast.error('appId 和 appName 必填')
    return
  }
  creatingApp.value = true
  try {
    const createdAppId = appForm.value.appId
    await controlPlaneApi.createApp(selectedTenantId.value, appForm.value)
    appForm.value = { appId: '', appName: '', description: '' }
    toast.success('应用创建成功')
    await loadApps(selectedTenantId.value)
    selectApp(createdAppId)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '创建应用失败')
  } finally {
    creatingApp.value = false
  }
}

async function toggleApp(app: AppView) {
  if (!selectedTenantId.value) {
    return
  }
  try {
    await controlPlaneApi.updateAppStatus(selectedTenantId.value, app.appId, !app.active)
    toast.success('应用状态已更新')
    await loadApps(selectedTenantId.value)
    if (selectedAppId.value === app.appId && app.active) {
      selectedAppId.value = ''
      keys.value = []
      emit('scope-change', { tenantId: selectedTenantId.value, appId: '' })
    }
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '更新应用失败')
  }
}

async function loadKeys(tenantId: string, appId: string) {
  loadingKeys.value = true
  try {
    keys.value = await controlPlaneApi.listApiKeys(tenantId, appId)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载 Key 失败')
  } finally {
    loadingKeys.value = false
  }
}

function selectTenant(tenantId: string) {
  selectedTenantId.value = tenantId
  selectedAppId.value = ''
  apps.value = []
  keys.value = []
  createdKey.value = ''
  emit('scope-change', { tenantId, appId: '' })
  loadApps(tenantId)
}

function selectApp(appId: string) {
  if (!selectedTenantId.value) {
    return
  }
  selectedAppId.value = appId
  createdKey.value = ''
  emit('scope-change', { tenantId: selectedTenantId.value, appId })
  loadKeys(selectedTenantId.value, appId)
}

async function createKey() {
  if (!selectedTenantId.value || !selectedAppId.value) {
    toast.error('请先选择租户和应用')
    return
  }
  if (!keyForm.value.keyName) {
    toast.error('keyName 必填')
    return
  }
  creatingKey.value = true
  try {
    const result = await controlPlaneApi.createApiKey(selectedTenantId.value, {
      appId: selectedAppId.value,
      keyName: keyForm.value.keyName
    })
    createdKey.value = result.apiKey
    keyForm.value = { keyName: '' }
    toast.success('API Key 申请成功')
    await loadKeys(selectedTenantId.value, selectedAppId.value)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '申请 Key 失败')
  } finally {
    creatingKey.value = false
  }
}

async function revoke(keyId: number) {
  if (!selectedTenantId.value || !selectedAppId.value) {
    return
  }
  try {
    await controlPlaneApi.revokeApiKey(keyId)
    toast.success('Key 已吊销')
    await loadKeys(selectedTenantId.value, selectedAppId.value)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '吊销失败')
  }
}

watch(
  () => props.selectedTenantId,
  async (tenantId) => {
    if (tenantId === selectedTenantId.value) {
      return
    }
    selectedTenantId.value = tenantId || ''
    selectedAppId.value = ''
    apps.value = []
    keys.value = []
    createdKey.value = ''
    if (selectedTenantId.value) {
      await loadApps(selectedTenantId.value)
    }
  }
)

watch(
  () => props.selectedAppId,
  async (appId) => {
    if (appId === selectedAppId.value) {
      return
    }
    selectedAppId.value = appId || ''
    keys.value = []
    createdKey.value = ''
    if (selectedTenantId.value && selectedAppId.value) {
      await loadKeys(selectedTenantId.value, selectedAppId.value)
    }
  }
)

watch(
  () => props.refreshKey,
  async () => {
    await loadTenants()
    if (!selectedTenantId.value) {
      return
    }
    await loadApps(selectedTenantId.value)
    if (selectedAppId.value) {
      await loadKeys(selectedTenantId.value, selectedAppId.value)
    }
  }
)

onMounted(async () => {
  await loadTenants()
  if (!selectedTenantId.value) {
    return
  }
  await loadApps(selectedTenantId.value)
  if (selectedAppId.value) {
    await loadKeys(selectedTenantId.value, selectedAppId.value)
  }
})
</script>

<style scoped>
.tenant-shell {
  padding: 1rem;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
}

.context {
  display: flex;
  gap: 0.4rem;
}

.board-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.lane {
  padding: 0.85rem;
  display: flex;
  flex-direction: column;
  min-height: 560px;
}

.lane-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.lane-head h3 {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
}

.count-badge {
  min-width: 26px;
  text-align: center;
  padding: 0.1rem 0.45rem;
  border-radius: 999px;
  background: #ecf2ff;
  color: #3451b2;
  font-size: 0.76rem;
  font-weight: 700;
}

.lane-sub {
  margin: 0.4rem 0 0.7rem;
  color: var(--text-sub);
  font-size: 0.8rem;
}

.lane-empty {
  color: var(--text-sub);
  font-size: 0.84rem;
  border: 1px dashed #d7dfef;
  border-radius: 10px;
  padding: 0.75rem;
  background: #fbfcff;
}

.entity-grid {
  display: grid;
  gap: 0.65rem;
}

.entity-card {
  border: 1px solid #dfe6f3;
  border-radius: 12px;
  background: #fff;
  padding: 0.75rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.entity-card:hover {
  border-color: #afc0ea;
}

.entity-card.active {
  border-color: #5b7ce8;
  box-shadow: 0 0 0 2px rgba(91, 124, 232, 0.14);
}

.entity-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.entity-title {
  font-weight: 800;
  font-size: 0.93rem;
  line-height: 1.2;
}

.entity-meta {
  margin-top: 0.25rem;
  color: #6a7a9a;
  font-size: 0.78rem;
  word-break: break-all;
}

.entity-desc {
  margin-top: 0.25rem;
  color: var(--text-sub);
  font-size: 0.8rem;
}

.entity-actions {
  margin-top: 0.65rem;
  display: flex;
  justify-content: flex-end;
}

.state-pill {
  border-radius: 999px;
  padding: 0.12rem 0.42rem;
  font-size: 0.72rem;
  font-weight: 700;
}

.state-on {
  background: #e9f9f1;
  color: #1f8b5b;
}

.state-off {
  background: #f5f6f8;
  color: #7a8290;
}

.create-card {
  border-style: dashed;
  border-color: #c8d4ee;
  background: #fbfdff;
}

.create-title {
  font-size: 0.88rem;
  font-weight: 800;
  color: #3451b2;
}

.create-form {
  margin-top: 0.6rem;
  display: grid;
  gap: 0.5rem;
}

.create-form.one-line {
  grid-template-columns: 1fr auto;
}

.new-key {
  margin-bottom: 0.7rem;
  padding: 0.7rem;
}

.new-key-title {
  font-size: 0.82rem;
  color: var(--text-sub);
}

.new-key code {
  margin-top: 0.35rem;
  display: block;
  background: #102127;
  color: #d1fae5;
  padding: 0.4rem;
  border-radius: 8px;
  word-break: break-all;
}

.muted {
  opacity: 0.65;
}

@media (max-width: 1280px) {
  .board-grid {
    grid-template-columns: 1fr;
  }

  .lane {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .panel-head {
    flex-direction: column;
  }

  .create-form.one-line {
    grid-template-columns: 1fr;
  }
}
</style>
