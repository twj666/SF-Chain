<template>
  <div class="json-viewer">
    <!-- 工具栏 -->
    <div class="json-toolbar">
      <div class="toolbar-left">
        <button @click="toggleFormat" class="tool-btn" :class="{ active: isFormatted }">
          <svg viewBox="0 0 16 16" fill="currentColor">
            <path d="M2 4h12M2 8h12M2 12h12" stroke="currentColor" stroke-width="1.5" fill="none"/>
          </svg>
          {{ isFormatted ? '压缩' : '格式化' }}
        </button>
        <button @click="toggleExpandAll" class="tool-btn">
          <svg viewBox="0 0 16 16" fill="currentColor">
            <path :d="allExpanded ? 'M8 3l5 5-1.5 1.5L8 6l-3.5 3.5L3 8l5-5z' : 'M3 8l5 5 5-5-1.5-1.5L8 10 4.5 6.5 3 8z'" />
          </svg>
          {{ allExpanded ? '折叠' : '展开' }}
        </button>
        <button @click="copyJson" class="tool-btn" :class="{ success: copySuccess }">
          <svg v-if="!copySuccess" viewBox="0 0 16 16" fill="currentColor">
            <path d="M4 2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V2Zm2-1a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V2a1 1 0 0 0-1-1H6ZM2 5a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1v-1h1v1a2 2 0 0 1-2 2H2a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h1v1H2Z"/>
          </svg>
          <svg v-else viewBox="0 0 16 16" fill="currentColor">
            <path d="M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z"/>
          </svg>
          {{ copySuccess ? '已复制' : '复制' }}
        </button>
      </div>
      <div class="toolbar-center">
        <div class="search-box" v-if="isFormatted">
          <svg viewBox="0 0 16 16" fill="currentColor">
            <path d="M11.742 10.344a6.5 6.5 0 1 0-1.397 1.398h-.001c.03.04.062.078.098.115l3.85 3.85a1 1 0 0 0 1.415-1.414l-3.85-3.85a1.007 1.007 0 0 0-.115-.1zM12 6.5a5.5 5.5 0 1 1-11 0 5.5 5.5 0 0 1 11 0z"/>
          </svg>
          <input
            v-model="searchQuery"
            placeholder="搜索键或值..."
            @keydown.esc="searchQuery = ''"
            class="search-input"
          />
          <button v-if="searchQuery" @click="searchQuery = ''" class="clear-search">
            <svg viewBox="0 0 16 16" fill="currentColor">
              <path d="M2.146 2.854a.5.5 0 1 1 .708-.708L8 7.293l5.146-5.147a.5.5 0 0 1 .708.708L8.707 8l5.147 5.146a.5.5 0 0 1-.708.708L8 8.707l-5.146 5.147a.5.5 0 0 1-.708-.708L7.293 8 2.146 2.854Z"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="toolbar-right">
        <span class="path-display" v-if="hoveredPath">{{ hoveredPath }}</span>
        <span class="size-info">{{ getSizeInfo() }}</span>
        <span class="item-count" v-if="isFormatted">{{ getItemCount() }}</span>
      </div>
    </div>

    <!-- JSON内容 -->
    <div class="json-content" ref="jsonContainer">
      <div v-if="!isFormatted" class="json-raw">
        {{ JSON.stringify(data) }}
      </div>
      <div v-else class="json-formatted" v-html="highlightedJson"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'

// 类型定义
type JsonValue = string | number | boolean | null | JsonObject | JsonValue[]
interface JsonObject {
  [key: string]: JsonValue
}

interface Props {
  data: JsonValue
  maxHeight?: string
}

const props = withDefaults(defineProps<Props>(), {
  maxHeight: '400px'
})

// 响应式状态
const isFormatted = ref(true)
const allExpanded = ref(true)
const expandedPaths = ref(new Set<string>())
const searchQuery = ref('')
const copySuccess = ref(false)
const hoveredPath = ref('')

// 搜索结果计算
const searchResults = computed(() => {
  if (!searchQuery.value.trim()) return new Set<string>()

  const results = new Set<string>()
  const query = searchQuery.value.toLowerCase()

  const searchInObject = (obj: JsonValue, path = ''): void => {
    if (obj === null || obj === undefined) return

    if (typeof obj === 'object' && !Array.isArray(obj)) {
      Object.entries(obj).forEach(([key, value]) => {
        const currentPath = path ? `${path}.${key}` : key

        // 搜索键名
        if (key.toLowerCase().includes(query)) {
          results.add(currentPath)
        }

        // 搜索值
        if (typeof value === 'string' && value.toLowerCase().includes(query)) {
          results.add(currentPath)
        }

        searchInObject(value, currentPath)
      })
    } else if (Array.isArray(obj)) {
      obj.forEach((item, index) => {
        const currentPath = `${path}[${index}]`
        if (typeof item === 'string' && item.toLowerCase().includes(query)) {
          results.add(currentPath)
        }
        searchInObject(item, currentPath)
      })
    }
  }

  searchInObject(props.data)
  return results
})

