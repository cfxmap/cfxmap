<template>
  <view class="container" :class="themeClass">
		<global-loading />
    <view class="bg"></view>
    <view class="grid"></view>
    
    <scroll-view class="page-scroll" scroll-y :scroll-into-view="scrollIntoView" scroll-with-animation>
      <view class="ai-bg"></view>
      
      <view class="hero" id="eco">
        <view class="hero-content">
          <text class="hero-title">{{ currentConfig.heroTitle }}</text>
          <text class="hero-subtitle">{{ currentConfig.heroSubtitle }}</text>
          <view class="cta" @click="scrollTo('deposit-1')">{{ currentConfig.heroBtnText }}</view>
        </view>
      </view>

      <view class="section-header">
        <view class="header-inner">
          <text class="h2">{{ currentConfig.firstTitle }}</text>
          <text class="subTitle">{{ currentConfig.firstSubtitle }}</text>
        </view>
      </view>

      <view class="panel" id="eco-panel">
        <view v-for="item in contentList" :key="item.id" class="section" :id="item.id">
          <view class="section-title">
            <text>{{ item.title }}</text>
          </view>
          <text v-if="item.subtitle" class="section-subtitle">{{ item.subtitle }}</text>
          
          <view class="info-list">
            <view v-for="(sitem, sindex) in item.content" :key="sindex" class="info-item" :id="item.id + '-' + (sindex + 1)">
              <view class="item-title">
                <text class="num">0{{ sindex + 1 }}</text>
                <text>{{ sitem.title }}</text>
              </view>
              
              <view v-for="(titem, tindex) in sitem.list" :key="tindex" class="single-item">
                <view class="item-info">
                  <!-- eslint-disable-next-line vue/no-v-text-v-html-on-component -->
                  <div class="num" v-html="titem.title"></div>
                  
                  <item-desc 
                    v-if="!titem.showDesc"
                    :title="currentLang === 'en' ? 'Description' : '描述'" 
                    :current-lang="currentLang"
                    :text="titem.desc"
                  ></item-desc>
                  
                  <view v-else class="desc-inline">
                    <text class="descTitle">{{ currentLang === 'en' ? 'Description' : '描述' }}</text>
                    <!-- eslint-disable-next-line vue/no-v-text-v-html-on-component -->
                    <div class="descText" v-html="titem.desc"></div>
                  </view>

                  <item-desc 
                    v-if="titem.explain"
                    :title="currentLang === 'en' ? 'Explanation' : currentLang === 'zh-TW' ? '說明' : '说明'"
                    :current-lang="currentLang" 
                    :text="titem.explain"
                  ></item-desc>
                </view>
                
                <view class="actions">
                  <view 
                    v-for="(action, aindex) in titem.btnList" 
                    :key="aindex"
                    class="btn" 
                    :class="action.class"
                    @click="handleOutboundLink(action)"
                  >
                    {{ action.text }}
                  </view>
                </view>
                
                <badge-desc 
                  v-if="titem.badge" 
                  :current-lang="currentLang" 
                  :line="titem.line"
                  :text="titem.badge"
                ></badge-desc>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="section-header marginTOP">
        <view class="header-inner">
          <text class="h2">{{ currentConfig.secondTitle }}</text>
          <text class="subTitle">{{ currentConfig.secondSubtitle }}</text>
        </view>
      </view>

      <view class="panel" id="tool-panel">
        <view v-for="item in toolsList" :key="item.id" class="tool-group">
          <view class="toolTitle" :id="item.id">{{ item.title }}</view>
          <view class="toolList">
            <view v-for="(sitem, sindex) in item.list" :key="sindex" class="toolItem">
              <text class="item-title">{{ sitem.text }}</text>
              <view class="btn" :class="sitem.class" @click="handleOutboundLink(sitem)">
                <text v-if="currentLang === 'en'">visit</text>
                <text v-else-if="currentLang === 'zh-TW'">訪問</text>
                <text v-else>访问</text>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view class="bottom-spacer"></view>
    </scroll-view>

    <bottom-nav current="guide" />
    <!-- #ifdef H5 -->
    <h5-add-to-home-tip bottomNav />
    <!-- #endif -->
  </view>
</template>

<script>
import { mapState, mapGetters } from 'vuex'
import BottomNav from '@/components/bottom-nav.vue'
// #ifdef H5
import H5AddToHomeTip from '@/components/h5-add-to-home-tip.vue'
// #endif
import ItemDesc from './item-desc.vue'
import BadgeDesc from './badge-desc.vue'
import { configInfo as localConfigInfo } from './config.js'
import { api } from '@/utils/request'

