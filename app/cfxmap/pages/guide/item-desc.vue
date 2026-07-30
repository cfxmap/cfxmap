<template>
  <view :class="expanded ? '' : 'desc'">
		<global-loading />
    <!-- eslint-disable-next-line vue/no-v-text-v-html-on-component -->
    <div class="descTitle" v-html="title"></div>
    <!-- eslint-disable-next-line vue/no-v-text-v-html-on-component -->
    <div 
      class="descText" 
      :class="{ clamped: !expanded }"
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
</template>

<script>
export default {
  props: ['text', 'title', 'currentLang'],
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
      // In UniApp, we need to use createSelectorQuery to check height
      const query = uni.createSelectorQuery().in(this);
      query.select('.descText').boundingClientRect(data => {
        if (data) {
          // A simple heuristic: if it's clamped, we show the button
          // Or we can just always show it if text length is long
          // Since we can't easily get scrollHeight vs clientHeight in standard way,
          // we'll just check if text length > 60 chars as a fallback, or use line-clamp.
          if (this.text && this.text.length > 50) {
            this.showButton = true;
          } else {
            this.showButton = false;
          }
        }
      }).exec();
    },
    toggle() {
      this.expanded = !this.expanded;
    }
  }
}
</script>

<style scoped>
.descTitle {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-strong, #1a2b55);
  margin-bottom: 8rpx;
  display: block;
}
.descText {
  font-size: 28rpx;
  line-height: 1.6;
  color: var(--text-soft, #5b6c8f);
  word-break: break-word;
}
.descText.clamped {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.more-btn {
  display: inline-block;
  margin-top: 8rpx;
  font-size: 28rpx;
  color: var(--primary, #2C5FFF);
  cursor: pointer;
  font-weight: 500;
}
</style>
