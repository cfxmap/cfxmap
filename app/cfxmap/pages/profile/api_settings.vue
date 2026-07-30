<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.apiSettings || 'API Settings' }}</text>
		</view>

		<view class="card-wrapper">
			<view class="card-inner">
				<text class="label">{{ i18n.backendIp || 'Backend IP Address' }}</text>
				<view class="input-box-wrapper">
					<view class="input-box-inner">
						<input class="input" v-model="backendIp" :placeholder="i18n.backendIpPlaceholder || 'e.g. http://192.168.1.100:8080'" />
					</view>
				</view>

				<button class="btn-primary" @click="saveSettings">
					<view class="btn-content">{{ i18n.save || 'Save' }}</view>
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { DEFAULT_APP_API_BASE, isIpV4Host, normalizeApiBaseUrl } from '@/utils/request'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				backendIp: ''
			}
		},
		computed: {
			...mapGetters(['i18n'])
		},
		onLoad() {
			const savedIp = uni.getStorageSync('backend_ip');
			if (savedIp) {
				this.backendIp = savedIp;
			} else {
				// #ifdef H5
				if (window.location && window.location.hostname && window.location.hostname !== 'localhost') {
					const { protocol, hostname } = window.location;
					this.backendIp = isIpV4Host(hostname)
						? `${protocol}//${hostname}:8080`
						: `${protocol}//${hostname}`;
				} else {
					this.backendIp = 'http://localhost:8080';
				}
				// #endif
				// #ifndef H5
				// #ifdef APP-PLUS
				this.backendIp = DEFAULT_APP_API_BASE;
				// #else
				this.backendIp = 'http://localhost:8080';
				// #endif
				// #endif
			}
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			saveSettings() {
				if (!this.backendIp) {
					uni.showToast({ title: this.i18n.pleaseEnterIp, icon: 'none' });
					return;
				}
				
				let ip = normalizeApiBaseUrl(this.backendIp.trim());
				if (!ip.startsWith('http://') && !ip.startsWith('https://')) {
					ip = 'http://' + ip;
					ip = normalizeApiBaseUrl(ip);
				}
				
				uni.setStorageSync('backend_ip', ip);
				uni.showToast({ title: this.i18n.saveSuccessful, icon: 'success' });
				
				setTimeout(() => {
					uni.navigateBack();
				}, 1500);
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		padding: 0 32rpx;
		box-sizing: border-box;
	}

	.header {
		display: flex;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
		gap: 32rpx;
	}

	.back-btn {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.title {
		font-size: 48rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.card-wrapper {
		background-color: var(--card-bg);
		margin-bottom: 32rpx;
		border-radius: 16rpx;
	}

	.card-inner {
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.input-box-wrapper {
		background-color: var(--control-bg);
		border-radius: 16rpx;
	}

	.input-box-inner {
		padding: 24rpx;
	}

	.input {
		width: 100%;
		font-size: 28rpx;
		color: var(--text-primary);
		background-color: transparent;
	}

	.btn-primary {
		background-color: var(--text-primary);
		margin-top: 16rpx;
		border: none;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100rpx;
		&::after { border: none; }
	}

	.btn-primary .btn-content {
		color: var(--bg-color);
		font-size: 32rpx;
		font-weight: bold;
	}
</style>
