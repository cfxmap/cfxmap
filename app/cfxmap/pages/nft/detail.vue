<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="nav-bar">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="nav-title">NFT Detail</text>
		</view>

		<view class="content">
			<view class="image-wrapper" v-if="nftData.is3D && nftData.animationUrl">
				<view class="web-view-container">
					<!-- #ifdef H5 -->
					<iframe :src="`/hybrid/html/3d.html?url=${encodeURIComponent(convertIpfsUrl(nftData.animationUrl))}`" frameborder="0" scrolling="no" class="model-view" style="width: 100%; height: 100%; pointer-events: auto; touch-action: none;"></iframe>
					<!-- #endif -->
					<!-- #ifndef H5 -->
					<!-- App 端 web-view 组件如果想要限制尺寸不全屏，需配置 webview-styles -->
					<web-view :src="`/hybrid/html/3d.html?url=${encodeURIComponent(convertIpfsUrl(nftData.animationUrl))}`" :webview-styles="webviewStyles"></web-view>
					<!-- #endif -->
				</view>
			</view>
			<view class="image-wrapper frame-style-wrapper" v-else>
				<view class="nft-frame">
					<image class="nft-image-framed" :src="convertIpfsUrl(nftData.image) || $staticUrl('/static/logo.png')" mode="aspectFill"></image>
				</view>
			</view>

			<view class="info-section">
				<view class="header-info">
					<text class="nft-series" v-if="nftData.series && nftData.series !== 'Unknown Collection' && nftData.series !== 'Unknown Series'">{{ nftData.series }}</text>
					<text class="nft-name">{{ nftData.name || 'Unknown NFT' }}</text>
				</view>

				<view class="detail-card">
					<view class="detail-row">
						<text class="label">Contract Address</text>
						<text class="value">{{ formatAddress(nftData.contractAddress) }}</text>
					</view>
					<view class="detail-row">
						<text class="label">Token ID</text>
						<text class="value">{{ nftData.tokenId || nftData.id }}</text>
					</view>
					<view class="detail-row">
						<text class="label">Token Standard</text>
						<text class="value">{{ nftData.standard || 'ERC-721' }}</text>
					</view>
					<view class="detail-row">
						<text class="label">Chain</text>
						<text class="value">{{ nftData.chain || 'Ethereum' }}</text>
					</view>
				</view>

				<view class="description-section" v-if="nftData.description">
					<text class="section-title">Description</text>
					<text class="description-text">{{ nftData.description }}</text>
				</view>
			</view>
		</view>

		<!-- Bottom Fixed Action Bar -->
		<view class="footer-action">
			<view class="send-btn" @click="handleSendNft">
				<image class="send-icon" :src="$staticUrl('/static/icons/icon_send_nft_btn.png')" mode="aspectFit"></image>
				<text class="send-text">Send NFT</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { api, convertIpfsUrl } from '@/utils/request'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				nftId: null,
				contract: null,
				tokenId: null,
				nftData: {},
				webviewStyles: {
					progress: false,
					width: '100%',
					height: '300px', // fallback height
					top: '100px'     // fallback top
				}
			}
		},
		onLoad(options) {
			if (options.id) {
				this.nftId = options.id;
			}
			if (options.contract && options.tokenId) {
				this.contract = options.contract;
				this.tokenId = options.tokenId;
			}
			this.fetchNftDetail();
			this.calculateWebviewStyle();
		},
		methods: {
			calculateWebviewStyle() {
				// 动态计算 web-view 的高度和位置，避免全屏遮挡
				try {
					const sysInfo = uni.getSystemInfoSync();
					// 计算导航栏高度（状态栏 + 导航栏约 88px）
					const navHeight = (sysInfo.statusBarHeight || 20) + 44; 
					// 图片区域高度约等于 600rpx -> 转换为 px
					const screenWidth = sysInfo.windowWidth;
					const imageHeight = (600 / 750) * screenWidth;
					
					// 留出一定的边距 (margin-top: 20rpx)
					const marginTop = (20 / 750) * screenWidth;
					
					this.webviewStyles = {
						progress: false,
						width: '100%',
						height: `${imageHeight}px`,
						top: `${navHeight + marginTop}px`,
						left: '0px',
						background: 'transparent'
					};
				} catch (e) {
					console.error('Failed to calculate webview styles', e);
				}
			},
			convertIpfsUrl,
			goBack() {
				uni.navigateBack();
			},
			formatAddress(addr) {
				if (!addr) return 'Unknown';
				if (addr.length > 12) {
					return addr.substring(0, 6) + '...' + addr.substring(addr.length - 4);
				}
				return addr;
			},
			async fetchNftDetail() {
				try {
					if (this.contract && this.tokenId) {
						const res = await api.getNftDetail(this.nftId, this.contract, this.tokenId);
						if (res) {
							this.nftData = res;
							return;
						}
					}
				} catch (e) {
					console.error('Failed to fetch NFT detail:', e);
				}
				
				this.nftData = {
					id: this.nftId,
					name: `NFT #${this.nftId || 'Unknown'}`,
					series: 'Unknown Collection',
					image: this.$staticUrl('/static/nft_detail.png')
				};
			},
			handleSendNft() {
				// 跳转到发送 NFT 页面，带上当前 NFT 的参数
				const query = `?id=${this.nftData.id}&contract=${this.nftData.contractAddress || ''}&tokenId=${this.nftData.tokenId || this.nftData.id || ''}`;
				uni.navigateTo({
					url: `/pages/transfer/send${query}`
				});
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
	}

	.nav-bar {
		padding: 80rpx 32rpx 20rpx;
		display: flex;
		align-items: center;
		background: var(--glass-bg);
		position: sticky;
		top: 0;
		z-index: 1000;
		backdrop-filter: blur(32rpx);
		-webkit-backdrop-filter: blur(32rpx);
	}

	.back-btn {
		padding: 10rpx;
		margin-right: 20rpx;
	}


	.nav-title {
		font-size: 32rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.content {
		flex: 1;
		padding-bottom: 200rpx; /* Increased to prevent footer-action overlap */
	}

	.image-wrapper {
		width: 100%;
		height: 600rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		margin-top: 20rpx;
		margin-bottom: 40rpx;
		position: relative;
		z-index: 1;
	}

	.web-view-container {
		width: 100%;
		height: 100%;
		position: relative;
		/* 确保容器不会拦截事件，让事件传递给 web-view */
		pointer-events: auto;
		touch-action: none;
	}

	.model-view-app {
		width: 100%;
		height: 600rpx;
		position: relative;
		margin-top: 100rpx;
	}

	.frame-style-wrapper {
		background-color: transparent;
		border-radius: 40rpx;
		padding: 48rpx;
		box-sizing: border-box;
	}

	.nft-frame {
		width: 520rpx;
		height: 520rpx;
		background-color: var(--card-bg-strong);
		border-radius: 32rpx;
		padding: 32rpx;
		box-shadow: var(--card-shadow-lg);
		border: var(--card-border);
		display: flex;
		justify-content: center;
		align-items: center;
		box-sizing: border-box;
	}

	.nft-image-framed {
		width: 100%;
		height: 100%;
		border-radius: 8rpx;
	}

	.nft-image {
		width: 100%;
		height: 100%;
	}

	.badge-3d {
		position: absolute;
		top: 32rpx;
		right: 32rpx;
		background: rgba(0, 0, 0, 0.6);
		color: white;
		padding: 8rpx 16rpx;
		border-radius: 8rpx;
		font-size: 24rpx;
		font-weight: bold;
	}

	.info-section {
		padding: 40rpx 32rpx;
	}

	.header-info {
		margin-bottom: 40rpx;
	}

	.nft-series {
		font-size: 28rpx;
		color: var(--text-secondary);
		margin-bottom: 8rpx;
		display: block;
	}

	.nft-name {
		font-size: 48rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.detail-card {
		background-color: var(--card-bg);
		border-radius: 28rpx;
		padding: 32rpx;
		margin-bottom: 40rpx;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.detail-row {
		display: flex;
		justify-content: space-between;
		padding: 20rpx 0;
		border-bottom: 1px solid var(--border-color);

		&:last-child {
			border-bottom: none;
			padding-bottom: 0;
		}

		&:first-child {
			padding-top: 0;
		}
	}

	.label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.value {
		font-size: 28rpx;
		color: var(--text-primary);
		font-weight: 500;
	}

	.description-section {
		margin-top: 40rpx;
	}

	.section-title {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
		margin-bottom: 16rpx;
		display: block;
	}

	.description-text {
		font-size: 28rpx;
		color: var(--text-secondary);
		line-height: 1.6;
	}

	/* Footer Action */
	.footer-action {
		position: fixed;
		bottom: 60rpx;
		left: 0;
		width: 100%;
		padding: 0 48rpx;
		box-sizing: border-box;
		z-index: 99;
	}

	.send-btn {
		width: 100%;
		height: 112rpx;
		background-color: var(--btn-confirm-bg);
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 16rpx;
		box-shadow: var(--btn-shadow);
	}

	.send-icon {
		width: 40rpx;
		height: 40rpx;
	}

	.send-text {
		color: var(--btn-text-white);
		font-size: 32rpx;
		font-weight: bold;
	}
</style>