export default {
  components: {
    BottomNav,
    // #ifdef H5
    H5AddToHomeTip,
    // #endif
    ItemDesc,
    BadgeDesc
  },
  data() {
    return {
      langLabels: {
        'zh-CN': '简体中文',
        'zh-TW': '繁體中文',
        'en': 'English'
      },
      currentLang: 'zh-CN',
      showLangMenu: false,
      scrollIntoView: '',
      remoteConfig: null
    }
  },
  computed: {
    ...mapState(['language']),
    ...mapGetters(['i18n']),
    currentConfig() {
      return this.remoteConfig || localConfigInfo[this.currentLang] || localConfigInfo['en']
    },
    contentList() {
      return this.currentConfig.contentList || []
    },
    toolsList() {
      return this.currentConfig.toolList || []
    }
  },
  watch: {
    language(newVal) {
      const next = this.guideLangFromStore(newVal)
      if (next !== this.currentLang) {
        this.currentLang = next
        this.fetchConfig()
      }
    }
  },
  onLoad() {
    this.currentLang = this.guideLangFromStore(this.language)
    this.fetchConfig()
  },
  onShow() {
    uni.hideTabBar()
    const next = this.guideLangFromStore(this.language)
    if (next !== this.currentLang) {
      this.currentLang = next
      this.fetchConfig()
    }
  },
  methods: {
    guideLangFromStore(storeLang) {
      if (storeLang === 'English') return 'en'
      if (storeLang === '中文') return 'zh-CN'
      return 'zh-CN'
    },
    async fetchConfig() {
      try {
        const res = await api.getGuideConfig(this.currentLang);
        if (res) {
          if (res.content) {
            const parsed = JSON.parse(res.content);
            if (Object.keys(parsed).length > 0) {
              this.remoteConfig = parsed;
            } else {
              this.remoteConfig = null;
            }
          } else if (res.name || res.heroTitle) {
            const repairGuideHtmlJson = (s) =>
              typeof s === 'string' ? s.replace(/<span class="rate">/g, '<span class=\\"rate\\">') : s;
            const parseList = (v) => {
              if (v == null || v === '') return [];
              if (Array.isArray(v)) return v;
              if (typeof v === 'string') {
                const tryParse = (str) => {
                  try {
                    return JSON.parse(str);
                  } catch (e) {
                    return null;
                  }
                };
                let out = tryParse(v);
                if (out == null) out = tryParse(repairGuideHtmlJson(v));
                return out || [];
              }
              return [];
            };
            this.remoteConfig = {
              ...res,
              contentList: parseList(res.contentList),
              menuList: parseList(res.menuList),
              toolList: parseList(res.toolList)
            };
          } else {
            this.remoteConfig = null;
          }
        } else {
          this.remoteConfig = null;
        }
      } catch (e) {
        console.error('Failed to fetch guide config:', e);
        this.remoteConfig = null;
      }
    },
    toggleLangMenu() {
      this.showLangMenu = !this.showLangMenu;
    },
    changeLang(lang) {
      this.currentLang = lang;
      this.showLangMenu = false;
      
      if (lang === 'en') {
        this.$store.commit('SET_LANGUAGE', 'English');
      } else {
        this.$store.commit('SET_LANGUAGE', '中文');
      }
      
      this.fetchConfig();
    },
    scrollTo(id) {
      this.scrollIntoView = id;
      // Reset after a short delay so it can be triggered again
      setTimeout(() => {
        this.scrollIntoView = '';
      }, 500);
    },
    handleOutboundLink(action) {
      if (!action.link) return;
      
      if (action.link.startsWith('#')) {
        this.scrollTo(action.link.substring(1));
        return;
      }

      uni.showModal({
        title: this.i18n.leavingPageTitle,
        content: this.i18n.leavingPageMessage,
        confirmText: this.i18n.continueToExternal,
        cancelText: this.i18n.cancelBtn,
        success: (res) => {
          if (res.confirm) {
            // #ifdef H5
            window.open(action.link, '_blank');
            // #endif
            // #ifdef APP-PLUS
            plus.runtime.openURL(action.link);
            // #endif
            // #ifdef MP
            uni.setClipboardData({
              data: action.link,
              success: () => {
                uni.showToast({
                  title: this.i18n.linkCopied,
                  icon: 'none'
                });
              }
            });
            // #endif
          }
        }
      });
    }
  }
}
</script>

<style scoped>
/* Core Variables mapped to the original HTML */
.container {
  --primary: #33cfff;
  --text: #1b2e5b;
  --text-strong: #1a2b55;
  --text-soft: #5b6c8f;

  min-height: 100vh;
  color: var(--text);
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
  position: relative;
}

.theme-dark.container {
  background: var(--page-bg-gradient);
  --text: #cbd5e1;
  --text-strong: #f1f5f9;
  --text-soft: #94a3b8;
}

.bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
}

.bg::before,
.bg::after {
  content: "";
  position: absolute;
  width: 540px;
  height: 540px;
  border-radius: 50%;
  filter: blur(60px);
}

.bg::before {
  left: -120px;
  top: 60px;
  background: radial-gradient(closest-side, rgba(51, 207, 255, 0.35) 0%, rgba(240, 252, 255, 0.5) 60%);
  opacity: .45;
}

.bg::after {
  right: -80px;
  top: 140px;
  background: radial-gradient(closest-side, rgba(91, 221, 255, 0.4) 0%, rgba(221, 250, 255, 0.45) 60%);
  opacity: .45;
}

