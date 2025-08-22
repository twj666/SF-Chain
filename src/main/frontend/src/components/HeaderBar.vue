<template>
  <div class="header-bar">
    <div class="header-content">
      <div class="header-left">
        <div class="brand-section">
          <div class="brand-logo">
            <!-- 更大更像素的小丑脸图标 -->
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" class="logo-svg">
              <defs>
                <!-- 蓝紫色渐变 -->
                <linearGradient id="faceGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#667eea"/>
                  <stop offset="100%" stop-color="#764ba2"/>
                </linearGradient>
                <linearGradient id="noseGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#f093fb"/>
                  <stop offset="100%" stop-color="#f093fb"/>
                </linearGradient>
                <linearGradient id="mouthGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#4facfe"/>
                  <stop offset="100%" stop-color="#00f2fe"/>
                </linearGradient>
              </defs>

              <!-- 脸部轮廓 (更大的像素化圆形) -->
              <g>
                <!-- 主脸部 -->
                <rect x="4" y="8" width="40" height="32" fill="url(#faceGradient)" rx="0"/>
                <rect x="8" y="4" width="32" height="8" fill="url(#faceGradient)" rx="0"/>
                <rect x="8" y="40" width="32" height="4" fill="url(#faceGradient)" rx="0"/>
                <rect x="2" y="12" width="8" height="24" fill="url(#faceGradient)" rx="0"/>
                <rect x="38" y="12" width="8" height="24" fill="url(#faceGradient)" rx="0"/>
              </g>

              <!-- 眼睛 (更大更像素) -->
              <g>
                <!-- 左眼 -->
                <rect x="10" y="14" width="10" height="10" fill="#1f2937" rx="0"/>
                <rect x="12" y="16" width="6" height="6" fill="#ffffff" rx="0"/>
                <rect x="14" y="18" width="2" height="2" fill="#1f2937" rx="0"/>

                <!-- 右眼 -->
                <rect x="28" y="14" width="10" height="10" fill="#1f2937" rx="0"/>
                <rect x="30" y="16" width="6" height="6" fill="#ffffff" rx="0"/>
                <rect x="32" y="18" width="2" height="2" fill="#1f2937" rx="0"/>
              </g>

              <!-- 鼻子 (更大) -->
              <rect x="20" y="26" width="8" height="6" fill="url(#noseGradient)" rx="0"/>

              <!-- 嘴巴 (更大的小丑笑容) -->
              <g>
                <!-- 嘴巴主体 -->
                <rect x="12" y="34" width="24" height="6" fill="url(#mouthGradient)" rx="0"/>
                <!-- 嘴角上扬 -->
                <rect x="8" y="32" width="8" height="6" fill="url(#mouthGradient)" rx="0"/>
                <rect x="32" y="32" width="8" height="6" fill="url(#mouthGradient)" rx="0"/>
                <!-- 牙齿 -->
                <rect x="16" y="35" width="4" height="4" fill="#ffffff" rx="0"/>
                <rect x="22" y="35" width="4" height="4" fill="#ffffff" rx="0"/>
                <rect x="28" y="35" width="4" height="4" fill="#ffffff" rx="0"/>
              </g>

              <!-- 脸颊红晕 (更大) -->
              <g opacity="0.7">
                <rect x="6" y="22" width="6" height="6" fill="#f093fb" rx="0"/>
                <rect x="36" y="22" width="6" height="6" fill="#f093fb" rx="0"/>
              </g>

              <!-- 像素化装饰点 (更多更大) -->
              <g fill="#667eea" opacity="0.8">
                <rect x="18" y="10" width="4" height="4" rx="0"/>
                <rect x="26" y="10" width="4" height="4" rx="0"/>
                <rect x="8" y="28" width="4" height="4" rx="0"/>
                <rect x="36" y="28" width="4" height="4" rx="0"/>
                <rect x="22" y="6" width="4" height="4" rx="0"/>
              </g>
            </svg>
          </div>
          <div class="brand-text">
            <h1 class="brand-title">SF-Chain</h1>
            <p class="brand-subtitle">AI智能调度框架</p>
          </div>
        </div>
      </div>

      <div class="header-right">
        <div class="auth-section">
          <div class="connection-status" :class="{ 'connected': currentToken }">
            <div class="status-indicator">
              <div class="status-pulse"></div>
            </div>
            <span class="status-label">{{ currentToken ? '已连接' : '未连接' }}</span>
          </div>

          <div class="auth-controls">
            <div class="token-input-group">
              <input
                v-model="tokenInput"
                type="password"
                class="token-input"
                placeholder="输入访问令牌"
                @keyup.enter="saveToken"
              />
              <div class="input-actions">
                <button
                  class="action-btn save-btn"
                  @click="saveToken"
                  :disabled="!tokenInput.trim()"
                  title="保存令牌"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd" />
                  </svg>
                </button>
                <button
                  class="action-btn refresh-btn"
                  @click="refreshData"
                  :disabled="!currentToken"
                  title="刷新数据"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor">
                    <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd" />
                  </svg>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { getAuthToken, setAuthToken } from '@/services/apiUtils'
import { toast } from '@/utils/toast'

// Props
interface Props {
  onRefresh?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  onRefresh: () => {}
})

// Token相关状态
const tokenInput = ref('')
const currentToken = ref(getAuthToken())

// 保存Token
const saveToken = () => {
  if (!tokenInput.value.trim()) {
    toast.error({
      title: '输入错误',
      message: '请输入有效的访问令牌',
      duration: 3000
    })
    return
  }

  setAuthToken(tokenInput.value.trim())
  currentToken.value = tokenInput.value.trim()
  tokenInput.value = ''

  toast.success({
    title: '令牌已保存',
    message: '访问令牌已成功保存并生效',
    duration: 3000
  })

  // 保存token后自动刷新数据
  refreshData()
}