// JSON高亮渲染
const highlightJson = (obj: JsonValue, path = '', level = 0): string => {
  if (obj === null) return '<span class="json-null">null</span>'
  if (obj === undefined) return '<span class="json-undefined">undefined</span>'

  const type = typeof obj
  const indent = '  '.repeat(level)
  const isSearchMatch = searchResults.value.has(path)
  const highlightClass = isSearchMatch ? ' search-highlight' : ''

  if (type === 'string') {
    return `<span class="json-string${highlightClass}" data-path="${path}">${escapeHtml(JSON.stringify(obj))}</span>`
  }

  if (type === 'number') {
    return `<span class="json-number${highlightClass}" data-path="${path}">${obj}</span>`
  }

  if (type === 'boolean') {
    return `<span class="json-boolean${highlightClass}" data-path="${path}">${obj}</span>`
  }

  if (Array.isArray(obj)) {
    if (obj.length === 0) return '<span class="json-bracket">[]</span>'

    const isExpanded = allExpanded.value || expandedPaths.value.has(path) || searchResults.value.has(path)
    const toggleClass = isExpanded ? 'expanded' : 'collapsed'

    if (!isExpanded) {
      return `<span class="json-toggle ${toggleClass}" data-path="${path}">▶</span><span class="json-bracket">[</span><span class="json-preview">...${obj.length}项</span><span class="json-bracket">]</span>`
    }

    const items = obj.map((item, index) => {
      const itemPath = `${path}[${index}]`
      return `\n${indent}  ${highlightJson(item, itemPath, level + 1)}`
    }).join(',')

    return `<span class="json-toggle ${toggleClass}" data-path="${path}">▼</span><span class="json-bracket">[</span>${items}\n${indent}<span class="json-bracket">]</span>`
  }

  if (type === 'object' && obj !== null) {
    const entries = Object.entries(obj as JsonObject)
    if (entries.length === 0) return '<span class="json-bracket">{}</span>'

    const isExpanded = allExpanded.value || expandedPaths.value.has(path) || searchResults.value.has(path)
    const toggleClass = isExpanded ? 'expanded' : 'collapsed'

    if (!isExpanded) {
      return `<span class="json-toggle ${toggleClass}" data-path="${path}">▶</span><span class="json-bracket">{</span><span class="json-preview">...${entries.length}键</span><span class="json-bracket">}</span>`
    }

    const items = entries.map(([key, value]) => {
      const keyPath = path ? `${path}.${key}` : key
      const keyHighlight = searchResults.value.has(keyPath) ? ' search-highlight' : ''
      return `\n${indent}  <span class="json-key${keyHighlight}" data-path="${keyPath}">${escapeHtml(JSON.stringify(key))}</span><span class="json-colon">:</span> ${highlightJson(value, keyPath, level + 1)}`
    }).join(',')

    return `<span class="json-toggle ${toggleClass}" data-path="${path}">▼</span><span class="json-bracket">{</span>${items}\n${indent}<span class="json-bracket">}</span>`
  }

  return String(obj)
}

