<template>
  <view class="badge-container">
		<global-loading />
    <view class="badge-icon">
      <view class="circle"></view>
    </view>
    <view class="badge-content" :class="{ 'expanded': expanded }">
      <!-- eslint-disable-next-line vue/no-v-text-v-html-on-component -->
      <div 
        class="badgeText" 
        :class="{ clamped: !expanded && line }"
        :style="line ? '-webkit-line-clamp: ' + line : ''"
        v-html="text"
      ></div>
      <view 
        v-if="showButton" 
        class="more-btn" 
        @click="toggle"
      >
        <text v-if="currentLang === 'en'">{{ expanded ? 'Collapse' : 'Read More' }}</text>
        <text v-else>{{ expanded ? '收起' : '查看更多' }}</text>
      </view>
    </view>
  </view>
</template>

<script>
export default {
  props: ['text', 'currentLang', 'line'],
  data() {
    return {
      expanded: false,
      showButton: false
    }
  },
  watch: {
    text: {
      handler() {
        this.expanded = false;
        this.$nextTick(() => {
          this.checkOverflow();
        });
      },
      immediate: true
    }
  },
  methods: {
    checkOverflow() {
      if (this.text && this.text.length > 50) {
        this.showButton = true;
      } else {
        this.showButton = false;
      }
    },
    toggle() {
      this.expanded = !this.expanded;
    }
  }
}
</script>

<style scoped>
.badge-container {
  display: flex;
  gap: 16rpx;
  padding: 24rpx;
  background: linear-gradient(135deg, rgba(255, 115, 141, 0.08) 0%, rgba(255, 163, 115, 0.08) 100%);
  border-radius: 16rpx;
  border: 1px solid rgba(255, 115, 141, 0.15);
  margin-top: 24rpx;
}
.badge-icon {
  flex-shrink: 0;
  width: 32rpx;
  height: 32rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff738d 0%, #ffa373 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 4rpx;
}
.badge-icon .circle {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #fff;
}
.badge-content {
  flex: 1;
}
.badgeText {
  font-size: 26rpx;
  line-height: 1.6;
  color: #d94a65;
  font-weight: 500;
  word-break: break-word;
}
.badgeText.clamped {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.more-btn {
  display: inline-block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #d94a65;
  cursor: pointer;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 4rpx;
}
</style>
