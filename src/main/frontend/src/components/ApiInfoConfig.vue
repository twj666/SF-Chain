<template>
  <div class="api-config-content">
    <!-- 页面头部 - 紧贴左上角 -->
    <div class="content-header">
      <div class="header-left">
        <h2>AI模型</h2>
      </div>
      <div class="header-actions">
        <button @click="loadModels" class="btn btn-secondary" :disabled="loading">
          <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
          </svg>
          <span>{{ loading ? '加载中' : '刷新' }}</span>
        </button>
        <button @click="showAddModel = true" class="btn btn-primary">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"></path>
          </svg>
          <span>添加模型</span>
        </button>
      </div>
    </div>

<!-- 可点击的统计卡片 -->
<div class="stats-section" v-if="modelsData">
  <div
    class="stat-card"
    :class="{ 'active': selectedProvider === 'all' }"
    @click="selectProvider('all')"
  >
    <div class="stat-content">
      <div class="stat-number">{{ modelsData.total }}</div>
      <div class="stat-label">全部模型</div>
    </div>
  </div>
  <div
    class="stat-card"
    v-for="{ provider, models } in sortedProviderStats"
    :key="provider"
    :class="{ 'active': selectedProvider === provider }"
    @click="selectProvider(provider)"
  >
    <div class="stat-content">
      <div class="stat-number">{{ models.length }}</div>
      <div class="stat-label">{{ getProviderName(provider) }}</div>
    </div>
    <div class="stat-icon">
      <img :src="getProviderIcon(provider)" alt="provider icon" class="stat-provider-icon" />
    </div>
  </div>
