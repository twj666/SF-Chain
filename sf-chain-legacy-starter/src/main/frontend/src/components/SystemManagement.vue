<template>
  <div class="system-content">
    <div class="content-header">
      <h2>系统管理</h2>
      <p>系统配置备份、刷新和重置操作</p>
    </div>

    <div class="system-operations">
      <div class="operation-card">
        <div class="card-icon">
          <span>💾</span>
        </div>
        <div class="card-content">
          <h3>配置备份</h3>
          <p>创建当前系统配置的备份文件</p>
          <button @click="createBackup" class="btn btn-primary" :disabled="backing">
            <span v-if="backing" class="btn-loading"></span>
            <span>{{ backing ? '备份中...' : '创建备份' }}</span>
          </button>
        </div>
      </div>

      <div class="operation-card">
        <div class="card-icon">
          <span>🔄</span>
        </div>
        <div class="card-content">
          <h3>刷新配置</h3>
          <p>重新加载系统配置信息</p>
          <button @click="refreshSystem" class="btn btn-secondary" :disabled="refreshing">
            <span v-if="refreshing" class="btn-loading"></span>
            <span>{{ refreshing ? '刷新中...' : '刷新配置' }}</span>
          </button>
        </div>
      </div>

      <div class="operation-card">
        <div class="card-icon">
          <span>⚠️</span>
        </div>
        <div class="card-content">
          <h3>重置系统</h3>
          <p>将系统配置重置为默认状态</p>
          <button @click="resetSystem" class="btn btn-danger" :disabled="resetting">
            <span v-if="resetting" class="btn-loading"></span>
            <span>{{ resetting ? '重置中...' : '重置系统' }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import type { SystemOverview, ApiResponse } from '@/types/system'

// Props
interface Props {
  systemOverview?: SystemOverview
}

defineProps<Props>()

// Emits
const emit = defineEmits<{
  'update-overview': [overview: SystemOverview]
}>()

// 状态管理
const backing = ref(false)
const refreshing = ref(false)
const resetting = ref(false)

// API基础URL
const API_BASE = '/api/sf-chain'

// 创建备份
const createBackup = async () => {
  backing.value = true
  try {
    const response = await fetch(`${API_BASE}/system/backup`, {
      method: 'POST'
    })
    const result: ApiResponse<SystemOverview> = await response.json()
    if (result.success) {
      alert('系统配置备份创建成功')
      if (result.data) {
        emit('update-overview', result.data)
      }
    } else {
      throw new Error(result.message || '备份失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '创建备份时出错'
    alert('备份失败: ' + errorMessage)
  } finally {
    backing.value = false
  }
}

// 刷新系统配置
const refreshSystem = async () => {
  refreshing.value = true
  try {
    const response = await fetch(`${API_BASE}/system/refresh`, {
      method: 'POST'
    })
    const result: ApiResponse<SystemOverview> = await response.json()
    if (result.success) {
      alert('系统配置刷新成功')
      if (result.data) {
        emit('update-overview', result.data)
      }
    } else {
      throw new Error(result.message || '刷新失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '刷新系统配置时出错'
    alert('刷新失败: ' + errorMessage)
  } finally {
    refreshing.value = false
  }
}

// 重置系统
const resetSystem = async () => {
  if (!confirm('确定要重置系统配置吗？此操作不可撤销！')) {
    return
  }

  resetting.value = true
  try {
    const response = await fetch(`${API_BASE}/system/reset`, {
      method: 'POST'
    })
    const result: ApiResponse<SystemOverview> = await response.json()
    if (result.success) {
      alert('系统配置重置成功')
      if (result.data) {
        emit('update-overview', result.data)
      }
    } else {
      throw new Error(result.message || '重置失败')
    }
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '重置系统配置时出错'
    alert('重置失败: ' + errorMessage)
  } finally {
    resetting.value = false
  }
}
</script>

<style scoped>
.content-header {
  margin-bottom: 2rem;
}

.content-header h2 {
  font-size: 2rem;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 0.5rem 0;
}

.content-header p {
  font-size: 1.1rem;
  color: #718096;
  margin: 0;
}

/* 系统管理内容 */
.system-content .system-operations {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.operation-card {
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  padding: 2rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 1rem;
}

.operation-card .card-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  color: white;
}

.operation-card .card-content h3 {
  font-size: 1.25rem;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 0.5rem 0;
}

.operation-card .card-content p {
  font-size: 1rem;
  color: #718096;
  margin: 0 0 1rem 0;
}

/* 按钮样式 */
.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  font-size: 1rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #e2e8f0;
  color: #4a5568;
}

.btn-secondary:hover:not(:disabled) {
  background: #cbd5e0;
}

.btn-danger {
  background: #fed7d7;
  color: #742a2a;
}

.btn-danger:hover:not(:disabled) {
  background: #feb2b2;
}

.btn-loading {
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .system-operations {
    grid-template-columns: 1fr;
  }
}
</style>