.theme-dark .bg::before,
.theme-dark .bg::after {
  opacity: 0.22;
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 20;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
}

.nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 32rpx;
  /* Use safe area for top notch */
  padding-top: calc(20rpx + env(safe-area-inset-top));
}

.brand {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.logo {
  width: 56rpx;
  height: 56rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #33cfff 0%, #5bddff 60%);
  box-shadow: 0 8px 24px rgba(51, 207, 255, .35);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 700;
}

.brand .text {
  font-weight: 700;
  font-size: 32rpx;
  color: var(--text-strong);
}

.nav-right {
  display: flex;
  align-items: center;
}

.lang-switch {
  position: relative;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-soft);
  padding: 8rpx 16rpx;
  border-radius: 12rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.lang-icon {
  font-size: 20rpx;
  transition: transform 0.2s;
}

.lang-icon.open {
  transform: rotate(180deg);
}

.lang-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 16rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
  padding: 8rpx;
  min-width: 200rpx;
  z-index: 100;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.lang-option {
  padding: 16rpx 24rpx;
  font-size: 28rpx;
  border-radius: 8rpx;
  color: var(--text-strong);
}

.lang-option.active {
  color: var(--primary);
  background: rgba(51, 207, 255, 0.12);
  font-weight: 700;
}

.page-scroll {
  height: 100vh;
  box-sizing: border-box;
}

.hero {
  padding: calc(180rpx + env(safe-area-inset-top)) 40rpx 80rpx;
  text-align: center;
  position: relative;
}

.hero-title {
  display: block;
  font-size: 72rpx;
  font-weight: 800;
  line-height: 1.1;
  color: var(--text-strong);
  margin-bottom: 32rpx;
  letter-spacing: -0.02em;
}

.hero-subtitle {
  display: block;
  font-size: 32rpx;
  line-height: 1.6;
  color: var(--text-soft);
  max-width: 600rpx;
  margin: 0 auto 48rpx;
}

.cta {
  display: inline-block;
  background: var(--primary);
  color: #fff;
  padding: 24rpx 48rpx;
  border-radius: 100rpx;
  font-size: 32rpx;
  font-weight: 600;
  box-shadow: 0 12rpx 32rpx rgba(51, 207, 255, 0.3);
}

.section-header {
  padding: 0 40rpx;
  margin-bottom: 48rpx;
}

.marginTOP {
  margin-top: 80rpx;
}

.h2 {
  display: block;
  font-size: 48rpx;
  font-weight: 700;
  color: var(--text-strong);
  margin-bottom: 16rpx;
}

.subTitle {
  display: block;
  font-size: 28rpx;
  line-height: 1.6;
  color: var(--text-soft);
}

.panel {
  padding: 0 32rpx;
}

.section {
  margin-bottom: 64rpx;
}

.section-title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text-strong);
  margin-bottom: 32rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.section-title::before {
  content: "";
  display: block;
  width: 8rpx;
  height: 32rpx;
  background: var(--primary);
  border-radius: 4rpx;
}

.info-item {
  margin-bottom: 48rpx;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 24rpx;
  font-size: 32rpx;
  font-weight: 600;
  color: var(--text-strong);
  margin-bottom: 24rpx;
}

.item-title .num {
  font-size: 40rpx;
  color: var(--primary);
  font-weight: 800;
  opacity: 0.2;
}

.single-item {
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 32rpx;
  padding: 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(15, 23, 42, 0.04), inset 0 2rpx 0 rgba(255, 255, 255, 0.8);
  margin-bottom: 24rpx;
}

.theme-dark .single-item {
  background: rgba(30, 41, 59, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.2), inset 0 2rpx 0 rgba(255, 255, 255, 0.05);
}

.item-info .num {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text-strong);
  margin-bottom: 24rpx;
  display: block;
}

.desc-inline {
  margin-bottom: 16rpx;
}

.descTitle {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text-strong);
  margin-bottom: 8rpx;
  display: block;
}

.descText {
  font-size: 28rpx;
  line-height: 1.6;
  color: var(--text-soft);
  display: block;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 32rpx;
}

.btn {
  padding: 16rpx 32rpx;
  border-radius: 100rpx;
  font-size: 28rpx;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
  background: #fff;
  color: var(--text-strong);
  border: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}

.theme-dark .btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--text-strong);
}

.btn.primary {
  background: var(--primary);
  color: #fff;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(51, 207, 255, 0.28);
}

.tool-group {
  margin-bottom: 48rpx;
}

.toolTitle {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text-strong);
  margin-bottom: 24rpx;
}

.toolList {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.toolItem {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 24rpx;
  padding: 32rpx;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.04);
}

.theme-dark .toolItem {
  background: rgba(30, 41, 59, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.2);
}

.toolItem .item-title {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--text-strong);
}

.bottom-spacer {
  height: 200rpx;
}

/* Rate styling for v-html content */
:deep(.rate) {
  color: #00c087;
  font-weight: 700;
}
</style>