</div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 加载状态 -->
      <div v-if="loading && !modelsData" class="loading-state">
        <div class="loading-spinner"></div>
        <p>正在加载模型配置...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="modelsData && modelsData.total === 0" class="empty-state">
        <div class="empty-icon">🤖</div>
        <h3>暂无模型配置</h3>
        <p>点击"添加模型"按钮开始配置您的第一个AI模型</p>
      </div>

      <!-- 模型列表 -->
      <div v-else class="models-list">
        <div class="list-header">
          <div class="header-info">
            <h3>{{ getFilterTitle() }} ({{ Object.keys(filteredModels).length }})</h3>
            <div class="search-box">
              <svg class="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
              </svg>
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索模型..."
                class="search-input"
              />
              <button
                v-if="searchQuery"
                @click="searchQuery = ''"
                class="clear-search"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- 表头 -->
        <div class="table-header">
          <div class="header-cell model-cell">模型信息</div>
          <div class="header-cell base-url-cell">Base URL</div>
          <div class="header-cell params-cell">Token/温度</div>
          <div class="header-cell description-cell">描述</div>
          <div class="header-cell status-cell">状态</div>
          <div class="header-cell actions-cell">操作</div>
        </div>

        <div class="models-container">
          <div
            v-for="(model, modelName) in filteredModels"
            :key="modelName"
            class="model-row"
            :class="{ 'disabled': !model.enabled }"
          >
            <!-- 模型信息 -->
            <div class="model-cell">
              <div class="model-identity">
                <div
                  class="model-avatar"
                  :title="'点击复制图标'"
                  @click="copyToClipboard(getModelIcon(model.provider || 'other'))"
                >
                  <img :src="getProviderIcon(model.provider || 'other')" alt="provider icon" class="provider-icon" />
                </div>
                <div class="model-info">
                  <h4
                    class="model-name"
                    :title="'点击复制: ' + model.modelName"
                    @click="copyToClipboard(model.modelName)"
                  >
                    {{ model.modelName }}
                  </h4>
                  <span class="provider-badge" :class="model.provider">
                    {{ getProviderName(model.provider || 'other') }}
                  </span>
                </div>
              </div>
            </div>

            <!-- Base URL -->
            <div class="base-url-cell">
              <span
                class="base-url-text"
                :title="'点击复制: ' + model.baseUrl"
                @click="copyToClipboard(model.baseUrl)"
              >
                {{ model.baseUrl }}
              </span>
            </div>

            <!-- Token/温度 -->
            <div class="params-cell">
              <span class="params-text">
                {{ model.defaultMaxTokens || 'N/A' }} / {{ model.defaultTemperature || 'N/A' }}
              </span>
            </div>

            <!-- 描述 -->
            <div class="description-cell">
              <span class="description-text" :title="model.description">
                {{ model.description || '-' }}
              </span>
            </div>

            <!-- 状态 -->
            <div class="status-cell">
              <span class="status-badge" :class="{ 'enabled': model.enabled, 'disabled': !model.enabled }">
                {{ model.enabled ? '启用' : '禁用' }}
              </span>
            </div>

            <!-- 操作按钮 -->
            <div class="actions-cell">
              <div class="action-buttons">
                <button
                  @click="testModel(String(modelName))"
                  class="action-btn test"
                  :disabled="testing === modelName"
                  :title="testing === modelName ? '测试中...' : '测试连接'"
                >
                  <svg v-if="testing === modelName" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                  </svg>
                  <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                </button>

                <button
                  @click="cloneModel(String(modelName), model)"
                  class="action-btn clone"
                  title="克隆模型"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                  </svg>
                </button>

                <button
                  @click="editModel(String(modelName), model)"
                  class="action-btn edit"
                  title="编辑模型"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path>
                  </svg>
                </button>

                <button
                  @click="deleteModel(String(modelName))"
                  class="action-btn delete"
                  title="删除模型"
                >
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加/编辑模型弹窗 - 无滚动条优化 -->
    <div v-if="showAddModel || editingModel" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editingModel ? '编辑模型' : '添加模型' }}</h3>
          <button @click="closeModal" class="btn-close">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
            </svg>
          </button>
        </div>

        <form @submit.prevent="saveModel" class="modal-body">
          <!-- 紧凑的两列布局 -->
          <div class="form-grid">
            <!-- 左列 -->
            <div class="form-column">
              <!-- 基本信息 -->
              <div class="form-group">
                <label for="modelName">模型名称 *</label>
                <input
                  id="modelName"
                  v-model="modelForm.modelName"
                  type="text"
                  placeholder="例如: gpt-4o"
                  class="form-input"
                  required
                  :disabled="!!editingModel"
                />
              </div>

              <div class="form-group">
                <label for="provider">提供商</label>
                <select id="provider" v-model="modelForm.provider" class="form-input" @change="onProviderChange">
                  <optgroup v-for="(providers, category) in providersByCategory" :key="category" :label="getCategoryName(category)">
                    <option v-for="provider in providers" :key="provider.key" :value="provider.key">
                      {{ provider.name }}
                    </option>
                  </optgroup>
                </select>
              </div>

              <div class="form-group">
                <label for="baseUrl">Base URL *</label>
                <input
                  id="baseUrl"
                  v-model="modelForm.baseUrl"
                  type="url"
                  placeholder="https://api.openai.com/v1"
                  class="form-input"
                  required
                />
              </div>

              <!-- 参数设置行 -->
              <div class="form-row">
                <div class="form-group">
                  <label for="maxTokens">Token数</label>
                  <input
                    id="maxTokens"
                    v-model.number="modelForm.defaultMaxTokens"
                    type="number"
                    placeholder="4096"
                    class="form-input"
                  />
                </div>
                <div class="form-group">
                  <label for="temperature">温度</label>
                  <input
                    id="temperature"
                    v-model.number="modelForm.defaultTemperature"
                    type="number"
                    step="0.1"
                    min="0"
                    max="2"
                    placeholder="0.7"
                    class="form-input"
                  />
                </div>
              </div>
            </div>

            <!-- 右列 -->
            <div class="form-column">
              <div class="form-group">
                <label for="apiKey">API Key *</label>
                <div class="input-wrapper">
                  <input
                    id="apiKey"
                    v-model="modelForm.apiKey"
                    :type="showApiKey ? 'text' : 'password'"
                    placeholder="请输入API Key"
                    class="form-input"
                    required
                  />
                  <button
                    @click="showApiKey = !showApiKey"
                    class="toggle-visibility"
                    type="button"
                  >
                    <svg v-if="showApiKey" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                    <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"></path>
                    </svg>
                  </button>
                </div>
              </div>

              <div class="form-group">
                <label for="enabled">状态</label>
                <select id="enabled" v-model="modelForm.enabled" class="form-input">
                  <option :value="true">启用</option>
                  <option :value="false">禁用</option>
                </select>
              </div>

              <div class="form-group">
                <label for="description">描述</label>
                <textarea
                  id="description"
                  v-model="modelForm.description"
                  placeholder="模型描述信息"
                  class="form-textarea"
                  rows="3"
                ></textarea>
              </div>
            </div>
          </div>

          <div class="form-actions">
            <button type="button" @click="closeModal" class="btn btn-secondary">
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

    <!-- 复制成功提示 -->
    <div v-if="showCopyToast" class="copy-toast">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
      </svg>
      复制成功
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { aiModelApi } from '@/services/aiModelApi'
import type { ModelConfigData, ModelsResponse } from '@/types/system'
// 导入增强的AI提供商工具函数
import { 
  getProviderName, 
  getProviderIcon, 
  getProviderOrder,
  getAllProviders,
  getProvidersByCategory,
  getCategoryName,
  getProviderDefaultConfig
} from '@/utils/aiProviders'

