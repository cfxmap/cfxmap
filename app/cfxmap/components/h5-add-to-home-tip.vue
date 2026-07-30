<!-- H5：提示用户将网页添加到桌面 / 安装为应用；按 iOS / Android / PC 展示不同说明 -->
<template>
	<view v-if="visible" class="h5-a2h-container" :class="themeClass">
		<!-- Placeholder to prevent content blocking -->
		<view class="h5-a2h-placeholder" :style="{ height: bottomNav ? 'calc(290rpx + env(safe-area-inset-bottom))' : 'calc(160rpx + env(safe-area-inset-bottom))' }"></view>
		
		<view class="h5-a2h-wrap" :style="{ bottom: bottomNav ? 'calc(160rpx + env(safe-area-inset-bottom))' : 'calc(24rpx + env(safe-area-inset-bottom))' }">
			<view class="h5-a2h-inner" @click="handlePillClick">
				<view class="h5-a2h-icon">
					<!-- App Logo -->
					<image :src="$staticUrl('/static/logo.png')" class="share-icon app-logo" mode="aspectFit"></image>
				</view>
				<view class="h5-a2h-texts">
					<text class="h5-a2h-title">{{ openAppText }}</text>
				</view>
				<!-- Top right close button -->
				<view class="h5-a2h-close" @click.stop="dismiss">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="close-icon">
						<line x1="18" y1="6" x2="6" y2="18"></line>
						<line x1="6" y1="6" x2="18" y2="18"></line>
					</svg>
				</view>
			</view>
		</view>

		<!-- iOS Guide Mask -->
		<view v-if="showIosGuide" class="ios-guide-mask" @click="showIosGuide = false">
			<view class="ios-guide-panel" @click.stop>
				<view class="ios-guide-close" @click="showIosGuide = false">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="close-icon-large">
						<line x1="18" y1="6" x2="6" y2="18"></line>
						<line x1="6" y1="6" x2="18" y2="18"></line>
					</svg>
				</view>
				<image class="ios-guide-image" :src="iosGuideImageSrc" mode="aspectFit"></image>
			</view>
		</view>
		<!-- Fullscreen Android Guide Mask -->
		<view v-if="showAndroidGuide" class="android-guide-mask" @click="showAndroidGuide = false">
			<view class="android-guide-content" @click.stop>
				<view class="android-guide-close" @click="showAndroidGuide = false">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="close-icon-large">
						<line x1="18" y1="6" x2="6" y2="18"></line>
						<line x1="6" y1="6" x2="18" y2="18"></line>
					</svg>
				</view>
				<text class="android-guide-title">{{ i18n.addToHomeTitle === 'Add to home screen' ? 'Download APP' : '下载APP' }}</text>
				<text class="android-guide-desc">{{ i18n.addToHomeTitle === 'Add to home screen' ? 'Cannot open APP. Please download and install the latest APK.' : '无法拉起APP，请下载并安装最新版本。' }}</text>
				<button class="btn-download" type="primary" @click="downloadApk">{{ i18n.addToHomeTitle === 'Add to home screen' ? 'Download APK' : '立即下载' }}</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { getStaticUrl } from '@/utils/static-resource.js'

	function isStandalone() {
		if (typeof window === 'undefined') return false
		try {
			if (window.matchMedia && window.matchMedia('(display-mode: standalone)').matches) return true
			if (window.navigator.standalone === true) return true
		} catch (e) {}
		return false
	}

	function detectKind() {
		if (typeof navigator === 'undefined') return 'desktop'
		const ua = navigator.userAgent || ''
		const isIOS =
			/iPhone|iPad|iPod/i.test(ua) ||
			(navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
		const isAndroid = /Android/i.test(ua)
		if (isIOS) return 'ios'
		if (isAndroid) return 'android'
		return 'desktop'
	}

const GUIDE_IMAGES = {
	zh: getStaticUrl('/static/guide/中文引导.jpg'),
	en: getStaticUrl('/static/guide/英文引导.jpg')
}

let guideImagesPreloaded = false

function preloadGuideImages() {
	if (guideImagesPreloaded || typeof window === 'undefined' || typeof window.Image === 'undefined') return
	guideImagesPreloaded = true
	Object.keys(GUIDE_IMAGES).forEach((key) => {
		const img = new window.Image()
		img.src = GUIDE_IMAGES[key]
	})
}

	export default {
		name: 'H5AddToHomeTip',
		props: {
			bottomNav: {
				type: Boolean,
				default: false
			}
		},
		data() {
			return {
				kind: 'desktop',
				standalone: false,
				dismissed: false,
				showIosGuide: false,
				showAndroidGuide: false,
				apkUrl: ''
			}
		},
		computed: {
			...mapGetters(['i18n', 'themeClass']),
			isEnglish() {
				return this.i18n.addToHomeTitle === 'Add to home screen'
			},
			iosGuideImageSrc() {
				return this.isEnglish ? GUIDE_IMAGES.en : GUIDE_IMAGES.zh
			},
			openAppText() {
				if (this.kind === 'ios') {
					return this.isEnglish ? 'Add to Home Screen' : '添加到主屏幕'
				}
				if (this.kind === 'android') {
					return this.isEnglish ? 'Download APP' : '下载APP'
				}
				return this.isEnglish ? 'Click to open APP' : '点击打开APP'
			},
			visible() {
				// #ifdef H5
				if (this.kind !== 'ios' && this.kind !== 'android') return false
				if (this.standalone) return false
				if (this.dismissed) return false
				return true
				// #endif
				// #ifndef H5
				return false
				// #endif
			},
			tipBody() {
				const t = this.i18n
				if (this.kind === 'ios') return t.addToHomeIos
				if (this.kind === 'android') return t.addToHomeAndroid
				return t.addToHomeDesktop
			}
		},
		mounted() {
			// #ifdef H5
			preloadGuideImages()
			this.kind = detectKind()
			this.standalone = isStandalone()
			if (this.kind === 'android') {
				this.fetchApkUrl()
			}
			// #endif
		},
		methods: {
			dismiss() {
				this.dismissed = true
			},
			handlePillClick() {
				if (this.kind === 'ios') {
					this.showIosGuide = true
				} else if (this.kind === 'android') {
					// 直接下载 APK，不再拉起 APP
					if (this.apkUrl) {
						this.downloadApk()
					} else {
						uni.showLoading({ title: this.i18n.addToHomeTitle === 'Add to home screen' ? 'Loading...' : '加载中...' })
						uni.request({
							url: api.getBaseUrl() + '/api/app/version',
							method: 'GET',
							success: (res) => {
								if (res.statusCode === 200 && res.data && res.data.code === 200) {
									const data = res.data.data
									this.apkUrl = data?.apkUrl || data?.officialUrl || ''
									this.downloadApk()
								} else {
									uni.showToast({ title: this.i18n.addToHomeTitle === 'Add to home screen' ? 'Failed to get URL' : '获取下载地址失败', icon: 'none' })
								}
							},
							complete: () => {
								uni.hideLoading()
							}
						})
					}
				} else {
					uni.showToast({
						title: this.i18n.addToHomeTitle === 'Add to home screen' ? 'APP is coming soon' : 'APP即将上线',
						icon: 'none'
					})
				}
			},
			fetchApkUrl() {
				if (this.apkUrl) return
				uni.request({
					url: api.getBaseUrl() + '/api/app/version',
					method: 'GET',
					success: (res) => {
						if (res.statusCode === 200 && res.data && res.data.code === 200) {
							const data = res.data.data
							this.apkUrl = data?.apkUrl || data?.officialUrl || ''
						}
					}
				})
			},
			downloadApk() {
				if (!this.apkUrl) {
					uni.showToast({ title: this.i18n.addToHomeTitle === 'Add to home screen' ? 'Download URL not configured' : '暂未配置下载地址', icon: 'none' })
					return
				}
				window.location.href = this.apkUrl
			}
		}
	}
</script>

<style scoped lang="scss">
	.h5-a2h-container {
		width: 100%;
	}

	.h5-a2h-placeholder {
		width: 100%;
		background: transparent;
	}

	.h5-a2h-wrap {
		position: fixed;
		left: 50%;
		transform: translateX(-50%);
		width: max-content;
		max-width: calc(100% - 48rpx);
		box-sizing: border-box;
		z-index: 100000;
		pointer-events: auto;
		display: flex;
		flex-direction: column;
		align-items: center;
	}

	.h5-a2h-inner {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 20rpx;
		padding: 20rpx 40rpx;
		border-radius: 999rpx;
		background-color: var(--card-bg-strong, var(--card-bg));
		border: 1px solid var(--border-color);
		box-shadow: var(--card-shadow-lg, var(--card-shadow));
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
		width: auto;
		box-sizing: border-box;
		position: relative;
	}

	.h5-a2h-icon {
		width: 48rpx;
		height: 48rpx;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.share-icon.app-logo {
		width: 100%;
		height: 100%;
		border-radius: 8rpx;
	}

	.h5-a2h-container.theme-dark .app-logo {
		filter: invert(1);
	}

	.h5-a2h-texts {
		display: flex;
		flex-direction: column;
		gap: 6rpx;
		min-width: 0;
	}

	.h5-a2h-title {
		font-size: 28rpx;
		font-weight: 700;
		color: var(--text-primary);
		line-height: 1.35;
		white-space: nowrap;
	}

	.h5-a2h-desc {
		font-size: 22rpx;
		color: var(--text-secondary);
		line-height: 1.4;
	}

	.h5-a2h-close {
		position: absolute;
		top: -12rpx;
		right: 12rpx;
		width: 40rpx;
		height: 40rpx;
		background: var(--card-bg-strong, #fff);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.15);
		color: var(--text-secondary, #666);
		border: 1px solid var(--border-color);
		z-index: 2;
	}

	.close-icon {
		width: 24rpx;
		height: 24rpx;
	}

	/* iOS Guide Styles */
	.ios-guide-mask {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.72);
		z-index: 9999999;
		display: flex;
		justify-content: center;
		align-items: center;
		padding: calc(env(safe-area-inset-top) + 12rpx) 8rpx 8rpx;
		box-sizing: border-box;
	}

	.ios-guide-panel {
		position: relative;
		width: calc(100vw - 8rpx);
		max-width: none;
		padding: 4rpx;
		border-radius: 18rpx;
		background: rgba(18, 18, 18, 0.92);
		box-shadow: 0 16rpx 48rpx rgba(0, 0, 0, 0.28);
		box-sizing: border-box;
	}

	.ios-guide-close {
		position: absolute;
		top: 44rpx;
		right: 18rpx;
		width: 64rpx;
		height: 64rpx;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.98);
		color: #333333;
		box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.24);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 3;
	}

	.ios-guide-image {
		display: block;
		width: 100%;
		height: calc(100vh - 16rpx);
		max-height: none;
		border-radius: 14rpx;
	}

	/* Android Fullscreen Guide Styles */
	.android-guide-mask {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background: rgba(0, 0, 0, 0.6);
		z-index: 9999999;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.android-guide-content {
		width: 600rpx;
		background: var(--card-bg, #ffffff);
		border-radius: 24rpx;
		padding: 40rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		position: relative;
		box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.15);
	}

	.android-guide-close {
		position: absolute;
		top: 24rpx;
		right: 24rpx;
		color: var(--text-secondary, #999);
		padding: 8rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.close-icon-large {
		width: 36rpx;
		height: 36rpx;
	}

	.android-guide-title {
		font-size: 36rpx;
		font-weight: bold;
		color: var(--text-primary, #333);
		margin-top: 20rpx;
		margin-bottom: 20rpx;
	}

	.android-guide-desc {
		font-size: 28rpx;
		color: var(--text-secondary, #666);
		text-align: center;
		line-height: 1.5;
		margin-bottom: 40rpx;
	}

	.btn-download {
		width: 100%;
		border-radius: 999rpx;
		font-size: 32rpx;
		height: 88rpx;
		line-height: 88rpx;
	}
</style>
