<template>
  <div class="page">
    <header class="panel-head">
      <div>
        <h2 class="section-title">模型配置</h2>
        <p class="section-sub">在当前租户 / 应用作用域下管理模型池。</p>
      </div>
      <button class="btn btn-secondary" :disabled="!canLoad || loading" @click="loadModels">{{ loading ? '加载中...' : '刷新模型' }}</button>
    </header>

    <section v-if="!hasExternalScope" class="soft-card filter-card">
      <div class="row">
        <select v-model="selectedTenantId" class="select" @change="onTenantChange">
          <option value="">选择租户</option>
          <option v-for="tenant in tenants" :key="tenant.tenantId" :value="tenant.tenantId">
            {{ tenant.name }} ({{ tenant.tenantId }})
          </option>
        </select>
        <select v-model="selectedAppId" class="select" :disabled="!selectedTenantId" @change="onAppChange">
          <option value="">选择应用</option>
          <option v-for="app in apps" :key="app.appId" :value="app.appId">{{ app.appName }} ({{ app.appId }})</option>
        </select>
      </div>
    </section>

    <section v-if="canLoad" class="work-grid">
      <div class="soft-card list-card">
        <h3>模型列表</h3>
        <div v-if="loading" class="empty">加载中...</div>
        <div v-else-if="models.length === 0" class="empty">暂无模型配置，先在右侧创建一个模型。</div>
        <div v-else class="list">
          <article
            v-for="item in models"
            :key="item.modelName"
            class="list-item"
            :class="{ active: editing && form.modelName === item.modelName }"
            @click="edit(item)"
          >
            <div>
              <div class="item-title">{{ item.modelName }}</div>
              <div class="item-sub">{{ item.provider }} | {{ item.baseUrl || '未设置 Base URL' }}</div>
            </div>
            <span class="pill" :class="item.active ? 'pill-ok' : 'pill-off'">{{ item.active ? '启用' : '禁用' }}</span>
          </article>
        </div>
      </div>

      <div class="soft-card form-card">
        <h3>{{ editing ? '编辑模型' : '新增模型' }}</h3>
        <div class="field-grid">
          <input v-model="form.modelName" class="input" placeholder="modelName" :disabled="editing" />
          <input v-model="form.provider" class="input" placeholder="provider" />
          <input v-model="form.baseUrl" class="input" placeholder="baseUrl" />
          <label class="switch"><input type="checkbox" v-model="form.active" /> 启用模型</label>
          <textarea v-model="configText" class="textarea" rows="7" placeholder='配置 JSON，例如 {"defaultTemperature":0.7}'></textarea>
        </div>
        <div class="actions">
          <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存配置' }}</button>
          <button class="btn btn-secondary" @click="resetForm">重置</button>
        </div>
      </div>
    </section>

    <section v-else class="soft-card placeholder">请先选择租户和应用，再开始模型配置。</section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { controlPlaneApi, type AppView, type TenantModelConfig, type TenantView } from '@/services/controlPlaneApi'
import { toast } from '@/utils/toast'

interface Props {
  tenantId?: string
  appId?: string
}

const props = defineProps<Props>()

const tenants = ref<TenantView[]>([])
const apps = ref<AppView[]>([])
const models = ref<TenantModelConfig[]>([])
const selectedTenantId = ref('')
const selectedAppId = ref('')
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)

const form = ref({
  modelName: '',
  provider: '',
  baseUrl: '',
  active: true
})
const configText = ref('{}')
const canLoad = ref(false)
const hasExternalScope = computed(() => !!props.tenantId && !!props.appId)

async function loadTenants() {
  tenants.value = await controlPlaneApi.listTenants()
}

async function onTenantChange() {
  if (hasExternalScope.value) return
  selectedAppId.value = ''
  apps.value = []
  models.value = []
  canLoad.value = false
  resetForm()
  if (!selectedTenantId.value) return
  apps.value = await controlPlaneApi.listApps(selectedTenantId.value)
}