// 刷新数据
const refreshData = async () => {
  if (!currentToken.value) {
    toast.error({
      title: '未设置令牌',
      message: '请先设置访问令牌',
      duration: 3000
    })
    return
  }

  try {
    await props.onRefresh()
    toast.success({
      title: '数据已刷新',
      message: '系统数据已成功更新',
      duration: 3000
    })
  } catch (error) {
    console.error('Refresh data error:', error)
  }
}
</script>

<style scoped>
.header-bar {
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.98) 0%,
    rgba(248, 250, 252, 0.95) 100%);
  backdrop-filter: blur(24px) saturate(200%);
  border-radius: 16px;
  padding: 1.25rem 2rem;
  margin-bottom: 1.5rem;
  box-shadow:
    0 8px 32px rgba(102, 126, 234, 0.08),
    0 4px 16px rgba(0, 0, 0, 0.04),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(102, 126, 234, 0.1);
  position: relative;
  overflow: hidden;
  height: 96px;
}

.header-bar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg,
    #667eea 0%,
    #764ba2 25%,
    #f093fb 50%,
    #4facfe 75%,
    #00f2fe 100%);
  opacity: 0.9;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 2rem;
  height: 100%;
}

.header-left {
  flex: 0 0 auto;
}

.brand-section {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.brand-logo {
  width: 52px;
  height: 52px;
  background: linear-gradient(135deg,
    rgba(102, 126, 234, 0.1) 0%,
    rgba(118, 75, 162, 0.1) 50%,
    rgba(240, 147, 251, 0.1) 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 4px 20px rgba(102, 126, 234, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(102, 126, 234, 0.2);
  position: relative;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.brand-logo:hover {
  transform: translateY(-1px) scale(1.02);
  box-shadow:
    0 8px 32px rgba(102, 126, 234, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
}

.logo-svg {
  width: 36px;
  height: 36px;
  transition: all 0.4s ease;
}

.brand-logo:hover .logo-svg {
  transform: scale(1.05) rotate(3deg);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
}

.brand-title {
  font-size: 1.875rem;
  font-weight: 700;
  background: linear-gradient(135deg,
    #1e293b 0%,
    #667eea 50%,
    #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
  letter-spacing: -0.025em;
  line-height: 1.1;
}

.brand-subtitle {
  font-size: 0.8rem;
  color: #64748b;
  font-weight: 500;
  margin: 0;
  letter-spacing: 0.02em;
  opacity: 0.8;
}

.header-right {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}

.auth-section {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.625rem 1rem;
  background: rgba(239, 68, 68, 0.06);
  border: 1px solid rgba(239, 68, 68, 0.12);
  border-radius: 12px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.connection-status.connected {
  background: rgba(16, 185, 129, 0.06);
  border-color: rgba(16, 185, 129, 0.12);
}

.status-indicator {
  position: relative;
  width: 10px;
  height: 10px;
}

.status-pulse {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: #ef4444;
  animation: pulse 2s infinite;
}

.connection-status.connected .status-pulse {
  background: #10b981;
}

@keyframes pulse {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.6;
  }
  100% {
    transform: scale(0.8);
    opacity: 1;
  }
}

.status-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: #ef4444;
  transition: color 0.3s ease;
}

.connection-status.connected .status-label {
  color: #10b981;
}

.auth-controls {
  display: flex;
  align-items: center;
}

.token-input-group {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.95);
  border: 1.5px solid rgba(226, 232, 240, 0.5);
  border-radius: 16px;
  padding: 0.375rem;
  transition: all 0.3s ease;
  backdrop-filter: blur(12px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.token-input-group:focus-within {
  border-color: #6366f1;
  box-shadow:
    0 0 0 3px rgba(99, 102, 241, 0.08),
    0 4px 20px rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.token-input {
  flex: 1;
  min-width: 200px;
  padding: 0.75rem 1rem;
  border: none;
  background: transparent;
  font-size: 0.8rem;
  color: #1e293b;
  outline: none;
  font-weight: 500;
  letter-spacing: 0.02em;
}

.token-input::placeholder {
  color: #94a3b8;
  font-weight: 400;
}

.input-actions {
  display: flex;
  gap: 0.375rem;
}

.action-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.action-btn svg {
  width: 16px;
  height: 16px;
  transition: all 0.3s ease;
}

.save-btn {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  color: white;
  box-shadow: 0 2px 12px rgba(99, 102, 241, 0.25);
}

.save-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(99, 102, 241, 0.35);
}

.save-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.refresh-btn {
  background: linear-gradient(135deg, #10b981 0%, #06b6d4 100%);
  color: white;
  box-shadow: 0 2px 12px rgba(16, 185, 129, 0.25);
}

.refresh-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.35);
}

.refresh-btn:hover:not(:disabled) svg {
  transform: rotate(180deg);
}

.refresh-btn:disabled {
  background: #e2e8f0;
  color: #94a3b8;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .header-content {
    flex-direction: column;
    gap: 1.5rem;
  }

  .header-bar {
    height: auto;
    padding: 1.25rem 1.5rem;
  }

  .header-right {
    justify-content: center;
  }

  .auth-section {
    flex-direction: column;
    gap: 1rem;
  }

  .token-input-group {
    width: 100%;
    max-width: 360px;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 1rem 1.25rem;
  }

  .brand-title {
    font-size: 1.5rem;
  }

  .brand-logo {
    width: 40px;
    height: 40px;
  }

  .logo-svg {
    width: 28px;
    height: 28px;
  }

  .token-input {
    min-width: 160px;
  }

  .auth-section {
    width: 100%;
  }

  .token-input-group {
    max-width: none;
  }
}
</style>