// 状态管理
const loading = ref(false)
const saving = ref(false)
const testing = ref<string | null>(null)
const showAddModel = ref(false)
const editingModel = ref<string | null>(null)
const showApiKey = ref(false)
const modelsData = ref<ModelsResponse | null>(null)
const searchQuery = ref('')
const selectedProvider = ref<string>('all')
const showCopyToast = ref(false)

// 表单数据
const modelForm = reactive<ModelConfigData>({
  modelName: '',
  baseUrl: '',
  apiKey: '',
  defaultMaxTokens: 4096,
  defaultTemperature: 0.7,
  supportStream: true,
  supportJsonOutput: true,
  supportThinking: false,
  description: '',
  provider: 'openai',
  enabled: true
})

// 计算属性 - 过滤后的模型
const filteredModels = computed(() => {
  if (!modelsData.value?.models) {
    return {}
  }

  let filtered = { ...modelsData.value.models }

  // 按提供商过滤
  if (selectedProvider.value !== 'all') {
    filtered = Object.fromEntries(
      Object.entries(filtered).filter(([, model]) => model.provider === selectedProvider.value)
    )
  }

  // 按搜索关键词过滤
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase()
    filtered = Object.fromEntries(
      Object.entries(filtered).filter(([, model]) =>
        model.modelName.toLowerCase().includes(query) ||
        model.provider?.toLowerCase().includes(query) ||
        model.baseUrl.toLowerCase().includes(query) ||
        model.description?.toLowerCase().includes(query)
      )
    )
  }

  return filtered
})

// 新增：按照getProviderIcon顺序排列的提供商统计
const sortedProviderStats = computed(() => {
  if (!modelsData.value?.groupedByProvider) {
    return []
  }

  const stats = []
  const providerOrder = getProviderOrder()

  // 按照预定义顺序添加存在的提供商
  for (const provider of providerOrder) {
    if (modelsData.value.groupedByProvider[provider]) {
      stats.push({
        provider,
        models: modelsData.value.groupedByProvider[provider]
      })
    }
  }

  // 添加不在预定义列表中的其他提供商
  for (const [provider, models] of Object.entries(modelsData.value.groupedByProvider)) {
    if (!providerOrder.includes(provider)) {
      stats.push({ provider, models })
    }
  }

  return stats
})

// 计算属性 - 按分类分组的提供商
const providersByCategory = computed(() => {
  return getProvidersByCategory()
})

// 辅助函数
const getModelIcon = (provider?: string) => {
  return getProviderIcon(provider || 'other')
}

const getFilterTitle = () => {
  if (selectedProvider.value === 'all') {
    return '全部模型'
  }
  return `${getProviderName(selectedProvider.value)}模型`
}

// 选择提供商
const selectProvider = (provider: string) => {
  selectedProvider.value = provider
  searchQuery.value = ''
}

