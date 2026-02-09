<template>
  <div class="db-shell">
    <header class="panel-head">
      <div>
        <h2 class="section-title">数据库控制台</h2>
        <p class="section-sub">配置连接、预检查、受保护初始化。防误操作默认开启。</p>
      </div>
      <div class="badge-group">
        <span class="pill" :class="status?.configSaved ? 'pill-ok' : 'pill-off'">{{ status?.configSaved ? '已保存配置' : '未保存配置' }}</span>
      </div>
    </header>

    <section class="soft-card block">
      <h3>1) 连接配置</h3>
      <div class="grid2">
        <select v-model="form.databaseType" class="select" @change="onDatabaseTypeChange">
          <option value="mysql">MySQL</option>
          <option value="postgresql">PostgreSQL</option>
        </select>
        <input v-model="form.jdbcUrl" class="input" placeholder="jdbc url" />
        <input v-model="form.username" class="input" placeholder="username" />
        <input v-model="form.password" class="input" type="password" placeholder="password" />
      </div>
      <div class="actions">
        <button class="btn btn-secondary" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存配置' }}</button>
        <button class="btn btn-secondary" :disabled="testing" @click="test">{{ testing ? '测试中...' : '测试连接' }}</button>
      </div>
    </section>

    <section class="soft-card block">
      <h3>2) 初始化保护</h3>
      <div class="grid2">
        <input v-model="confirmText" class="input" placeholder="输入 INIT 以确认初始化" />
        <label class="force-line"><input type="checkbox" v-model="forceInit" /> 空表强制初始化（force）</label>
      </div>
      <div class="actions">
        <button class="btn btn-secondary" :disabled="checking" @click="runPrecheck">{{ checking ? '检查中...' : '初始化前检查' }}</button>
        <button class="btn btn-primary" :disabled="initializing" @click="initialize">{{ initializing ? '初始化中...' : '初始化数据库' }}</button>
      </div>
      <p class="hint">规则：检测到非空表时禁止初始化；检测到已存在空表时必须勾选 force。</p>
    </section>

    <section class="info-grid">
      <article class="soft-card block">
        <h3>当前配置状态</h3>
        <p class="line">数据库类型: {{ status?.databaseType || '-' }}</p>
        <p class="line">JDBC URL: {{ status?.jdbcUrl || '-' }}</p>
        <p class="line">保存时间: {{ status?.savedAt || '-' }}</p>
      </article>

      <article class="soft-card block" v-if="precheck">
        <h3>预检查结果</h3>
        <p class="line">存在控制面表: {{ precheck.hasExistingTables ? '是' : '否' }}</p>
        <p class="line">存在非空表: {{ precheck.hasNonEmptyTables ? '是' : '否' }}</p>
        <p class="line">可直接初始化: {{ precheck.safeToInitialize ? '是' : '否' }}</p>
        <p class="line">是否需 force: {{ precheck.requiresForce ? '是' : '否' }}</p>
        <p class="line" v-if="precheck.existingTables.length">已存在表: {{ precheck.existingTables.join(', ') }}</p>
        <p class="line" v-if="precheck.nonEmptyTables.length">非空表: {{ precheck.nonEmptyTables.join(', ') }}</p>
      </article>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { toast } from '@/utils/toast'
import { bootstrapApi, type DatabaseBootstrapRequest, type DatabaseBootstrapStatus, type DatabasePrecheckResult } from '@/services/bootstrapApi'

const testing = ref(false)
const initializing = ref(false)
const saving = ref(false)
const checking = ref(false)
const status = ref<DatabaseBootstrapStatus | null>(null)
const precheck = ref<DatabasePrecheckResult | null>(null)
const confirmText = ref('')
const forceInit = ref(false)

const form = ref<DatabaseBootstrapRequest>({
  databaseType: 'mysql',
  jdbcUrl: 'jdbc:mysql://127.0.0.1:3306/sf_chain?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai',
  username: 'root',
  password: ''
})

async function loadStatus() {
  try {
    status.value = await bootstrapApi.status()
    if (status.value.configSaved && status.value.databaseType && status.value.jdbcUrl) {
      form.value.databaseType = status.value.databaseType as 'mysql' | 'postgresql'
      form.value.jdbcUrl = status.value.jdbcUrl
    }
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '读取状态失败')
  }
}

async function test() {
  testing.value = true
  try {
    await bootstrapApi.testConnection(form.value)
    toast.success('数据库连接成功')
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '连接测试失败')
  } finally {
    testing.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const result = await bootstrapApi.saveConfig(form.value)
    toast.success(result.message)
    if (result.restartRequired) {
      toast.warning('配置已保存，重启配置中心后自动生效')
    }
    await loadStatus()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '保存配置失败')
  } finally {
    saving.value = false
  }
}

async function runPrecheck() {
  checking.value = true
  try {
    const result = await bootstrapApi.precheck(form.value)
    precheck.value = result
    return result
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '预检查失败')
    return null
  } finally {
    checking.value = false
  }
}

async function initialize() {
  if (confirmText.value.trim().toUpperCase() !== 'INIT') {
    toast.error('请输入 INIT 确认初始化')
    return
  }

  const checkResult = await runPrecheck()
  if (!checkResult) {
    return
  }
  if (checkResult.hasNonEmptyTables) {
    toast.error('检测到已有业务数据，初始化已被禁止')
    return
  }
  if (checkResult.requiresForce && !forceInit.value) {
    toast.error('检测到已存在空表，请勾选 force 后再初始化')
    return
  }

  initializing.value = true
  try {
    const result = await bootstrapApi.initDatabase({ ...form.value, force: forceInit.value })
    if (!result.success) {
      toast.error(result.message)
      if (result.precheck) {
        precheck.value = result.precheck
      }
      return
    }
    toast.success('数据库初始化成功')
    if (result.restartRequired) {
      toast.warning('初始化成功，建议重启配置中心使数据库配置完全生效')
    }
    await loadStatus()
    await runPrecheck()
  } catch (e) {
    toast.error(e instanceof Error ? e.message : '初始化失败')
  } finally {
    initializing.value = false
  }
}

function onDatabaseTypeChange() {
  if (form.value.databaseType === 'mysql') {
    form.value.jdbcUrl = 'jdbc:mysql://127.0.0.1:3306/sf_chain?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
    form.value.username = 'root'
    return
  }
  form.value.jdbcUrl = 'jdbc:postgresql://127.0.0.1:5432/sf_chain'
  form.value.username = 'postgres'
}

onMounted(loadStatus)
</script>

<style scoped>
.db-shell {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.8rem;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.8rem;
}

.block {
  padding: 0.85rem;
}

h3 {
  margin: 0;
  font-size: 0.98rem;
  font-weight: 800;
}

.grid2 {
  margin-top: 0.75rem;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
}

.actions {
  margin-top: 0.7rem;
  display: flex;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.8rem;
}

.line {
  margin: 0.45rem 0 0;
  color: var(--text-sub);
  font-size: 0.9rem;
}

.force-line {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  color: var(--text-sub);
}

.hint {
  margin-top: 0.55rem;
  color: #9a3412;
  font-size: 0.84rem;
}

@media (max-width: 960px) {
  .info-grid,
  .grid2 {
    grid-template-columns: 1fr;
  }

  .panel-head {
    flex-direction: column;
  }
}
</style>
