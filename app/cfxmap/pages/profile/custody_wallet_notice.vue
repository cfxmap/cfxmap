<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.custodyWalletTitle }}</text>
		</view>

		<scroll-view scroll-y class="content-scroll">
			<view class="hero-card">
				<text class="hero-badge">{{ i18n.custodyWalletBadge }}</text>
				<text class="hero-title">{{ i18n.custodyWalletHeroTitle }}</text>
				<text class="hero-desc">{{ i18n.custodyWalletHeroDesc }}</text>
			</view>

			<view v-for="item in noticeSections" :key="item.title" class="section-card">
				<text class="section-title">{{ item.title }}</text>
				<text class="section-body">{{ item.body }}</text>
			</view>

			<view class="section-card">
				<text class="section-title">{{ i18n.custodyWalletFaqTitle }}</text>
				<view v-for="item in faqList" :key="item.question" class="faq-item">
					<text class="faq-question">{{ item.question }}</text>
					<text class="faq-answer">{{ item.answer }}</text>
				</view>
			</view>

			<view class="footer-card">
				<text class="footer-note">{{ i18n.custodyWalletFooterNote }}</text>
				<button class="primary-btn" @click="goWalletManage">
					<view class="btn-content">{{ i18n.manageWallet }}</view>
				</button>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		computed: {
			...mapGetters(['i18n', 'themeClass']),
			noticeSections() {
				return [
					{
						title: this.i18n.custodyWalletTypeTitle,
						body: this.i18n.custodyWalletTypeBody
					},
					{
						title: this.i18n.custodyWalletKeyTitle,
						body: this.i18n.custodyWalletKeyBody
					},
					{
						title: this.i18n.custodyWalletRecoveryTitle,
						body: this.i18n.custodyWalletRecoveryBody
					},
					{
						title: this.i18n.custodyWalletShutdownTitle,
						body: this.i18n.custodyWalletShutdownBody
					},
					{
						title: this.i18n.custodyWalletExportTitle,
						body: this.i18n.custodyWalletExportBody
					},
					{
						title: this.i18n.custodyWalletRiskTitle,
						body: this.i18n.custodyWalletRiskBody
					},
					{
						title: this.i18n.custodyWalletExitTitle,
						body: this.i18n.custodyWalletExitBody
					}
				]
			},
			faqList() {
				return [
					{
						question: this.i18n.custodyWalletFaq1Question,
						answer: this.i18n.custodyWalletFaq1Answer
					},
					{
						question: this.i18n.custodyWalletFaq2Question,
						answer: this.i18n.custodyWalletFaq2Answer
					},
					{
						question: this.i18n.custodyWalletFaq3Question,
						answer: this.i18n.custodyWalletFaq3Answer
					}
				]
			}
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			goWalletManage() {
				uni.navigateTo({
					url: '/pages/profile/wallet_manage'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		background:
			radial-gradient(circle at top left, rgba(91, 140, 255, 0.14), transparent 34%),
			var(--bg-color);
		padding: 0 32rpx 32rpx;
		box-sizing: border-box;
	}

	.header {
		position: relative;
		display: flex;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
	}

	.back-btn {
		position: relative;
		z-index: 2;
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.title {
		position: absolute;
		left: 50%;
		transform: translateX(-50%);
		width: 68%;
		text-align: center;
		font-size: 42rpx;
		font-weight: bold;
		color: var(--text-primary);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.content-scroll {
		height: calc(100vh - 180rpx);
	}

	.hero-card,
	.section-card,
	.footer-card {
		background-color: var(--card-bg);
		border-radius: 28rpx;
		padding: 32rpx;
		margin-bottom: 24rpx;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.hero-card {
		display: flex;
		flex-direction: column;
		gap: 18rpx;
	}

	.hero-badge {
		align-self: flex-start;
		font-size: 22rpx;
		font-weight: 600;
		color: var(--btn-confirm-bg);
		background-color: rgba(88, 101, 242, 0.12);
		padding: 8rpx 16rpx;
		border-radius: 999rpx;
	}

	.hero-title,
	.section-title {
		font-size: 34rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.hero-desc,
	.section-body,
	.faq-answer,
	.footer-note {
		font-size: 28rpx;
		line-height: 1.7;
		color: var(--text-secondary);
	}

	.faq-item {
		display: flex;
		flex-direction: column;
		gap: 10rpx;
		padding-top: 24rpx;
		margin-top: 24rpx;
		border-top: 1px solid var(--border-color);
	}

	.faq-item:first-child {
		border-top: none;
		padding-top: 0;
		margin-top: 24rpx;
	}

	.faq-question {
		font-size: 30rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.footer-card {
		display: flex;
		flex-direction: column;
		gap: 24rpx;
		margin-bottom: calc(32rpx + env(safe-area-inset-bottom));
	}

	.primary-btn {
		background-color: var(--btn-confirm-bg);
		border: none;
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100rpx;

		&::after {
			border: none;
		}
	}

	.btn-content {
		color: var(--btn-text-white);
		font-size: 32rpx;
		font-weight: bold;
	}
</style>
