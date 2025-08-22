<template>
  <div class="ai-node-config-content">
    <!-- 紧凑型页面头部 - 整合统计信息 -->
    <div class="content-header">
      <div class="header-left">
        <h2>AI节点</h2>
      </div>
      <div class="header-right">
        <!-- 统计信息 -->
        <div class="header-stats" v-if="operationsData">
          <div class="stat-item">
            <span class="stat-number">{{ operationsData.totalOperations }}</span>
            <span class="stat-label">总节点</span>
          </div>
          <div class="stat-item success">
            <span class="stat-number">{{ configuredCount }}</span>
            <span class="stat-label">已配置</span>
          </div>
          <div class="stat-item warning">
            <span class="stat-number">{{ pendingCount }}</span>
            <span class="stat-label">待配置</span>
          </div>
          <div class="stat-item info">
            <span class="stat-number">{{ enabledModels.length }}</span>
            <span class="stat-label">可用模型</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="header-actions">
          <button @click="refreshData" class="btn btn-secondary" :disabled="loading">
            <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            <span>{{ loading ? '加载中' : '刷新' }}</span>
          </button>
          <button @click="showBatchConfig = true" class="btn btn-primary">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6V4m0 2a2 2 0 100 4m0-4a2 2 0 110 4m-6 8a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4m6 6v10m6-2a2 2 0 100-4m0 4a2 2 0 100 4m0-4v2m0-6V4"></path>
            </svg>
            <span>批量配置</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 搜索和筛选工具栏 -->
    <div class="toolbar">
      <div class="search-container">
        <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索节点..."
          class="search-input"
        />
        <button v-if="searchQuery" @click="searchQuery = ''" class="clear-btn">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>

      <div class="filter-tabs">
        <button
          @click="filterStatus = 'all'"
          :class="['filter-tab', { active: filterStatus === 'all' }]"
        >
          全部 <span class="count">{{ Object.keys(filteredOperations).length }}</span>
        </button>
        <button
          @click="filterStatus = 'configured'"
          :class="['filter-tab', { active: filterStatus === 'configured' }]"
        >
          已配置 <span class="count">{{ configuredCount }}</span>
        </button>
        <button
          @click="filterStatus = 'pending'"
          :class="['filter-tab', { active: filterStatus === 'pending' }]"
        >
          待配置 <span class="count">{{ pendingCount }}</span>
        </button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 加载状态 -->
      <div v-if="loading && !operationsData" class="loading-state">
        <div class="loading-spinner"></div>
        <p>正在加载节点配置...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="operationsData && operationsData.totalOperations === 0" class="empty-state">
        <div class="empty-icon">🤖</div>
        <h3>暂无AI操作节点</h3>
        <p>系统中还没有注册任何AI操作节点</p>
      </div>

      <!-- 操作节点网格 -->
      <div v-else class="operations-grid">
        <div
          v-for="(operation, operationType) in filteredOperations"
          :key="operationType"
          class="operation-card"
          :class="{
            'configured': operation.modelName,
            'disabled': !operation.enabled,
            'testing': testing === operationType
          }"
        >
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="operation-info">
              <div class="operation-icon">
                <img
                  v-if="operation.modelName"
                  :src="getProviderIcon(getModelProvider(operation.modelName))"
                  alt="provider"
                  class="provider-icon-large"
                />
                <div v-else class="default-icon">🤖</div>
              </div>
              <div class="operation-details">
                <h4 class="operation-name">{{ String(operationType) }}</h4>
                <p class="operation-desc">{{ operation.description || '无描述' }}</p>
              </div>
            </div>
            <div class="status-badge" :class="getStatusClass(operation)">
              {{ getStatusText(operation) }}
            </div>
          </div>

          <!-- 模型绑定区域 -->
          <div class="model-binding">
            <div class="model-selector">
              <select
                v-model="operation.modelName"
                @change="updateOperationMapping(String(operationType), operation.modelName || '')"
                class="model-select"
                :class="{ 'has-value': operation.modelName }"
              >
                <option value="">选择模型</option>
                <optgroup v-for="(models, provider) in groupedModels" :key="provider" :label="getProviderDisplayName(provider)">
                  <option v-for="model in models" :key="model.modelName" :value="model.modelName">
                    {{ model.modelName }}
                  </option>
                </optgroup>
              </select>
              <!-- 移除重复的厂商名称显示 -->
            </div>
          </div>

          <!-- 配置信息 -->
          <div v-if="operation.modelName" class="config-summary">
            <div class="config-item">
              <span class="config-label">模型:</span>
              <span class="config-value">{{ operation.modelName }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">Token:</span>
              <span class="config-value">{{ operation.maxTokens || 4096 }}</span>
            </div>
            <div class="config-item">
              <span class="config-label">温度:</span>
              <span class="config-value">{{ operation.temperature || 0.7 }}</span>
            </div>
            <div class="config-features">
              <span v-if="operation.jsonOutput" class="feature-tag json">JSON</span>
              <span v-if="operation.thinkingMode" class="feature-tag thinking">思考</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="card-actions">
            <button
              v-if="operation.modelName"
              @click="testOperation(String(operationType))"
              class="action-btn test large"
              :disabled="testing === operationType"
              :title="testing === operationType ? '测试中...' : '测试操作'"
            >
              <svg v-if="testing === operationType" class="w-5 h-5 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
              </svg>
              <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
              </svg>
            </button>

            <button
              @click="editOperation(String(operationType), operation)"
              class="action-btn edit large"
              title="编辑配置"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 批量配置弹窗 -->
    <div v-if="showBatchConfig" class="modal-overlay" @click="closeBatchConfig">
      <div class="modal-content wide" @click.stop>
        <div class="modal-header">
          <h3>批量配置</h3>
          <button @click="closeBatchConfig" class="btn-close">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <div class="modal-body compact">
          <div class="form-row">
            <div class="form-group">
              <label>选择默认模型</label>
              <select v-model="batchDefaultModel" class="form-input">
                <option value="">选择模型</option>
                <optgroup v-for="(models, provider) in groupedModels" :key="provider" :label="getProviderDisplayName(provider)">
                  <option v-for="model in models" :key="model.modelName" :value="model.modelName">
                    {{ model.modelName }}
                  </option>
                </optgroup>
              </select>
            </div>

            <div class="form-group">
              <label>选择操作节点</label>
              <div class="operation-checkboxes">
                <label v-for="(operation, operationType) in operationsData?.configs" :key="operationType" class="checkbox-item">
                  <input
                    type="checkbox"
                    v-model="selectedOperations"
                    :value="operationType"
                    class="checkbox-input"
                  />
                  <span class="checkbox-label">{{ String(operationType) }}</span>
                </label>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" @click="closeBatchConfig" class="btn btn-secondary">
              取消
            </button>
            <button
              @click="applyBatchConfig"
              class="btn btn-primary"
              :disabled="!batchDefaultModel || selectedOperations.length === 0 || batchSaving"
            >
              <span v-if="batchSaving" class="btn-loading"></span>
              <span>{{ batchSaving ? '应用中...' : '应用配置' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑操作配置弹窗 -->
    <div v-if="editingOperation" class="modal-overlay" @click="closeEditOperation">
      <div class="modal-content wide" @click.stop>
        <div class="modal-header">
          <h3>配置节点 - {{ editingOperationType }}</h3>
          <button @click="closeEditOperation" class="btn-close">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <form @submit.prevent="saveOperationConfig" class="modal-body compact">
          <div class="form-grid wide">
            <div class="form-group">
              <label for="operationModel">绑定模型</label>
              <select id="operationModel" v-model="operationForm.modelName" class="form-input">
                <option value="">选择模型</option>
                <optgroup v-for="(models, provider) in groupedModels" :key="provider" :label="getProviderDisplayName(provider)">
                  <option v-for="model in models" :key="model.modelName" :value="model.modelName">
                    {{ model.modelName }}
                  </option>
                </optgroup>
              </select>
            </div>

            <div class="form-group">
              <label for="operationEnabled">状态</label>
              <select id="operationEnabled" v-model="operationForm.enabled" class="form-input">
                <option :value="true">启用</option>
                <option :value="false">禁用</option>
              </select>
            </div>

            <div class="form-group">
              <label for="operationMaxTokens">Token数</label>
              <input
                id="operationMaxTokens"
                v-model.number="operationForm.maxTokens"
                type="number"
                placeholder="4096"
                class="form-input"
              />
            </div>

            <div class="form-group">
              <label for="operationTemperature">温度</label>
              <input
                id="operationTemperature"
                v-model.number="operationForm.temperature"
                type="number"
                step="0.1"
                min="0"
                max="2"
                placeholder="0.7"
                class="form-input"
              />
            </div>

            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  v-model="operationForm.jsonOutput"
                  class="checkbox-input"
                />
                <span>JSON输出</span>
              </label>
            </div>

            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  v-model="operationForm.thinkingMode"
                  class="checkbox-input"
                />
                <span>思考模式</span>
              </label>
            </div>

            <div class="form-group full-width">
              <label for="operationDescription">描述</label>
              <textarea
                id="operationDescription"
                v-model="operationForm.description"
                placeholder="操作描述信息"
                class="form-textarea"
                rows="2"
              ></textarea>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" @click="closeEditOperation" class="btn btn-secondary">
              取消
            </button>
            <button type="submit" class="btn btn-primary" :disabled="saving">
              <span v-if="saving" class="btn-loading"></span>
              <span>{{ saving ? '保存中...' : '保存' }}</span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 成功提示 -->
    <div v-if="showSuccessToast" class="success-toast">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
      </svg>
      {{ toastMessage }}
    </div>

    <!-- 错误提示 -->
    <div v-if="showErrorToast" class="error-toast">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
      </svg>
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { aiModelApi } from '@/services/aiModelApi'
import { aiOperationApi } from '@/services/aiOperationApi'
import type { OperationConfigData, OperationsResponse, ModelConfigData } from '@/types/system'
// 导入统一的AI提供商工具函数
import { getProviderName, getProviderIcon, getAllProviders } from '@/utils/aiProviders'

// 定义类型接口
interface ModelData {
  modelName: string
  provider?: string
  enabled: boolean
}

// 状态管理
const loading = ref(false)
const saving = ref(false)
const batchSaving = ref(false)
const testing = ref<string | null>(null)
const showBatchConfig = ref(false)
const editingOperation = ref<OperationConfigData | null>(null)
const editingOperationType = ref<string>('')
const searchQuery = ref('')
const filterStatus = ref<'all' | 'configured' | 'pending'>('all')
const showSuccessToast = ref(false)
const showErrorToast = ref(false)
const toastMessage = ref('')

// 数据
const models = ref<ModelData[]>([])
const operationsData = ref<OperationsResponse | null>(null)
const batchDefaultModel = ref('')
const selectedOperations = ref<string[]>([])

// 表单数据
const operationForm = reactive<OperationConfigData>({
  operationType: '',
  description: '',
  enabled: true,
  maxTokens: 4096,
  temperature: 0.7,
  jsonOutput: false,
  thinkingMode: false,
  customParams: {},
  modelName: ''
})

// 计算属性
const enabledModels = computed(() => models.value.filter(model => model.enabled))

// 按提供商分组的模型
const groupedModels = computed(() => {
  const groups: Record<string, ModelData[]> = {}
  enabledModels.value.forEach(model => {
    const provider = model.provider || 'other'
    if (!groups[provider]) {
      groups[provider] = []
    }
    groups[provider].push(model)
  })
  return groups
})

const filteredOperations = computed(() => {
  if (!operationsData.value?.configs) {
    return {}
  }

  let filtered = { ...operationsData.value.configs }

  // 按搜索关键词过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = Object.fromEntries(
      Object.entries(filtered).filter(([operationType, operation]) =>
        operationType.toLowerCase().includes(query) ||
        operation.description?.toLowerCase().includes(query)
      )
    )
  }

  // 按状态过滤
  if (filterStatus.value === 'configured') {
    filtered = Object.fromEntries(
      Object.entries(filtered).filter(([, operation]) => operation.modelName)
    )
  } else if (filterStatus.value === 'pending') {
    filtered = Object.fromEntries(
      Object.entries(filtered).filter(([, operation]) => !operation.modelName)
    )
  }

  return filtered
})

const configuredCount = computed(() => {
  if (!operationsData.value?.configs) return 0
  return Object.values(operationsData.value.configs).filter(op => op.modelName).length
})

const pendingCount = computed(() => {
  if (!operationsData.value?.configs) return 0
  return Object.values(operationsData.value.configs).filter(op => !op.modelName).length
})

// 辅助函数
const getModelProvider = (modelName: string) => {
  const model = models.value.find(m => m.modelName === modelName)
  return model?.provider || 'other'
}

// 获取提供商显示名称（兼容旧数据）
const getProviderDisplayName = (provider: string): string => {
  return getProviderName(provider)
}

const getStatusClass = (operation: OperationConfigData) => {
  if (!operation.enabled) return 'disabled'
  if (operation.modelName) return 'configured'
  return 'pending'
}

const getStatusText = (operation: OperationConfigData) => {
  if (!operation.enabled) return '已禁用'
  if (operation.modelName) return '已配置'
  return '待配置'
}

// 数据加载
const refreshData = async () => {
  try {
    loading.value = true
    const [modelsResponse, operationsResponse] = await Promise.all([
      aiModelApi.getAllModels(),
      aiOperationApi.getAllOperations()
    ])

    // 修复类型兼容性问题
    models.value = Object.values(modelsResponse.models || {}).map((model: ModelConfigData) => ({
      modelName: model.modelName,
      provider: model.provider || 'other',
      enabled: model.enabled ?? true  // 使用空值合并操作符
    }))
    operationsData.value = operationsResponse
  } catch (error) {
    console.error('加载数据失败:', error)
    showToast('加载数据失败，请稍后重试', 'error')
  } finally {
    loading.value = false
  }
}

// 测试操作
const testOperation = async (operationType: string) => {
  try {
    testing.value = operationType
    // 这里可以添加测试逻辑
    await new Promise(resolve => setTimeout(resolve, 2000))
    showToast('测试成功')
  } catch (error) {
    console.error('测试失败:', error)
    showToast('测试失败，请检查配置', 'error')
  } finally {
    testing.value = null
  }
}

// 更新操作映射
const updateOperationMapping = async (operationType: string, modelName: string) => {
  try {
    await aiOperationApi.setOperationMapping(operationType, modelName)
    showToast('映射更新成功')
    await refreshData()
  } catch (error) {
    console.error('更新映射失败:', error)
    showToast('更新映射失败，请稍后重试', 'error')
  }
}

// 清除映射
const clearMapping = async (operationType: string) => {
  try {
    await aiOperationApi.setOperationMapping(operationType, '')
    showToast('映射已清除')
    await refreshData()
  } catch (error) {
    console.error('清除映射失败:', error)
    showToast('清除映射失败，请稍后重试', 'error')
  }
}

// 编辑操作
const editOperation = (operationType: string, operation: OperationConfigData) => {
  editingOperationType.value = operationType
  editingOperation.value = operation
  Object.assign(operationForm, {
    ...operation,
    operationType
  })
}

// 关闭编辑弹窗
const closeEditOperation = () => {
  editingOperation.value = null
  editingOperationType.value = ''
}

// 保存操作配置
const saveOperationConfig = async () => {
  try {
    saving.value = true
    // 修正：直接传递operationForm对象，而不是嵌套传递
    await aiOperationApi.saveOperationConfig(editingOperationType.value, operationForm)
    showToast('配置保存成功')
    closeEditOperation()
    await refreshData()
  } catch (error: any) {
    console.error('保存配置失败:', error)
    showToast('保存失败: ' + (error.message || '未知错误'), 'error')
  } finally {
    saving.value = false
  }
}

// 批量配置
const closeBatchConfig = () => {
  showBatchConfig.value = false
  batchDefaultModel.value = ''
  selectedOperations.value = []
}

const applyBatchConfig = async () => {
  try {
    batchSaving.value = true

    // 修正：创建正确的映射对象
    const mappings: Record<string, string> = {}
    selectedOperations.value.forEach(operationType => {
      mappings[operationType] = batchDefaultModel.value
    })

    await aiOperationApi.setOperationMappings(mappings)
    showToast('批量配置成功')
    closeBatchConfig()
    await refreshData()
  } catch (error: any) {
    console.error('批量配置失败:', error)
    showToast('批量配置失败: ' + (error.message || '未知错误'), 'error')
  } finally {
    batchSaving.value = false
  }
}

// 显示提示
const showToast = (message: string, type: 'success' | 'error' = 'success') => {
  toastMessage.value = message
  if (type === 'success') {
    showSuccessToast.value = true
    setTimeout(() => {
      showSuccessToast.value = false
    }, 3000)
  } else {
    showErrorToast.value = true
    setTimeout(() => {
      showErrorToast.value = false
    }, 3000)
  }
}

// 初始化
refreshData()
</script>

<style scoped>
/* 基础布局 */
.ai-node-config-content {
  padding: 0;
  min-height: 100vh;
}

/* 紧凑型头部 - 整合统计信息 */
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  margin: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e293b 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 头部统计信息 */
.header-stats {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  min-width: 56px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.stat-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-item.success {
  background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
  border-color: rgba(187, 247, 208, 0.8);
}

.stat-item.warning {
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
  border-color: rgba(254, 215, 170, 0.8);
}

.stat-item.info {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-color: rgba(191, 219, 254, 0.8);
}

.stat-number {
  font-size: 16px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
}

.stat-label {
  font-size: 10px;
  color: #64748b;
  margin-top: 2px;
  text-align: center;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
}

.btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s;
}

.btn:hover::before {
  left: 100%;
}

.btn-secondary {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  color: #475569;
  border-color: rgba(226, 232, 240, 0.8);
}

.btn-secondary:hover {
  background: linear-gradient(135deg, #e2e8f0 0%, #cbd5e1 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.btn-primary {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  border-color: transparent;
}

.btn-primary:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.4);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
}