// 提供商变更时的处理
const onProviderChange = () => {
  const defaultConfig = getProviderDefaultConfig(modelForm.provider)
  if (defaultConfig) {
    // 只在字段为空时应用默认值，避免覆盖用户已输入的内容
    if (!modelForm.baseUrl) {
      modelForm.baseUrl = defaultConfig.baseUrl || ''
    }
    if (!modelForm.defaultMaxTokens) {
      modelForm.defaultMaxTokens = defaultConfig.maxTokens || 4096
    }
    if (!modelForm.defaultTemperature) {
      modelForm.defaultTemperature = defaultConfig.temperature || 0.7
    }
    // 应用支持特性的默认值
    modelForm.supportStream = defaultConfig.supportStream ?? true
    modelForm.supportJsonOutput = defaultConfig.supportJsonOutput ?? true
    modelForm.supportThinking = defaultConfig.supportThinking ?? false
  }
}

// 复制到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    showCopyToast.value = true
    setTimeout(() => {
      showCopyToast.value = false
    }, 2000)
  } catch (error) {
    console.error('复制失败:', error)
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = text
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)

    showCopyToast.value = true
    setTimeout(() => {
      showCopyToast.value = false
    }, 2000)
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(modelForm, {
    modelName: '',
    baseUrl: '',
    apiKey: '',
    defaultMaxTokens: 4096,
    defaultTemperature: 0.7,
    supportStream: true,
    supportJsonOutput: true,
    supportThinking: false,
    description: '',
    provider: 'openai',
    enabled: true
  })
}

