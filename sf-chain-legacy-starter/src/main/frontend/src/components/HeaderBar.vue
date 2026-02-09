<template>
  <header class="header page-shell">
    <div class="brand">
      <div class="logo-dot"></div>
      <div>
        <h1>SF-Chain Console</h1>
        <p>Remote Config Center</p>
      </div>
    </div>

    <div class="auth-zone">
      <div class="status" :class="currentToken ? 'ok' : 'off'">
        <span class="status-dot"></span>
        <span>{{ currentToken ? '已认证' : '未认证' }}</span>
      </div>

      <div class="token-wrap">
        <input
          v-model="tokenInput"
          type="password"
          class="input"
          placeholder="输入访问 Token"
          @keyup.enter="saveToken"
        />
        <button class="btn btn-primary" :disabled="!tokenInput.trim()" @click="saveToken">保存</button>
        <button class="btn btn-secondary" :disabled="!currentToken" @click="refreshData">刷新</button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getAuthToken, setAuthToken } from '@/services/apiUtils'
import { toast } from '@/utils/toast'

interface Props {
  onRefresh?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  onRefresh: () => {}
})

const tokenInput = ref('')
const currentToken = ref(getAuthToken())

const saveToken = () => {
  const token = tokenInput.value.trim()
  if (!token) {
    toast.error({ title: '输入错误', message: '请输入有效 Token', duration: 2600 })
    return
  }

  setAuthToken(token)
  currentToken.value = token
  tokenInput.value = ''
  toast.success({ title: '保存成功', message: 'Token 已生效', duration: 2200 })
  refreshData()
}

const refreshData = async () => {
  if (!currentToken.value) {
    toast.error({ title: '未认证', message: '请先设置 Token', duration: 2500 })
    return
  }
  await props.onRefresh()
}
</script>

<style scoped>
.header {
  padding: 0.9rem 1rem;
  margin-bottom: 0.95rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-dot {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #0f766e 0%, #0891b2 100%);
  box-shadow: 0 8px 16px rgba(8, 145, 178, 0.3);
}

.brand h1 {
  margin: 0;
  line-height: 1.1;
  font-size: 1.05rem;
  font-weight: 800;
}

.brand p {
  margin: 0.22rem 0 0;
  color: var(--text-sub);
  font-size: 0.8rem;
}

.auth-zone {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}

.status {
  height: 38px;
  border-radius: 999px;
  padding: 0 0.7rem;
  border: 1px solid var(--line);
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  font-size: 0.85rem;
  font-weight: 700;
}

.status.ok {
  background: #ecfdf5;
  color: #166534;
  border-color: #bbf7d0;
}

.status.off {
  background: #fef2f2;
  color: #7f1d1d;
  border-color: #fecaca;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: currentColor;
}

.token-wrap {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto auto;
  gap: 0.45rem;
  align-items: center;
}

@media (max-width: 860px) {
  .header {
    flex-direction: column;
    align-items: stretch;
  }

  .auth-zone {
    flex-direction: column;
    align-items: stretch;
  }

  .token-wrap {
    grid-template-columns: 1fr;
  }
}
</style>
