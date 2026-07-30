<template>
	<view class="bottom-nav">
		<view class="bottom-nav-item" :class="{ active: current === 'wallet' }" @click="goPage('wallet')">
			<image
				class="bottom-nav-icon-img"
				:src="current === 'wallet' ? $staticUrl('/static/bottom-nav/tab-wallet-active.png') : $staticUrl('/static/bottom-nav/tab-wallet.png')"
				mode="aspectFit"
				:lazy-load="false"
			/>
			<text class="bottom-nav-text">{{ i18n.walletTitle }}</text>
		</view>
		<view class="bottom-nav-item" :class="{ active: current === 'community' }" @click="goPage('community')">
			<view class="bottom-nav-community-icon" :class="{ 'bottom-nav-community-icon--active': current === 'community' }">
				<view class="community-head community-head--left"></view>
				<view class="community-head community-head--center"></view>
				<view class="community-head community-head--right"></view>
				<view class="community-body community-body--back"></view>
				<view class="community-body community-body--front"></view>
			</view>
			<text class="bottom-nav-text">{{ i18n.communityTitle }}</text>
		</view>
		<view class="bottom-nav-item" :class="{ active: current === 'guide' }" @click="goPage('guide')">
			<image
				class="bottom-nav-icon-img"
				:src="current === 'guide' ? $staticUrl('/static/bottom-nav/tab-guide-active.png') : $staticUrl('/static/bottom-nav/tab-guide.png')"
				mode="aspectFit"
				:lazy-load="false"
			/>
			<text class="bottom-nav-text">{{ i18n.guideTitle }}</text>
		</view>
		<view class="bottom-nav-item" :class="{ active: current === 'profile' }" @click="goPage('profile')">
			<image
				class="bottom-nav-icon-img"
				:src="current === 'profile' ? $staticUrl('/static/bottom-nav/tab-profile-active.png') : $staticUrl('/static/bottom-nav/tab-profile.png')"
				mode="aspectFit"
				:lazy-load="false"
			/>
			<text class="bottom-nav-text">{{ i18n.profileTitle }}</text>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'

	export default {
		props: {
			current: {
				type: String,
				default: ''
			}
		},
		computed: {
			...mapGetters(['i18n'])
		},
		methods: {
			goPage(page) {
				if (page === this.current) {
					return
				}
				const map = {
					wallet: '/pages/wallet/wallet',
					community: '/pages/community/community',
					guide: '/pages/guide/conflux',
					profile: '/pages/profile/profile'
				}
				const url = map[page]
				if (url) {
					uni.switchTab({ url })
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	.bottom-nav {
		position: fixed;
		left: 32rpx;
		right: 32rpx;
		bottom: calc(12rpx + env(safe-area-inset-bottom));
		display: flex;
		align-items: center;
		justify-content: space-around;
		padding: 18rpx 24rpx 16rpx;
		border-radius: 999rpx;
		background: var(--glass-bg);
		border: 1px solid rgba(255, 255, 255, 0.18);
		box-shadow: var(--card-shadow-md);
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
		z-index: 40;
		/* 独立合成层，减轻 Tab 切换时整栏闪烁 */
		transform: translateZ(0);
		-webkit-transform: translateZ(0);
		backface-visibility: hidden;
		-webkit-backface-visibility: hidden;
		contain: layout style;
		box-sizing: border-box;
	}

	.theme-dark .bottom-nav {
		background: var(--ios-glass-bg);
		border-color: var(--ios-glass-border);
		box-shadow:
			0 -12rpx 32rpx rgba(2, 8, 20, 0.34),
			var(--ios-glass-highlight);
		backdrop-filter: blur(32rpx) saturate(165%);
		-webkit-backdrop-filter: blur(32rpx) saturate(165%);
	}

	.bottom-nav-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 8rpx;
		flex: 1;
		padding: 10rpx 0;
		border-radius: 999rpx;
		-webkit-tap-highlight-color: transparent;
	}

	.bottom-nav-item:active {
		transform: scale(0.98);
	}

	.bottom-nav-item.active {
		background: rgba(255, 255, 255, 0.12);
	}

	.theme-dark .bottom-nav-item.active {
		background: rgba(255, 255, 255, 0.1);
	}

	.bottom-nav-icon-img {
		width: 48rpx;
		height: 48rpx;
		flex-shrink: 0;
		display: block;
		object-fit: contain;
	}

	.bottom-nav-community-icon {
		position: relative;
		width: 48rpx;
		height: 48rpx;
		flex-shrink: 0;
	}

	.community-head,
	.community-body {
		position: absolute;
		border: 2px solid currentColor;
		box-sizing: border-box;
		background: transparent;
	}

	.community-head {
		width: 12rpx;
		height: 12rpx;
		border-radius: 999rpx;
	}

	.community-head--left,
	.community-head--right,
	.community-body--back {
		color: rgba(143, 150, 163, 0.9);
	}

	.community-head--center,
	.community-body--front {
		color: var(--text-secondary);
	}

	.community-head--left {
		left: 6rpx;
		top: 12rpx;
	}

	.community-head--center {
		left: 18rpx;
		top: 8rpx;
		width: 14rpx;
		height: 14rpx;
	}

	.community-head--right {
		right: 6rpx;
		top: 12rpx;
	}

	.community-body {
		border-radius: 999rpx 999rpx 14rpx 14rpx;
		border-bottom-width: 0;
	}

	.community-body--back {
		left: 3rpx;
		top: 24rpx;
		width: 18rpx;
		height: 10rpx;
	}

	.community-body--front {
		left: 14rpx;
		top: 22rpx;
		width: 20rpx;
		height: 12rpx;
	}

	.bottom-nav-community-icon--active .community-head,
	.bottom-nav-community-icon--active .community-body {
		color: var(--accent-color);
	}

	.bottom-nav-text {
		font-size: 24rpx;
		color: var(--text-secondary);
		font-weight: 500;
	}

	.bottom-nav-item.active .bottom-nav-text {
		color: var(--text-primary);
	}

	@media (max-width: 480px) {
		.bottom-nav {
			left: 16rpx;
			right: 16rpx;
			bottom: calc(8rpx + env(safe-area-inset-bottom));
			padding: 10rpx 12rpx 8rpx;
		}

		.bottom-nav-item {
			gap: 4rpx;
			padding: 6rpx 0;
		}

		.bottom-nav-icon-img,
		.bottom-nav-community-icon {
			width: 40rpx;
			height: 40rpx;
		}

		.community-head {
			width: 10rpx;
			height: 10rpx;
		}

		.community-head--left {
			left: 4rpx;
			top: 11rpx;
		}

		.community-head--center {
			left: 15rpx;
			top: 7rpx;
			width: 12rpx;
			height: 12rpx;
		}

		.community-head--right {
			right: 4rpx;
			top: 11rpx;
		}

		.community-body--back {
			left: 2rpx;
			top: 21rpx;
			width: 16rpx;
			height: 9rpx;
		}

		.community-body--front {
			left: 11rpx;
			top: 20rpx;
			width: 18rpx;
			height: 10rpx;
		}

		.bottom-nav-text {
			font-size: 18rpx;
		}
	}

	/* 与 App.vue 主内容 max-width 分段对齐，底栏同宽居中 */
	@media (min-width: 600px) {
		.bottom-nav {
			left: 50%;
			right: auto;
			width: calc(100% - 64px);
			max-width: 840rpx;
			transform: translate3d(-50%, 0, 0);
			-webkit-transform: translate3d(-50%, 0, 0);
		}
	}

	@media (min-width: 900px) {
		.bottom-nav {
			max-width: 1040rpx;
		}
	}
</style>
