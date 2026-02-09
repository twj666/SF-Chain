<template>
  <Teleport to="body">
    <Transition
      name="toast"
      enter-active-class="toast-enter-active"
      leave-active-class="toast-leave-active"
      enter-from-class="toast-enter-from"
      leave-to-class="toast-leave-to"
      @enter="onEnter"
      @leave="onLeave"
    >
      <div
        v-if="visible"
        class="toast-overlay"
        :class="`toast-${type}`"
        :style="overlayStyle"
        @mouseenter="pause"
        @mouseleave="resume"
      >
        <div class="toast-container" :class="`toast-${type}`">
          <div class="toast-content">
            <div class="toast-icon-wrapper">
              <div class="toast-icon">
                <component :is="iconComponent" />
              </div>
            </div>
            <div class="toast-message">
              <div class="toast-title" v-if="title">{{ title }}</div>
              <div class="toast-text">{{ message }}</div>
            </div>
            <button v-if="closable" @click="close" class="toast-close">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                <path d="M9 7.586L13.657 3l1.414 1.414L10.414 9l4.657 4.586-1.414 1.414L9 10.414 4.343 15 3 13.657 7.586 9 3 4.343 4.343 3 9 7.586z"/>
              </svg>
            </button>
          </div>
          <div v-if="showProgress && duration > 0" class="toast-progress">
            <div
              class="toast-progress-bar"
              :style="{
                width: progressWidth + '%',
                animationDuration: duration + 'ms',
                animationPlayState: isPaused ? 'paused' : 'running'
              }"
            ></div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

// 图标组件
const SuccessIcon = {
  template: `
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
    </svg>
  `
}

const ErrorIcon = {
  template: `
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
    </svg>
  `
}

const WarningIcon = {
  template: `
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/>
    </svg>
  `
}

const InfoIcon = {
  template: `
    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
      <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>
    </svg>
  `
}

interface Props {
  type?: 'success' | 'error' | 'warning' | 'info'
  title?: string
  message: string
  duration?: number
  closable?: boolean
  showProgress?: boolean
  position?: 'top' | 'center' | 'bottom'
  offset?: number
  id?: string
}

interface Emits {
  close: []
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info',
  duration: 2000,
  closable: true,
  showProgress: true,
  position: 'center',
  offset: 0
})

const emit = defineEmits<Emits>()

const visible = ref(false)
const progressWidth = ref(100)
const isPaused = ref(false)
let timer: NodeJS.Timeout | null = null
let startTime: number = 0
let remainingTime: number = 0

// 计算位置样式
const overlayStyle = computed(() => {
  const baseStyle: Record<string, string> = {
    position: 'fixed',
    zIndex: '9999',
    pointerEvents: 'none'
  }

  switch (props.position) {
    case 'top':
      return {
        ...baseStyle,
        top: `${20 + props.offset}px`,
        left: '50%',
        transform: 'translateX(-50%)'
      }
    case 'center':
      return {
        ...baseStyle,
        top: '50%',
        left: '50%',
        transform: `translate(-50%, calc(-50% + ${props.offset}px))`
      }
    case 'bottom':
      return {
        ...baseStyle,
        bottom: `${20 + props.offset}px`,
        left: '50%',
        transform: 'translateX(-50%)'
      }
    default:
      return baseStyle
  }
})

// 图标组件
const iconComponent = computed(() => {
  switch (props.type) {
    case 'success': return SuccessIcon
    case 'error': return ErrorIcon
    case 'warning': return WarningIcon
    case 'info': return InfoIcon
    default: return InfoIcon
  }
})

const show = () => {
  visible.value = true
  startTime = Date.now()
  remainingTime = props.duration

  if (props.duration > 0) {
    startTimer()
  }
}

const startTimer = () => {
  if (timer) clearTimeout(timer)

  timer = setTimeout(() => {
    close()
  }, remainingTime)

  startTime = Date.now()
}

const close = () => {
  visible.value = false
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
  emit('close')
}

const pause = () => {
  if (timer && !isPaused.value) {
    isPaused.value = true
    clearTimeout(timer)
    remainingTime = remainingTime - (Date.now() - startTime)
  }
}

const resume = () => {
  if (isPaused.value && props.duration > 0) {
    isPaused.value = false
    startTimer()
  }
}

const onEnter = (el: Element) => {
  // 进入动画完成后的回调
}

const onLeave = (el: Element) => {
  // 离开动画完成后的回调
}

onMounted(() => {
  show()
})

onUnmounted(() => {
  if (timer) clearTimeout(timer)
})

defineExpose({
  show,
  close,
  pause,
  resume
})
</script>

<style scoped>
.toast-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  pointer-events: none;
}

.toast-container {
  min-width: 320px;
  max-width: 480px;
  width: auto;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.15),
    0 10px 20px -8px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  overflow: hidden;
  pointer-events: auto;
  border: 1px solid rgba(255, 255, 255, 0.3);
  transform: translateZ(0); /* 启用硬件加速 */
}

