import { createApp, type App } from 'vue'
import Toast from '@/components/Toast.vue'

interface ToastOptions {
  type?: 'success' | 'error' | 'warning' | 'info'
  title?: string
  message: string
  duration?: number
  closable?: boolean
  showProgress?: boolean
  position?: 'top' | 'center' | 'bottom'
  offset?: number
}

// 预设的停留时间配置
const DURATION_PRESETS = {
  success: 3000,
  error: 6000,
  warning: 4000,
  info: 3000,
  short: 2000,
  medium: 4000,
  long: 6000,
  persistent: 0 // 不自动关闭
}

class ToastManager {
  private toasts: { app: App; container: HTMLElement; id: string }[] = []
  private toastCounter = 0

  private show(options: ToastOptions) {
    const id = `toast-${++this.toastCounter}`
    const container = document.createElement('div')

    // 计算位置偏移
    const offset = this.calculateOffset(options.position || 'top')

    const app = createApp(Toast, {
      ...options,
      id,
      offset,
      onClose: () => {
        this.remove(app)
      }
    })

    app.mount(container)
    this.toasts.push({ app, container, id })

    return app
  }

  private calculateOffset(position: string): number {
    const samePositionToasts = this.toasts.filter(toast => {
      // 这里需要从toast实例中获取position，简化处理
      return true
    })
    return samePositionToasts.length * 80 // 每个toast间隔80px
  }

  private remove(targetApp: App) {
    const index = this.toasts.findIndex(({ app }) => app === targetApp)
    if (index > -1) {
      const { app, container } = this.toasts[index]
      this.toasts.splice(index, 1)
      app.unmount()
      if (container.parentNode) {
        container.parentNode.removeChild(container)
      }
      // 重新计算其他toast的位置
      this.updatePositions()
    }
  }

  private updatePositions() {
    // 更新剩余toast的位置
    this.toasts.forEach((toast, index) => {
      // 这里可以通过expose的方法更新位置
    })
  }

  // 支持预设时长
  success(messageOrOptions: string | ToastOptions, title?: string, options?: Partial<ToastOptions>) {
    if (typeof messageOrOptions === 'string') {
      return this.show({
        type: 'success',
        title: title || '成功',
        message: messageOrOptions,
        duration: DURATION_PRESETS.success,
        ...options
      })
    } else {
      return this.show({
        type: 'success',
        title: messageOrOptions.title || '成功',
        duration: DURATION_PRESETS.success,
        ...messageOrOptions
      })
    }
  }

  error(messageOrOptions: string | ToastOptions, title?: string, options?: Partial<ToastOptions>) {
    if (typeof messageOrOptions === 'string') {
      return this.show({
        type: 'error',
        title: title || '错误',
        message: messageOrOptions,
        duration: DURATION_PRESETS.error,
        ...options
      })
    } else {
      return this.show({
        type: 'error',
        title: messageOrOptions.title || '错误',
        duration: DURATION_PRESETS.error,
        ...messageOrOptions
      })
    }
  }

  warning(messageOrOptions: string | ToastOptions, title?: string, options?: Partial<ToastOptions>) {
    if (typeof messageOrOptions === 'string') {
      return this.show({
        type: 'warning',
        title: title || '警告',
        message: messageOrOptions,
        duration: DURATION_PRESETS.warning,
        ...options
      })
    } else {
      return this.show({
        type: 'warning',
        title: messageOrOptions.title || '警告',
        duration: DURATION_PRESETS.warning,
        ...messageOrOptions
      })
    }
  }

  info(messageOrOptions: string | ToastOptions, title?: string, options?: Partial<ToastOptions>) {
    if (typeof messageOrOptions === 'string') {
      return this.show({
        type: 'info',
        title: title || '提示',
        message: messageOrOptions,
        duration: DURATION_PRESETS.info,
        ...options
      })
    } else {
      return this.show({
        type: 'info',
        title: messageOrOptions.title || '提示',
        duration: DURATION_PRESETS.info,
        ...messageOrOptions
      })
    }
  }

  // 新增便捷方法
  quick(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
    return this.show({
      type,
      message,
      duration: DURATION_PRESETS.short,
      showProgress: false,
      closable: false
    })
  }

  persistent(message: string, title?: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
    return this.show({
      type,
      title,
      message,
      duration: 0,
      closable: true
    })
  }

  clear() {
    this.toasts.forEach(({ app }) => {
      this.remove(app)
    })
  }
}

export const toast = new ToastManager()
export type { ToastOptions }
export { DURATION_PRESETS }
