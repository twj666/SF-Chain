<template>
  <div class="tenant-page">
    <div class="content-header">
      <h2>租户与密钥</h2>
      <p>创建租户、申请 API Key、吊销密钥</p>
    </div>

    <div class="card create-card">
      <h3>创建租户</h3>
      <div class="row">
        <input v-model="tenantForm.tenantId" placeholder="tenantId（如 poet-prod）" class="form-input" />
        <input v-model="tenantForm.name" placeholder="租户名称" class="form-input" />
      </div>
      <div class="row">
        <input v-model="tenantForm.description" placeholder="描述（可选）" class="form-input" />
        <button class="btn btn-primary" :disabled="creatingTenant" @click="createTenant">{{ creatingTenant ? '创建中...' : '创建租户' }}</button>
      </div>
    </div>

    <div class="card list-card">
      <h3>租户列表</h3>
      <div v-if="loadingTenants" class="empty">加载中...</div>
      <div v-else-if="tenants.length === 0" class="empty">暂无租户</div>
      <div v-else class="tenant-list">
        <div
          v-for="tenant in tenants"
          :key="tenant.tenantId"
          class="tenant-item"
          :class="{ active: selectedTenantId === tenant.tenantId }"
          @click="selectTenant(tenant.tenantId)"
        >
          <div>
            <div class="tenant-title">{{ tenant.name }} <span class="tenant-id">({{ tenant.tenantId }})</span></div>
            <div class="tenant-desc">{{ tenant.description || '无描述' }}</div>
          </div>
          <button class="btn btn-secondary" @click.stop="toggleTenant(tenant)">{{ tenant.active ? '禁用' : '启用' }}</button>
        </div>
      </div>
    </div>

    <div v-if="selectedTenantId" class="card app-card">
      <h3>应用管理（{{ selectedTenantId }}）</h3>
      <div class="row">
        <input v-model="appForm.appId" placeholder="appId（如 poet-agent）" class="form-input" />
        <input v-model="appForm.appName" placeholder="应用名称" class="form-input" />
      </div>
      <div class="row">
        <input v-model="appForm.description" placeholder="描述（可选）" class="form-input" />
        <button class="btn btn-primary" :disabled="creatingApp" @click="createApp">{{ creatingApp ? '创建中...' : '创建应用' }}</button>
      </div>
      <div v-if="loadingApps" class="empty">应用加载中...</div>
      <div v-else-if="apps.length === 0" class="empty">当前租户暂无应用，请先创建应用</div>
      <div v-else class="tenant-list">
        <div
          v-for="app in apps"
          :key="app.appId"
          class="tenant-item"
          :class="{ active: selectedAppId === app.appId }"
          @click="selectApp(app.appId)"
        >
          <div>
            <div class="tenant-title">{{ app.appName }} <span class="tenant-id">({{ app.appId }})</span></div>
            <div class="tenant-desc">{{ app.description || '无描述' }}</div>
          </div>
          <button class="btn btn-secondary" @click.stop="toggleApp(app)">{{ app.active ? '禁用' : '启用' }}</button>
        </div>
      </div>
    </div>

    <div v-if="selectedTenantId && selectedAppId" class="card key-card">
      <h3>申请 API Key（{{ selectedTenantId }} / {{ selectedAppId }}）</h3>
      <div class="row">
        <input v-model="keyForm.keyName" placeholder="key 名称（如 prod-key）" class="form-input" />
        <button class="btn btn-primary" :disabled="creatingKey" @click="createKey">{{ creatingKey ? '申请中...' : '申请 Key' }}</button>
      </div>
      <div v-if="createdKey" class="key-result">新 Key：<code>{{ createdKey }}</code></div>

      <h3>Key 列表</h3>
      <div v-if="loadingKeys" class="empty">加载中...</div>
      <div v-else-if="keys.length === 0" class="empty">当前租户暂无 key</div>
      <table v-else class="key-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>App</th>
            <th>名称</th>
            <th>前缀</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in keys" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.appId }}</td>
            <td>{{ item.keyName }}</td>
            <td><code>{{ item.keyPrefix }}</code></td>
            <td>{{ item.active ? '启用' : '禁用' }}</td>
            <td>
              <button class="btn btn-danger" :disabled="!item.active" @click="revoke(item.id)">吊销</button>
            </td>
          </tr>
        </tbody>
      </table>
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
    const message = e instanceof Error ? e.message : '加载租户失败'
    toast.error(message)
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
    toast.success('租户创建成功')
    tenantForm.value = { tenantId: '', name: '', description: '' }
    await loadTenants()
  } catch (e) {
    const message = e instanceof Error ? e.message : '创建租户失败'
    toast.error(message)
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
    const message = e instanceof Error ? e.message : '更新租户失败'
    toast.error(message)
  }
}