.toast-content {
  display: flex;
  align-items: flex-start;
  padding: 20px;
  gap: 14px;
}

.toast-icon-wrapper {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.toast-icon-wrapper::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  opacity: 0.12;
  background: currentColor;
  transform: scale(0);
  animation: iconPulse 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

@keyframes iconPulse {
  to {
    transform: scale(1);
  }
}

.toast-icon {
  position: relative;
  z-index: 1;
  transform: scale(0);
  animation: iconScale 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) 0.1s forwards;
}

@keyframes iconScale {
  to {
    transform: scale(1);
  }
}

.toast-message {
  flex: 1;
  min-width: 0;
  padding-top: 2px;
}

.toast-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  line-height: 1.4;
  color: #1a1a1a;
  opacity: 0;
  transform: translateY(8px);
  animation: slideInUp 0.4s cubic-bezier(0.4, 0, 0.2, 1) 0.2s forwards;
}

.toast-text {
  font-size: 14px;
  line-height: 1.5;
  color: #666;
  opacity: 0;
  transform: translateY(8px);
  animation: slideInUp 0.4s cubic-bezier(0.4, 0, 0.2, 1) 0.3s forwards;
}

@keyframes slideInUp {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.toast-close {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.04);
  cursor: pointer;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: 8px;
  opacity: 0;
  transform: scale(0.8);
  animation: fadeInScale 0.3s cubic-bezier(0.4, 0, 0.2, 1) 0.4s forwards;
}

@keyframes fadeInScale {
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.toast-close:hover {
  background: rgba(0, 0, 0, 0.08);
  color: #666;
  transform: scale(1.05);
}

.toast-close:active {
  transform: scale(0.95);
}

.toast-progress {
  height: 3px;
  background: rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.toast-progress-bar {
  height: 100%;
  width: 100%;
  transform-origin: left;
  animation: progressShrink linear;
}

@keyframes progressShrink {
  from {
    transform: scaleX(1);
  }
  to {
    transform: scaleX(0);
  }
}

/* 类型特定样式 */
.toast-success {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.03), rgba(52, 211, 153, 0.03));
  border: 1px solid rgba(16, 185, 129, 0.15);
}

.toast-success .toast-icon-wrapper {
  color: #10b981;
}

.toast-success .toast-title {
  color: #065f46;
}

.toast-success .toast-progress-bar {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.toast-error {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.03), rgba(248, 113, 113, 0.03));
  border: 1px solid rgba(239, 68, 68, 0.15);
}

.toast-error .toast-icon-wrapper {
  color: #ef4444;
}

.toast-error .toast-title {
  color: #7f1d1d;
}

.toast-error .toast-progress-bar {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.toast-warning {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.03), rgba(251, 191, 36, 0.03));
  border: 1px solid rgba(245, 158, 11, 0.15);
}

.toast-warning .toast-icon-wrapper {
  color: #f59e0b;
}

.toast-warning .toast-title {
  color: #78350f;
}

.toast-warning .toast-progress-bar {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.toast-info {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.03), rgba(96, 165, 250, 0.03));
  border: 1px solid rgba(59, 130, 246, 0.15);
}

.toast-info .toast-icon-wrapper {
  color: #3b82f6;
}

.toast-info .toast-title {
  color: #1e3a8a;
}

.toast-info .toast-progress-bar {
  background: linear-gradient(90deg, #3b82f6, #60a5fa);
}

/* 更丝滑的动画效果 */
.toast-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.toast-leave-active {
  transition: all 0.35s cubic-bezier(0.4, 0, 1, 1);
}

.toast-enter-from {
  opacity: 0;
  transform: translateX(-50%) scale(0.85) translateY(-30px);
  filter: blur(4px);
}

.toast-leave-to {
  opacity: 0;
  transform: translateX(-50%) scale(0.95) translateY(-15px);
  filter: blur(2px);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .toast-container {
    min-width: auto;
    max-width: calc(100vw - 32px);
    margin: 0 16px;
  }

  .toast-content {
    padding: 18px;
    gap: 12px;
  }

  .toast-icon-wrapper {
    width: 40px;
    height: 40px;
  }

  .toast-title {
    font-size: 14px;
  }

  .toast-text {
    font-size: 13px;
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .toast-container {
    background: rgba(30, 30, 30, 0.98);
    border: 1px solid rgba(255, 255, 255, 0.08);
  }

  .toast-title {
    color: #f5f5f5;
  }

  .toast-text {
    color: #a3a3a3;
  }

  .toast-close {
    background: rgba(255, 255, 255, 0.08);
    color: #a3a3a3;
  }

  .toast-close:hover {
    background: rgba(255, 255, 255, 0.15);
    color: #f5f5f5;
  }
}
</style>
