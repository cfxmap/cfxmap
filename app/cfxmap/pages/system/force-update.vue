<template>
	<view class="page-shell" :class="themeClass">
		<global-loading />
		<view class="mask" />
		<view class="dialog-card">
			<text class="title">需要更新</text>
			<text class="desc">当前版本与服务器要求不一致，请前往官网获取最新版本安装包。</text>
			<view class="ver-row">
				<text class="ver-label">当前版本</text>
				<text class="ver-value">{{ formattedClientVersion }}</text>
			</view>
			<view class="ver-row">
				<text class="ver-label">要求版本</text>
				<text class="ver-value">{{ formattedServerVersion }}</text>
			</view>
			<button class="btn-update" type="primary" @click="openOfficial">立即更新</button>
			<text v-if="!officialUrl && !apkUrl" class="hint-url">下载地址未配置，请联系管理员。</text>
		</view>
	</view>
</template>

<script>
	import { formatDisplayVersion } from '@/config/app-version.js'

	export default {
		data() {
			return {
				officialUrl: '',
				apkUrl: '',
				serverVersion: '',
				clientVersion: ''
			}
		},
		onLoad(query) {
			this.officialUrl = decodeURIComponent(query.url || '')
			this.apkUrl = decodeURIComponent(query.apk || '')
			this.serverVersion = decodeURIComponent(query.server || '')
			this.clientVersion = decodeURIComponent(query.client || '')
		},
		onBackPress() {
			return true
		},
		computed: {
			formattedClientVersion() {
				return formatDisplayVersion(this.clientVersion)
			},
			formattedServerVersion() {
				return formatDisplayVersion(this.serverVersion)
			}
		},
		methods: {
			openOfficial() {
				// 优先使用 apkUrl，如果没有则回退到 officialUrl
				const url = (this.apkUrl || this.officialUrl || '').trim()
				if (!url) {
					uni.showToast({ title: '下载地址未配置', icon: 'none' })
					return
				}
				// #ifdef APP-PLUS
				try {
					plus.runtime.openURL(url)
				} catch (e) {
					uni.showToast({ title: '无法打开浏览器', icon: 'none' })
				}
				// #endif
				// #ifdef H5
				window.location.href = url
				// #endif
			}
		}
	}
</script>

<style scoped>
	.page-shell {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 48rpx 40rpx;
		box-sizing: border-box;
		position: relative;
	}
	.mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		background: rgba(15, 23, 42, 0.55);
		z-index: 0;
	}
	.dialog-card {
		position: relative;
		z-index: 1;
		width: 100%;
		max-width: 620rpx;
		padding: 48rpx 40rpx 40rpx;
		border-radius: 28rpx;
		background: var(--card-bg-strong, #ffffff);
		border: var(--card-border, 1px solid rgba(220, 231, 248, 0.96));
		box-shadow: var(--card-shadow-lg, 0 24rpx 60rpx rgba(51, 207, 255, 0.18));
		backdrop-filter: blur(28rpx);
	}
	.theme-dark .dialog-card {
		background: var(--card-bg-strong, #101a30);
	}
	.title {
		display: block;
		font-size: 40rpx;
		font-weight: 700;
		color: var(--text-primary);
		text-align: center;
		margin-bottom: 20rpx;
	}
	.desc {
		display: block;
		font-size: 28rpx;
		line-height: 1.55;
		color: var(--text-secondary);
		text-align: center;
		margin-bottom: 32rpx;
	}
	.ver-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 16rpx 0;
		border-bottom: 1px solid var(--border-color);
		font-size: 26rpx;
	}
	.ver-label {
		color: var(--text-secondary);
	}
	.ver-value {
		color: var(--text-primary);
		font-weight: 600;
	}
	.btn-update {
		margin-top: 40rpx;
		width: 100%;
		height: 96rpx;
		line-height: 96rpx;
		border-radius: 48rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: #fff;
		background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
		box-shadow: var(--btn-shadow);
		border: none;
	}
	.hint-url {
		display: block;
		margin-top: 24rpx;
		font-size: 24rpx;
		color: var(--warning-text);
		text-align: center;
	}
</style>