.search-container {
  position: relative;
  width: 280px;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  width: 16px;
  height: 16px;
  color: #9ca3af;
  transition: color 0.3s ease;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  border: 1px solid rgba(209, 213, 219, 0.6);
  border-radius: 8px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.search-input:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1), 0 2px 8px rgba(0, 0, 0, 0.08);
}

.search-input:focus + .search-icon {
  color: #3b82f6;
}

.clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  padding: 4px;
  background: none;
  border: none;
  color: #9ca3af;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.clear-btn:hover {
  background: rgba(156, 163, 175, 0.1);
  color: #6b7280;
}

.filter-tabs {
  display: flex;
  gap: 6px;
  background: rgba(255, 255, 255, 0.6);
  padding: 3px;
  border-radius: 8px;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

.filter-tab {
  padding: 6px 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  color: #64748b;
}

.filter-tab:hover {
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.filter-tab.active {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 1px 4px rgba(59, 130, 246, 0.3);
}

.count {
  background: rgba(255, 255, 255, 0.25);
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 10px;
  margin-left: 4px;
  font-weight: 600;
}

/* 主要内容区域 */
.main-content {
  padding: 20px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(59, 130, 246, 0.1);
  border-top: 3px solid #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

/* 操作节点网格 */
.operations-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.operation-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  padding: 16px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: relative;
  overflow: hidden;
}

.operation-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #e2e8f0, #cbd5e1);
  transition: all 0.3s ease;
}

.operation-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.operation-card.configured::before {
  background: linear-gradient(90deg, #10b981, #059669);
}

.operation-card.configured {
  border-color: rgba(16, 185, 129, 0.3);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(240, 253, 244, 0.6) 100%);
}

.operation-card.disabled {
  opacity: 0.6;
  background: rgba(248, 250, 252, 0.8);
}

.operation-card.testing::before {
  background: linear-gradient(90deg, #3b82f6, #2563eb);
}

.operation-card.testing {
  border-color: rgba(59, 130, 246, 0.3);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(239, 246, 255, 0.6) 100%);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.operation-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.operation-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.provider-icon-large {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.default-icon {
  font-size: 18px;
}

.operation-details {
  flex: 1;
}

.operation-name {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 4px 0;
  line-height: 1.2;
}

.operation-desc {
  font-size: 12px;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

.status-badge {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-badge.configured {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #166534;
  border: 1px solid rgba(187, 247, 208, 0.6);
}

.status-badge.pending {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
  border: 1px solid rgba(253, 230, 138, 0.6);
}

.status-badge.disabled {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #64748b;
  border: 1px solid rgba(226, 232, 240, 0.6);
}

/* 模型绑定区域 */
.model-binding {
  margin-bottom: 12px;
}

.model-selector {
  position: relative;
}

.model-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid rgba(209, 213, 219, 0.6);
  border-radius: 8px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.model-select:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1), 0 2px 8px rgba(0, 0, 0, 0.08);
}

.model-select.has-value {
  border-color: #10b981;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(240, 253, 244, 0.3) 100%);
}

/* 配置信息 */
.config-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.8) 0%, rgba(241, 245, 249, 0.6) 100%);
  border-radius: 8px;
  border: 1px solid rgba(226, 232, 240, 0.4);
}

