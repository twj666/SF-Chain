<template>
  <div class="tenant-shell">
    <header class="panel-head">
      <div>
        <h2 class="section-title">租户与密钥</h2>
        <p class="section-sub">左到右三步：选租户 -> 选应用 -> 管理 API Key。</p>
      </div>
      <div class="context" v-if="selectedTenantId">
        <span class="pill pill-ok">租户 {{ selectedTenantId }}</span>
        <span class="pill" :class="selectedAppId ? 'pill-ok' : 'pill-off'">应用 {{ selectedAppId || '未选择' }}</span>
      </div>
    </header>

    <div class="flow-grid">
      <section class="soft-card block">
        <h3>租户</h3>
        <div class="form-grid">
          <input v-model="tenantForm.tenantId" class="input" placeholder="tenantId（poet-prod）" />
          <input v-model="tenantForm.name" class="input" placeholder="租户名称" />
          <input v-model="tenantForm.description" class="input" placeholder="描述（可选）" />
          <button class="btn btn-primary" :disabled="creatingTenant" @click="createTenant">{{ creatingTenant ? '创建中...' : '创建租户' }}</button>
        </div>

        <div class="list-wrap" v-if="loadingTenants">租户加载中...</div>
        <div class="list-wrap" v-else-if="tenants.length === 0">暂无租户</div>
        <div class="list-wrap" v-else>
          <article
            v-for="tenant in tenants"
            :key="tenant.tenantId"
            class="list-item"
            :class="{ active: selectedTenantId === tenant.tenantId }"
            @click="selectTenant(tenant.tenantId)"
          >
            <div class="item-main">
              <div class="item-title">{{ tenant.name }}</div>
              <div class="item-sub">{{ tenant.tenantId }} · {{ tenant.description || '无描述' }}</div>
            </div>
            <button class="btn btn-secondary" @click.stop="toggleTenant(tenant)">{{ tenant.active ? '禁用' : '启用' }}</button>
          </article>
        </div>
      </section>

      <section class="soft-card block" :class="{ muted: !selectedTenantId }">
        <h3>应用</h3>
        <div class="form-grid">
          <input v-model="appForm.appId" class="input" placeholder="appId（poet-agent）" :disabled="!selectedTenantId" />
          <input v-model="appForm.appName" class="input" placeholder="应用名称" :disabled="!selectedTenantId" />
          <input v-model="appForm.description" class="input" placeholder="描述（可选）" :disabled="!selectedTenantId" />
          <button class="btn btn-primary" :disabled="creatingApp || !selectedTenantId" @click="createApp">{{ creatingApp ? '创建中...' : '创建应用' }}</button>
        </div>

        <div class="list-wrap" v-if="!selectedTenantId">请先选择租户</div>
        <div class="list-wrap" v-else-if="loadingApps">应用加载中...</div>
        <div class="list-wrap" v-else-if="apps.length === 0">暂无应用</div>
        <div class="list-wrap" v-else>
          <article
            v-for="app in apps"
            :key="app.appId"
            class="list-item"
            :class="{ active: selectedAppId === app.appId }"
            @click="selectApp(app.appId)"
          >
            <div class="item-main">
              <div class="item-title">{{ app.appName }}</div>
              <div class="item-sub">{{ app.appId }} · {{ app.description || '无描述' }}</div>
            </div>
            <button class="btn btn-secondary" @click.stop="toggleApp(app)">{{ app.active ? '禁用' : '启用' }}</button>
          </article>
        </div>
      </section>

      <section class="soft-card block" :class="{ muted: !selectedAppId }">
        <h3>API Key</h3>
        <div class="form-grid key-form">
          <input v-model="keyForm.keyName" class="input" placeholder="key 名称（prod-key）" :disabled="!selectedAppId" />
          <button class="btn btn-primary" :disabled="creatingKey || !selectedAppId" @click="createKey">{{ creatingKey ? '申请中...' : '申请 Key' }}</button>
        </div>

        <div v-if="createdKey" class="new-key soft-card">
          <div class="new-key-title">新 Key（仅展示一次）</div>
          <code>{{ createdKey }}</code>
        </div>

        <div class="list-wrap" v-if="!selectedAppId">请先选择应用</div>
        <div class="list-wrap" v-else-if="loadingKeys">Key 加载中...</div>
        <div class="list-wrap" v-else-if="keys.length === 0">暂无 Key</div>
        <div class="list-wrap" v-else>
          <article v-for="item in keys" :key="item.id" class="list-item key-item">
            <div class="item-main">
              <div class="item-title">{{ item.keyName }}</div>
              <div class="item-sub">{{ item.keyPrefix }} · {{ item.active ? '启用' : '禁用' }}</div>
            </div>
            <button class="btn btn-danger" :disabled="!item.active" @click="revoke(item.id)">吊销</button>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { controlPlaneApi, type ApiKeyView, type AppView, type TenantView } from '@/services/controlPlaneApi'
import { toast } from '@/utils/toast'

const tenants = ref<TenantView[]>([])
const apps = ref<AppView[]>([])
const keys = ref<ApiKeyView[]>([])
const selectedTenantId = ref('')
const selectedAppId = ref('')
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

async function loadTenants() {
  loadingTenants.value = true
  try {
    tenants.value = await controlPlaneApi.listTenants()
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
    await controlPlaneApi.createTenant(tenantForm.value)
    tenantForm.value = { tenantId: '', name: '', description: '' }
    toast.success('租户创建成功')
    await loadTenants()
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
    await controlPlaneApi.createApp(selectedTenantId.value, appForm.value)
    appForm.value = { appId: '', appName: '', description: '' }
    toast.success('应用创建成功')
    await loadApps(selectedTenantId.value)
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
  loadApps(tenantId)
}

function selectApp(appId: string) {
  if (!selectedTenantId.value) {
    return
  }
  selectedAppId.value = appId
  createdKey.value = ''
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

onMounted(loadTenants)
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

.flow-grid {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.block {
  padding: 0.85rem;
}

.block h3 {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 800;
}

.form-grid {
  margin-top: 0.7rem;
  display: grid;
  gap: 0.55rem;
  grid-template-columns: 1fr;
}

.key-form {
  grid-template-columns: 1fr auto;
}

.list-wrap {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  color: var(--text-sub);
}

.item-main {
  min-width: 0;
}

.item-title {
  font-weight: 800;
  font-size: 0.92rem;
}

.item-sub {
  margin-top: 0.25rem;
  color: var(--text-sub);
  font-size: 0.8rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.new-key {
  margin-top: 0.75rem;
  padding: 0.72rem;
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

@media (max-width: 1200px) {
  .flow-grid {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .key-form {
    grid-template-columns: 1fr;
  }
}
</style>
