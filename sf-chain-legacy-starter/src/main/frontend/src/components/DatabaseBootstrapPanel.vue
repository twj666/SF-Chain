<template>
  <div class="db-page">
    <div class="content-header">
      <h2>数据库初始化</h2>
      <p>在前端填写数据库信息，一键测试并初始化配置中心持久化表</p>
    </div>

    <div class="card">
      <div class="row">
        <select v-model="form.databaseType" class="form-input">
          <option value="postgresql">PostgreSQL</option>
          <option value="mysql">MySQL</option>
        </select>
        <input v-model="form.jdbcUrl" class="form-input" placeholder="jdbc url" />
      </div>
      <div class="row">
        <input v-model="form.username" class="form-input" placeholder="username" />
        <input v-model="form.password" class="form-input" type="password" placeholder="password" />
      </div>
      <div class="row">
        <button class="btn btn-secondary" :disabled="testing" @click="test">{{ testing ? '测试中...' : '测试连接' }}</button>
        <button class="btn btn-primary" :disabled="initializing" @click="initialize">{{ initializing ? '初始化中...' : '初始化数据库' }}</button>
      </div>
    </div>

    <div class="card" v-if="status">
      <h3>当前状态</h3>
      <p>已保存配置: {{ status.configSaved ? '是' : '否' }}</p>
      <p v-if="status.databaseType">数据库类型: {{ status.databaseType }}</p>
      <p v-if="status.jdbcUrl">JDBC URL: {{ status.jdbcUrl }}</p>
      <p v-if="status.savedAt">保存时间: {{ status.savedAt }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { toast } from '@/utils/toast'
import { bootstrapApi, type DatabaseBootstrapRequest, type DatabaseBootstrapStatus } from '@/services/bootstrapApi'

const testing = ref(false)
const initializing = ref(false)
const status = ref<DatabaseBootstrapStatus | null>(null)

const form = ref<DatabaseBootstrapRequest>({
  databaseType: 'postgresql',
  jdbcUrl: 'jdbc:postgresql://127.0.0.1:5432/sf_chain',
  username: 'postgres',
  password: 'postgres'
})

async function loadStatus() {
  try {
    status.value = await bootstrapApi.status()
  } catch (e) {
    const message = e instanceof Error ? e.message : '读取状态失败'
    toast.error(message)
  }
}

async function test() {
  testing.value = true
  try {
    await bootstrapApi.testConnection(form.value)
    toast.success('数据库连接成功')
  } catch (e) {
    const message = e instanceof Error ? e.message : '连接测试失败'
    toast.error(message)
  } finally {
    testing.value = false
  }
}

async function initialize() {
  initializing.value = true
  try {
    const result = await bootstrapApi.initDatabase(form.value)
    toast.success(result.message)
    if (result.restartRequired) {
      toast.warning('初始化成功，建议重启配置中心使新数据库配置完全生效')
    }
    await loadStatus()
  } catch (e) {
    const message = e instanceof Error ? e.message : '初始化失败'
    toast.error(message)
  } finally {
    initializing.value = false
  }
}

onMounted(loadStatus)
</script>

<style scoped>
.db-page { display: flex; flex-direction: column; gap: 1rem; }
.card { background: #fff; border-radius: 12px; padding: 1rem; border: 1px solid #e2e8f0; }
.row { display: flex; gap: 0.75rem; margin-top: 0.75rem; }
.form-input { flex: 1; padding: 0.65rem 0.75rem; border: 1px solid #cbd5e1; border-radius: 8px; }
.btn { border: none; border-radius: 8px; padding: 0.5rem 0.75rem; cursor: pointer; }
.btn-primary { background: #2563eb; color: #fff; }
.btn-secondary { background: #e2e8f0; color: #334155; }
@media (max-width: 768px) { .row { flex-direction: column; } }
</style>