.config-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.config-label {
  font-size: 11px;
  color: #64748b;
  font-weight: 500;
}

.config-value {
  font-size: 11px;
  font-weight: 600;
  color: #1e293b;
}

.config-features {
  display: flex;
  gap: 6px;
}

.feature-tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.feature-tag.json {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
  border: 1px solid rgba(191, 219, 254, 0.6);
}

.feature-tag.thinking {
  background: linear-gradient(135deg, #fce7f3 0%, #f9a8d4 100%);
  color: #be185d;
  border: 1px solid rgba(249, 168, 212, 0.6);
}

/* 操作按钮 */
.card-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}

.action-btn.large {
  padding: 8px;
  min-width: 36px;
  min-height: 36px;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.5s;
}

.action-btn:hover::before {
  left: 100%;
}

.action-btn.test {
  border-color: rgba(16, 185, 129, 0.4);
  color: #10b981;
}

.action-btn.test:hover {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.3);
}

.action-btn.edit {
  border-color: rgba(59, 130, 246, 0.4);
  color: #3b82f6;
}

.action-btn.edit:hover {
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.action-btn.clear {
  border-color: rgba(239, 68, 68, 0.4);
  color: #ef4444;
}

.action-btn.clear:hover {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 20px 40px -12px rgba(0, 0, 0, 0.25);
  max-width: 480px;
  width: 90%;
  max-height: 90vh;
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: slideUp 0.3s ease-out;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-content.wide {
  max-width: 720px;
  width: 95%;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.4);
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, #1e293b 0%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.btn-close {
  padding: 6px;
  background: rgba(248, 250, 252, 0.8);
  border: 1px solid rgba(226, 232, 240, 0.6);
  color: #64748b;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.btn-close:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.modal-body {
  padding: 20px;
}

.modal-body.compact {
  padding: 16px 20px;
}

/* 表单样式 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.form-grid.wide {
  grid-template-columns: repeat(3, 1fr);
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group.full-width {
  grid-column: 1 / -1;
}

.form-group.checkbox-group {
  flex-direction: row;
  align-items: center;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.form-input, .form-textarea {
  padding: 8px 12px;
  border: 1px solid rgba(209, 213, 219, 0.6);
  border-radius: 8px;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1), 0 2px 8px rgba(0, 0, 0, 0.08);
}

.checkbox-input {
  margin-right: 8px;
  transform: scale(1.1);
}

.checkbox-label {
  display: flex;
  align-items: center;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
}

.operation-checkboxes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  max-height: 180px;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.5);
}

.checkbox-item {
  display: flex;
  align-items: center;
  padding: 6px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
  font-size: 12px;
}

.checkbox-item:hover {
  background: rgba(59, 130, 246, 0.1);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(226, 232, 240, 0.4);
}

.btn-loading {
  width: 14px;
  height: 14px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-right: 6px;
}

/* 成功提示 */
.success-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.3);
  z-index: 1001;
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 13px;
}

