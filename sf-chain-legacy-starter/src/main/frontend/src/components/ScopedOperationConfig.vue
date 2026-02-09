<template>
  <div class="page">
    <header class="panel-head">
      <div>
        <h2 class="section-title">Operation 配置</h2>
        <p class="section-sub">将 Operation 映射到模型，并为每个节点配置扩展参数。</p>
      </div>
      <button class="btn btn-secondary" :disabled="!canLoad || loading" @click="loadData">{{ loading ? '加载中...' : '刷新配置' }}</button>
    </header>

    <section class="soft-card filter-card">
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
        <h3>Operation 列表</h3>
        <div v-if="loading" class="empty">加载中...</div>
        <div v-else-if="operations.length === 0" class="empty">暂无 Operation 配置，先在右侧新增</div>
        <div v-else class="list">
          <article
            v-for="item in operations"
            :key="item.operationType"
            class="list-item"
            :class="{ active: editing && form.operationType === item.operationType }"
            @click="edit(item)"
          >
            <div>
              <div class="item-title">{{ item.operationType }}</div>
              <div class="item-sub">{{ item.modelName || '未绑定模型' }}</div>
            </div>
            <span class="pill" :class="item.active ? 'pill-ok' : 'pill-off'">{{ item.active ? '启用' : '禁用' }}</span>
          </article>
        </div>
      </div>

      <div class="soft-card form-card">
        <h3>{{ editing ? '编辑 Operation' : '新增 Operation' }}</h3>
        <div class="field-grid">
          <input v-model="form.operationType" class="input" placeholder="operationType" :disabled="editing" />
          <select v-model="form.modelName" class="select">
            <option value="">选择模型（可选）</option>
            <option v-for="model in models" :key="model.modelName" :value="model.modelName">{{ model.modelName }}</option>
          </select>
          <label class="switch"><input type="checkbox" v-model="form.active" /> 启用 Operation</label>
          <textarea v-model="configText" class="textarea" rows="7" placeholder='扩展配置 JSON，例如 {"temperature":0.7}'></textarea>
        </div>
        <div class="actions">
          <button class="btn btn-primary" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存配置' }}</button>
          <button class="btn btn-secondary" @click="resetForm">重置</button>
        </div>
      </div>
    </section>

    <section v-else class="soft-card placeholder">
      先选择租户和应用，再开始配置 Operation。
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { controlPlaneApi, type AppView, type TenantModelConfig, type TenantOperationConfig, type TenantView } from '@/services/controlPlaneApi'
import { toast } from '@/utils/toast'

const tenants = ref<TenantView[]>([])
const apps = ref<AppView[]>([])
const models = ref<TenantModelConfig[]>([])
const operations = ref<TenantOperationConfig[]>([])
const selectedTenantId = ref('')
const selectedAppId = ref('')
const loading = ref(false)
const saving = ref(false)
const editing = ref(false)

const form = ref({
  operationType: '',
  modelName: '',
  active: true
})
const configText = ref('{}')
const canLoad = ref(false)

async function loadTenants() {
  tenants.value = await controlPlaneApi.listTenants()
}

async function onTenantChange() {
  selectedAppId.value = ''
  apps.value = []
  models.value = []
  operations.value = []
  canLoad.value = false
  resetForm()
  if (!selectedTenantId.value) {
    return
  }
  apps.value = await controlPlaneApi.listApps(selectedTenantId.value)
}

function onAppChange() {
  operations.value = []
  models.value = []
  canLoad.value = !!selectedTenantId.value && !!selectedAppId.value
  resetForm()
  if (canLoad.value) {
    loadData()
  }
}

async function loadData() {
  if (!selectedTenantId.value || !selectedAppId.value) {
    return
  }
  loading.value = true
  try {
    const [modelData, operationData] = await Promise.all([
      controlPlaneApi.listModelConfigs(selectedTenantId.value, selectedAppId.value),
      controlPlaneApi.listOperationConfigs(selectedTenantId.value, selectedAppId.value)
    ])
    models.value = modelData
    operations.value = operationData
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载配置失败')
  } finally {
    loading.value = false
  }
}

function edit(item: TenantOperationConfig) {
  editing.value = true
  form.value = {
    operationType: item.operationType,
    modelName: item.modelName,
    active: item.active
  }
  configText.value = JSON.stringify(item.config ?? {}, null, 2)
}

function resetForm() {
  editing.value = false
  form.value = { operationType: '', modelName: '', active: true }
  configText.value = '{}'
}

async function save() {
  if (!selectedTenantId.value || !selectedAppId.value) {
    toast.error('请先选择租户和应用')
    return
  }
  if (!form.value.operationType) {
    toast.error('operationType 必填')
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
    await controlPlaneApi.upsertOperationConfig(selectedTenantId.value, selectedAppId.value, {
      operationType: form.value.operationType,
      modelName: form.value.modelName || undefined,
      active: form.value.active,
      config
    })
    toast.success('Operation 配置已保存')
    resetForm()
    await loadData()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    await loadTenants()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '加载租户失败')
  }
})
</script>

<style scoped>
.page {
  padding: 1rem;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
}

.filter-card {
  margin-top: 0.9rem;
  padding: 0.8rem;
}

.row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.7rem;
}

.work-grid {
  margin-top: 0.9rem;
  display: grid;
  grid-template-columns: 1fr 1.15fr;
  gap: 0.8rem;
}

.list-card,
.form-card {
  padding: 0.85rem;
}

h3 {
  margin: 0;
  font-size: 0.96rem;
  font-weight: 800;
}

.list,
.field-grid {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}

.item-title {
  font-weight: 800;
  font-size: 0.9rem;
}

.item-sub {
  margin-top: 0.2rem;
  font-size: 0.78rem;
  color: var(--text-sub);
}

.switch {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--text-sub);
}

.actions {
  display: flex;
  gap: 0.55rem;
  margin-top: 0.7rem;
}

.placeholder {
  margin-top: 0.9rem;
  padding: 1rem;
  color: var(--text-sub);
}

.empty {
  margin-top: 0.7rem;
  color: var(--text-sub);
}

@media (max-width: 1100px) {
  .work-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .row {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