async function loadApps(tenantId: string) {
  loadingApps.value = true
  try {
    apps.value = await controlPlaneApi.listApps(tenantId)
  } catch (e) {
    const message = e instanceof Error ? e.message : '加载应用失败'
    toast.error(message)
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
    toast.success('应用创建成功')
    appForm.value = { appId: '', appName: '', description: '' }
    await loadApps(selectedTenantId.value)
  } catch (e) {
    const message = e instanceof Error ? e.message : '创建应用失败'
    toast.error(message)
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
    const message = e instanceof Error ? e.message : '更新应用失败'
    toast.error(message)
  }
}

async function loadKeys(tenantId: string, appId: string) {
  loadingKeys.value = true
  try {
    keys.value = await controlPlaneApi.listApiKeys(tenantId, appId)
  } catch (e) {
    const message = e instanceof Error ? e.message : '加载 key 失败'
    toast.error(message)
  } finally {
    loadingKeys.value = false
  }
}

function selectTenant(tenantId: string) {
  selectedTenantId.value = tenantId
  selectedAppId.value = ''
  apps.value = []
  createdKey.value = ''
  keys.value = []
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
  if (!selectedTenantId.value) {
    toast.error('请先选择租户')
    return
  }
  if (!selectedAppId.value) {
    toast.error('请先选择应用')
    return
  }
  if (!keyForm.value.keyName) {
    toast.error('keyName 必填')
    return
  }
  creatingKey.value = true
  try {
    const result = await controlPlaneApi.createApiKey(selectedTenantId.value, { appId: selectedAppId.value, keyName: keyForm.value.keyName })
    createdKey.value = result.apiKey
    keyForm.value = { keyName: '' }
    toast.success('API Key 申请成功')
    await loadKeys(selectedTenantId.value, selectedAppId.value)
  } catch (e) {
    const message = e instanceof Error ? e.message : '申请 key 失败'
    toast.error(message)
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
    const message = e instanceof Error ? e.message : '吊销失败'
    toast.error(message)
  }
}

onMounted(loadTenants)
</script>

<style scoped>
.tenant-page { display: flex; flex-direction: column; gap: 1rem; }
.card { background: #fff; border-radius: 12px; padding: 1rem; border: 1px solid #e2e8f0; }
.row { display: flex; gap: 0.75rem; margin-top: 0.75rem; }
.form-input { flex: 1; padding: 0.65rem 0.75rem; border: 1px solid #cbd5e1; border-radius: 8px; }
.tenant-list { display: flex; flex-direction: column; gap: 0.5rem; margin-top: 0.75rem; }
.tenant-item { display: flex; justify-content: space-between; align-items: center; border: 1px solid #e2e8f0; border-radius: 10px; padding: 0.75rem; cursor: pointer; }
.tenant-item.active { border-color: #3b82f6; background: #eff6ff; }
.tenant-title { font-weight: 600; }
.tenant-id { color: #64748b; font-weight: 400; }
.tenant-desc { font-size: 0.9rem; color: #64748b; }
.key-table { width: 100%; border-collapse: collapse; margin-top: 0.75rem; }
.key-table th, .key-table td { border-bottom: 1px solid #e2e8f0; padding: 0.55rem; text-align: left; }
.key-result { margin-top: 0.75rem; background: #f8fafc; padding: 0.65rem; border-radius: 8px; word-break: break-all; }
.empty { color: #64748b; margin-top: 0.75rem; }
.btn { border: none; border-radius: 8px; padding: 0.5rem 0.75rem; cursor: pointer; }
.btn-primary { background: #2563eb; color: #fff; }
.btn-secondary { background: #e2e8f0; color: #334155; }
.btn-danger { background: #ef4444; color: #fff; }
@media (max-width: 768px) { .row { flex-direction: column; } }
</style>