const escapeHtml = (text: string): string => {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const highlightedJson = computed(() => {
  return highlightJson(props.data)
})

// 工具函数
const toggleFormat = () => {
  isFormatted.value = !isFormatted.value
  if (!isFormatted.value) {
    searchQuery.value = ''
  }
}

const toggleExpandAll = () => {
  allExpanded.value = !allExpanded.value
  expandedPaths.value.clear()
}

const copyJson = async () => {
  try {
    const text = isFormatted.value
      ? JSON.stringify(props.data, null, 2)
      : JSON.stringify(props.data)
    await navigator.clipboard.writeText(text)
    copySuccess.value = true
    setTimeout(() => {
      copySuccess.value = false
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}

const getSizeInfo = () => {
  const jsonStr = JSON.stringify(props.data)
  const size = new Blob([jsonStr]).size
  if (size < 1024) return `${size}B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${(size / (1024 * 1024)).toFixed(1)}MB`
}

const getItemCount = () => {
  const count = countItems(props.data)
  return `${count}项`
}

const countItems = (obj: JsonValue): number => {
  if (obj === null || obj === undefined || typeof obj !== 'object') return 1

  if (Array.isArray(obj)) {
    return obj.reduce((sum, item) => sum + countItems(item), 0)
  }

  return Object.values(obj as JsonObject).reduce((sum, value) => sum + countItems(value), 0)
}

// 事件处理
const handleClick = (event: Event) => {
  const target = event.target as HTMLElement
  if (target.classList.contains('json-toggle')) {
    const path = target.dataset.path || ''
    if (expandedPaths.value.has(path)) {
      expandedPaths.value.delete(path)
    } else {
      expandedPaths.value.add(path)
    }
  }
}

const handleMouseOver = (event: Event) => {
  const target = event.target as HTMLElement
  const path = target.dataset.path
  if (path) {
    hoveredPath.value = path
  }
}

const handleMouseOut = () => {
  hoveredPath.value = ''
}

// 生命周期
nextTick(() => {
  const container = document.querySelector('.json-formatted')
  if (container) {
    container.addEventListener('click', handleClick)
    container.addEventListener('mouseover', handleMouseOver)
    container.addEventListener('mouseout', handleMouseOut)
  }
})
</script>

<style scoped>
.json-viewer {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  background: #ffffff;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 13px;
}

.json-toolbar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-bottom: 1px solid #e2e8f0;
  gap: 12px;
  min-height: 40px;
}

.toolbar-left {
  display: flex;
  gap: 6px;
}

.toolbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #64748b;
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  background: #ffffff;
  color: #6b7280;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  height: 24px;
}

.tool-btn:hover {
  background: #f9fafb;
  border-color: #9ca3af;
  color: #374151;
}

.tool-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.tool-btn.success {
  background: #10b981;
  color: white;
  border-color: #10b981;
}

.tool-btn svg {
  width: 12px;
  height: 12px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 200px;
}

.search-box svg {
  position: absolute;
  left: 8px;
  width: 12px;
  height: 12px;
  color: #9ca3af;
  z-index: 1;
}

.search-input {
  width: 100%;
  padding: 4px 24px 4px 24px;
  border: 1px solid #d1d5db;
  border-radius: 4px;
  font-size: 11px;
  background: #ffffff;
  outline: none;
  transition: border-color 0.15s ease;
}

.search-input:focus {
  border-color: #3b82f6;
}

.clear-search {
  position: absolute;
  right: 4px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  color: #9ca3af;
  display: flex;
  align-items: center;
}

.clear-search svg {
  width: 10px;
  height: 10px;
}

.path-display {
  font-family: inherit;
  color: #3b82f6;
  font-weight: 500;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.size-info,
.item-count {
  font-weight: 600;
  color: #64748b;
}

.json-content {
  max-height: v-bind('props.maxHeight');
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: #cbd5e1 transparent;
}

.json-content::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.json-content::-webkit-scrollbar-track {
  background: transparent;
}

.json-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.json-raw {
  padding: 16px;
  white-space: pre-wrap;
  word-break: break-all;
  color: #374151;
  line-height: 1.5;
}

.json-formatted {
  padding: 16px;
  line-height: 1.4;
  color: #374151;
  white-space: pre;
  overflow-x: auto;
}

/* JSON语法高亮 */
:deep(.json-string) {
  color: #059669;
  font-weight: 500;
}

:deep(.json-number) {
  color: #dc2626;
  font-weight: 600;
}

:deep(.json-boolean) {
  color: #7c3aed;
  font-weight: 600;
}

:deep(.json-null),
:deep(.json-undefined) {
  color: #6b7280;
  font-style: italic;
  font-weight: 500;
}

:deep(.json-key) {
  color: #1e40af;
  font-weight: 600;
}

:deep(.json-bracket) {
  color: #374151;
  font-weight: 700;
}

:deep(.json-colon) {
  color: #6b7280;
  margin: 0 4px;
}

:deep(.json-toggle) {
  cursor: pointer;
  color: #6b7280;
  margin-right: 4px;
  font-size: 10px;
  transition: color 0.15s ease;
  user-select: none;
}

:deep(.json-toggle:hover) {
  color: #374151;
}

:deep(.json-preview) {
  color: #9ca3af;
  font-style: italic;
  margin: 0 4px;
  font-size: 11px;
}

:deep(.search-highlight) {
  background: #fef3c7;
  padding: 1px 2px;
  border-radius: 2px;
  box-shadow: 0 0 0 1px #f59e0b;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .json-toolbar {
    flex-wrap: wrap;
    gap: 8px;
  }

  .toolbar-center {
    order: 3;
    flex-basis: 100%;
    justify-content: flex-start;
  }

  .search-box {
    max-width: none;
    width: 100%;
  }

  .path-display {
    display: none;
  }
}
</style>