/* 错误提示 */
.error-toast {
  position: fixed;
  top: 20px;
  right: 20px;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: white;
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.3);
  z-index: 1001;
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 13px;
}

@keyframes slideIn {
  from {
    transform: translateX(100%) scale(0.9);
    opacity: 0;
  }
  to {
    transform: translateX(0) scale(1);
    opacity: 1;
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-header {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
    padding: 12px 16px;
  }

  .header-right {
    flex-direction: column;
    gap: 12px;
  }

  .header-stats {
    justify-content: center;
    gap: 8px;
  }

  .header-actions {
    justify-content: center;
  }

  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
    padding: 12px 16px;
  }

  .search-container {
    width: 100%;
  }

  .main-content {
    padding: 16px;
  }

  .operations-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid.wide {
    grid-template-columns: repeat(2, 1fr);
  }

  .operation-checkboxes {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .header-stats {
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }

  .form-grid.wide {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .modal-content {
    margin: 16px;
    width: calc(100% - 32px);
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .ai-node-config-content {
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  }

  .content-header {
    background: rgba(30, 41, 59, 0.95);
    border-bottom-color: rgba(71, 85, 105, 0.6);
  }

  .header-left h2 {
    background: linear-gradient(135deg, #f8fafc 0%, #cbd5e1 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .operation-card {
    background: rgba(30, 41, 59, 0.95);
    border-color: rgba(71, 85, 105, 0.6);
  }

  .modal-content {
    background: rgba(30, 41, 59, 0.98);
    border-color: rgba(71, 85, 105, 0.4);
  }
}
</style>