// 加载模型列表
const loadModels = async () => {
  try {
    loading.value = true
    modelsData.value = await aiModelApi.getAllModels()
  } catch (error) {
    console.error('加载模型列表失败:', error)
    alert('加载模型列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 保存模型
const saveModel = async () => {
  try {
    saving.value = true
    const response = await aiModelApi.saveModel(modelForm.modelName, modelForm)

    if (response.success) {
      alert(response.message || '模型保存成功')
      closeModal()
      await loadModels()
    } else {
      alert(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存模型失败:', error)
    alert('保存模型失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

// 克隆模型
const cloneModel = (modelName: string, model: ModelConfigData) => {
  // 复制所有模型信息，但清空模型名称
  Object.assign(modelForm, {
    ...model,
    modelName: '' // 清空模型名称，让用户重新填写
  })

  // 设置为添加模式（不是编辑模式）
  editingModel.value = null
  showAddModel.value = true
}

// 编辑模型
const editModel = (modelName: string, model: ModelConfigData) => {
  editingModel.value = modelName
  Object.assign(modelForm, model)
  showAddModel.value = false
}

// 删除模型
const deleteModel = async (modelName: string) => {
  if (!confirm(`确定要删除模型 "${modelName}" 吗？`)) {
    return
  }

  try {
    const response = await aiModelApi.deleteModel(modelName)
    if (response.success) {
      alert(response.message || '删除成功')
      await loadModels()
    } else {
      alert(response.message || '删除失败')
    }
  } catch (error) {
    console.error('删除模型失败:', error)
    alert('删除模型失败，请稍后重试')
  }
}

// 测试模型
const testModel = async (modelName: string) => {
  try {
    testing.value = modelName
    const response = await aiModelApi.testModel(modelName)

    if (response.success) {
      alert('模型连接测试成功！')
    } else {
      alert(`测试失败: ${response.message}`)
    }
  } catch (error) {
    console.error('测试模型失败:', error)
    alert('测试模型失败，请稍后重试')
  } finally {
    testing.value = null
  }
}

// 关闭弹窗
const closeModal = () => {
  showAddModel.value = false
  editingModel.value = null
  resetForm()
  showApiKey.value = false
}

// 组件挂载时加载数据
onMounted(() => {
  loadModels()
})
</script>

<style scoped>
/* 页面头部 - 更紧贴左上角 */
.content-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.header-left {
  flex: 1;
}

.header-left h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 0.125rem 0;
}

.header-left p {
  font-size: 0.8125rem;
  color: #718096;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

/* 可点击的统计卡片 */
.stats-section {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.stat-card {
  background: white;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 2px solid #e2e8f0;
  min-width: 90px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.stat-card:hover {
  border-color: #667eea;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
}

.stat-card.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: #667eea;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 1.25rem;
  font-weight: 700;
  margin-bottom: 0.125rem;
  line-height: 1;
}

.stat-label {
  font-size: 0.6875rem;
  opacity: 0.8;
  font-weight: 500;
}

.stat-icon {
  font-size: 1.25rem;
  opacity: 0.7;
  margin-left: 0.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-provider-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

/* 主要内容区域 */
.main-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.list-header {
  padding: 1rem;
  border-bottom: 1px solid #e2e8f0;
  background: #f8fafc;
}

.header-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-info h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.search-box {
  position: relative;
  width: 250px;
}

.search-icon {
  position: absolute;
  left: 0.75rem;
  top: 50%;
  transform: translateY(-50%);
  width: 0.875rem;
  height: 0.875rem;
  color: #9ca3af;
}

.search-input {
  width: 100%;
  padding: 0.5rem 2.5rem 0.5rem 2rem;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.8125rem;
  background: white;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.clear-search {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #9ca3af;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.clear-search:hover {
  background: #f3f4f6;
  color: #374151;
}

/* 精确修复表格对齐 */
.table-header {
  display: grid;
  grid-template-columns: 2.6fr 1.5fr 1.0fr 2.2fr 0.8fr 1.2fr;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  background: #f1f5f9;
  border-bottom: 1px solid #e2e8f0;
  font-size: 0.75rem;
  font-weight: 600;
  color: #475569;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.header-cell {
  display: flex;
  align-items: center;
}

.models-container {
  max-height: 55vh;
  overflow-y: auto;
}

.model-row {
  display: grid;
  grid-template-columns: 2.6fr 1.5fr 1.0fr 2.2fr 0.8fr 1.2fr;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
  transition: background-color 0.2s ease;
  align-items: center;
}

.model-row:hover {
  background: #f8fafc;
}

.model-row.disabled {
  opacity: 0.6;
}

.model-row:last-child {
  border-bottom: none;
}

/* 模型信息单元格 */
.model-cell {
  display: flex;
  align-items: center;
}

.model-identity {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
}

.model-avatar {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 4px;
}

.model-avatar:hover {
  background: rgba(0, 0, 0, 0.05);
  transform: scale(1.05);
}

.provider-icon {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.model-info {
  flex: 1;
  min-width: 0;
}

.model-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0 0 0.25rem 0;
  cursor: pointer;
  transition: color 0.2s ease;
  word-break: break-all;
}

.model-name:hover {
  color: #667eea;
}

.provider-badge {
  padding: 0.125rem 0.5rem;
  border-radius: 10px;
  font-size: 0.625rem;
  font-weight: 600;
  background: #e2e8f0;
  color: #4a5568;
}

.provider-badge.openai {
  background: #dcfce7;
  color: #166534;
}

.provider-badge.anthropic {
  background: #fef3c7;
  color: #92400e;
}

.provider-badge.google {
  background: #dbeafe;
  color: #1e40af;
}

.provider-badge.deepseek {
  background: #e0e7ff;
  color: #3730a3;
}

.provider-badge.doubao {
  background: #fce7f3;
  color: #be185d;
}

.provider-badge.qianwen {
  background: #c6e0e9;
  color: #0f0e0e;
}

/* 其他单元格 - 精确对齐 */
.base-url-cell {
  overflow: hidden;
  display: flex;
  align-items: center;
}

.base-url-text {
  font-size: 0.8125rem;
  color: #374151;
  cursor: pointer;
  transition: color 0.2s ease;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.base-url-text:hover {
  color: #667eea;
}

.params-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.params-text {
  font-size: 0.8125rem;
  color: #374151;
  font-weight: 500;
  text-align: center;
}

.description-cell {
  overflow: hidden;
  display: flex;
  align-items: center;
}

.description-text {
  font-size: 0.8125rem;
  color: #6b7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-badge {
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.6875rem;
  font-weight: 600;
  display: inline-block;
}

.status-badge.enabled {
  background: #dcfce7;
  color: #166534;
}

.status-badge.disabled {
  background: #fecaca;
  color: #991b1b;
}

/* 操作按钮 */
.actions-cell {
  display: flex;
  justify-content: center;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 0.25rem;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
}

.action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.action-btn.test {
  background: #dcfce7;
  color: #166534;
}

.action-btn.test:hover:not(:disabled) {
  background: #bbf7d0;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(34, 197, 94, 0.3);
}

.action-btn.clone {
  background: linear-gradient(135deg, #fff9c4 0%, #fef3bd 100%);
  color: #6b7280;
  border: 1px solid #f3e8ff;
}

.action-btn.clone:hover:not(:disabled) {
  background: linear-gradient(135deg, #fef3bd 0%, #fde68a 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(254, 243, 189, 0.3);
  border-color: #e5e7eb;
}

.action-btn.edit {
  background: #dbeafe;
  color: #1e40af;
}

.action-btn.edit:hover:not(:disabled) {
  background: #bfdbfe;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}

.action-btn.delete {
  background: #fecaca;
  color: #991b1b;
}

.action-btn.delete:hover:not(:disabled) {
  background: #fca5a5;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(239, 68, 68, 0.3);
}

/* 加载和空状态 */
.loading-state, .empty-state {
  text-align: center;
  padding: 3rem 2rem;
  color: #6b7280;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #e5e7eb;
  border-top: 3px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.5;
}

.empty-state h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #374151;
  margin: 0 0 0.5rem 0;
}

.empty-state p {
  font-size: 0.875rem;
  margin: 0;
}

/* 精致小巧的弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 800px;
  max-height: 85vh;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.875rem 1.25rem;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 12px 12px 0 0;
  flex-shrink: 0;
}

.modal-header h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.btn-close:hover {
  background: #e5e7eb;
  color: #374151;
}

.modal-body {
  padding: 1.25rem;
  flex: 1;
  overflow-y: auto;
}

/* 两列网格布局 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  margin-bottom: 1rem;
}

.form-column {
  display: flex;
  flex-direction: column;
  gap: 0.875rem;
}

.form-group {
  margin-bottom: 0;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

.form-group label {
  display: block;
  font-weight: 600;
  color: #374151;
  margin-bottom: 0.375rem;
  font-size: 0.75rem;
}

.form-input, .form-textarea {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  background: white;
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 50px;
  font-family: inherit;
}

.input-wrapper {
  position: relative;
}

.toggle-visibility {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  padding: 0.25rem;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.toggle-visibility:hover {
  background: #f3f4f6;
  color: #374151;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid #e5e7eb;
}

/* 按钮样式 */
.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  min-width: 70px;
  justify-content: center;
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
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
  border: 1px solid #d1d5db;
}

.btn-secondary:hover:not(:disabled) {
  background: #e5e7eb;
}

.btn-loading {
  width: 12px;
  height: 12px;
  border: 2px solid transparent;
  border-top: 2px solid currentColor;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 复制成功提示 */
.copy-toast {
  position: fixed;
  top: 2rem;
  right: 2rem;
  background: #059669;
  color: white;
  padding: 0.75rem 1rem;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  font-weight: 500;
  z-index: 1001;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.w-4 {
  width: 1rem;
}

.h-4 {
  height: 1rem;
}

.w-5 {
  width: 1.25rem;
}

.h-5 {
  height: 1.25rem;
}

.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .table-header,
  .model-row {
    grid-template-columns: 2fr 2.5fr 1fr 1.2fr 0.8fr 1fr;
    gap: 0.5rem;
  }

  .search-box {
    width: 200px;
  }

  .modal-content {
    max-width: 600px;
  }
}

@media (max-width: 768px) {
  .content-header {
    flex-direction: column;
    gap: 1rem;
  }

  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .header-info {
    flex-direction: column;
    gap: 0.75rem;
    align-items: flex-start;
  }

  .search-box {
    width: 100%;
  }

  .table-header {
    display: none;
  }

  .model-row {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    padding: 1rem;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    margin-bottom: 0.75rem;
  }

  .models-container {
    padding: 0.75rem;
  }

  .model-identity {
    width: 100%;
  }

  .action-buttons {
    justify-content: flex-end;
    width: 100%;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column;
  }

  .copy-toast {
    top: 1rem;
    right: 1rem;
    left: 1rem;
    right: 1rem;
  }

  .modal-content {
    width: 95%;
    max-width: none;
    margin: 1rem;
    max-height: 70vh;
  }

  .modal-header {
    padding: 0.75rem 1rem;
  }

  .modal-body {
    padding: 0.875rem 1rem;
  }
}

@media (max-width: 480px) {
  .stats-section {
    grid-template-columns: 1fr;
  }

  .stat-card {
    min-width: auto;
  }

  .modal-content {
    max-height: 75vh;
  }
}
</style>