function onAppChange() {
  if (hasExternalScope.value) return
  models.value = []
  canLoad.value = !!selectedTenantId.value && !!selectedAppId.value
  resetForm()
  if (canLoad.value) {
    loadModels()
  }
}

async function loadModels() {
  if (!selectedTenantId.value || !selectedAppId.value) return
  loading.value = true
  try {
    models.value = await controlPlaneApi.listModelConfigs(selectedTenantId.value, selectedAppId.value)
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载模型失败')
  } finally {
    loading.value = false
  }
}

function edit(item: TenantModelConfig) {
  editing.value = true
  form.value = {
    modelName: item.modelName,
    provider: item.provider,
    baseUrl: item.baseUrl,
    active: item.active
  }
  configText.value = JSON.stringify(item.config ?? {}, null, 2)
}

function resetForm() {
  editing.value = false
  form.value = { modelName: '', provider: '', baseUrl: '', active: true }
  configText.value = '{}'
}

async function save() {
  if (!selectedTenantId.value || !selectedAppId.value) {
    toast.error('请先选择租户和应用')
    return
  }
  if (!form.value.modelName || !form.value.provider) {
    toast.error('modelName 和 provider 必填')
    return
  }

  let config: Record<string, unknown>
  try {
    config = JSON.parse(configText.value || '{}') as Record<string, unknown>
  } catch {
    toast.error('配置 JSON 格式错误')
    return
  }

  saving.value = true
  try {
    await controlPlaneApi.upsertModelConfig(selectedTenantId.value, selectedAppId.value, {
      modelName: form.value.modelName,
      provider: form.value.provider,
      baseUrl: form.value.baseUrl,
      active: form.value.active,
      config
    })
    toast.success('模型配置已保存')
    resetForm()
    await loadModels()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function applyExternalScope() {
  selectedTenantId.value = props.tenantId || ''
  selectedAppId.value = props.appId || ''
  canLoad.value = !!selectedTenantId.value && !!selectedAppId.value
}

onMounted(async () => {
  try {
    applyExternalScope()
    if (hasExternalScope.value) {
      if (canLoad.value) await loadModels()
      return
    }
    await loadTenants()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载租户失败')
  }
})

watch(
  () => [props.tenantId, props.appId],
  async () => {
    if (!hasExternalScope.value) return
    applyExternalScope()
    models.value = []
    resetForm()
    if (canLoad.value) {
      await loadModels()
    }
  }
)
</script>

<style scoped>
.page { padding: 1rem; }
.panel-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 0.8rem; }
.filter-card { margin-top: 0.9rem; padding: 0.8rem; }
.row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0.7rem; }
.work-grid { margin-top: 0.9rem; display: grid; grid-template-columns: 1fr 1.15fr; gap: 0.8rem; }
.list-card, .form-card { padding: 0.85rem; }
h3 { margin: 0; font-size: 0.96rem; font-weight: 800; }
.list, .field-grid { margin-top: 0.75rem; display: flex; flex-direction: column; gap: 0.55rem; }
.item-title { font-weight: 800; font-size: 0.9rem; }
.item-sub { margin-top: 0.2rem; font-size: 0.78rem; color: var(--text-sub); }
.switch { display: flex; align-items: center; gap: 0.45rem; color: var(--text-sub); }
.actions { display: flex; gap: 0.55rem; margin-top: 0.7rem; }
.placeholder { margin-top: 0.9rem; padding: 1rem; color: var(--text-sub); }
.empty { margin-top: 0.7rem; color: var(--text-sub); }
@media (max-width: 1100px) { .work-grid { grid-template-columns: 1fr; } }
@media (max-width: 760px) {
  .row { grid-template-columns: 1fr; }
  .panel-head { flex-direction: column; }
  .actions { flex-direction: column; }
}
</style>
