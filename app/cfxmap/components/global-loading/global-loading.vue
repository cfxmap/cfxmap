<template>
  <view v-if="isLoading" class="global-loading-mask" :class="{ 'is-dark': isDarkMode }">
    <view class="loading-content">
      <image class="loading-img" :src="$staticUrl('/static/loading.svg')" mode="aspectFit"></image>
      <text class="loading-text">CFXMAP</text>
    </view>
  </view>
</template>

<script>
import { getActiveRequestCount } from '../../utils/request.js'

export default {
  name: 'global-loading',
  data() {
    return {
      isLoading: true,
      isInitialLoad: true
    }
  },
  computed: {
    isDarkMode() {
      return this.$store ? this.$store.getters.isDarkMode : false
    }
  },
  mounted() {
    uni.$on('showGlobalLoading', this.showLoading)
    uni.$on('hideGlobalLoading', this.hideLoading)
    uni.$on('globalRequestStart', this.onRequestStart)
    uni.$on('globalRequestEnd', this.onRequestEnd)
    
    // 页面加载后立即判断是否有进行中的接口请求
    // 给一点延迟（例如 300ms），让 onLoad 里的接口有时间发出去
    this.staticTimer = setTimeout(() => {
      if (getActiveRequestCount() === 0) {
        this.hideLoading()
      }
    }, 300)

    // 兜底策略：如果接口一直不返回（或发生未知异常），最多展示 5 秒后强制关闭
    this.fallbackTimer = setTimeout(() => {
      this.hideLoading()
    }, 5000)
  },
  beforeDestroy() {
    this.cleanup()
  },
  unmounted() {
    this.cleanup()
  },
  methods: {
    cleanup() {
      uni.$off('showGlobalLoading', this.showLoading)
      uni.$off('hideGlobalLoading', this.hideLoading)
      uni.$off('globalRequestStart', this.onRequestStart)
      uni.$off('globalRequestEnd', this.onRequestEnd)
      if (this.staticTimer) clearTimeout(this.staticTimer)
      if (this.fallbackTimer) clearTimeout(this.fallbackTimer)
    },
    showLoading() {
      this.isLoading = true
    },
    hideLoading() {
      this.isLoading = false
      this.isInitialLoad = false
    },
    onRequestStart() {
      if (this.isInitialLoad) {
        this.isLoading = true
      }
    },
    onRequestEnd() {
      if (getActiveRequestCount() === 0) {
        this.hideLoading()
      }
    }
  }
}
</script>

<style scoped>
.global-loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999999;
  background-color: #ffffff; /* 浅色模式背景 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: background-color 0.3s;
}

/* 深色模式背景 */
.global-loading-mask.is-dark {
  background-color: #101d36; /* 与 App.vue 的深色背景色接近 */
}

.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.loading-img {
  width: 100px;
  height: 100px;
  margin-bottom: 16px;
  transition: all 0.3s;
}

.loading-text {
  font-size: 16px;
  color: #333333;
  font-weight: bold;
  letter-spacing: 2px;
}

/* 深色模式下反转 SVG 颜色和文字颜色 */
.global-loading-mask.is-dark .loading-img {
  filter: invert(1);
}

.global-loading-mask.is-dark .loading-text {
  color: #ffffff;
}
</style>
