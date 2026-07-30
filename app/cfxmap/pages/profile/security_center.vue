<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.securityTitle }}</text>
		</view>

		<view class="section-title">{{ i18n.personalInfo }}</view>
		<view class="list-group-wrapper">
			<view class="list-group-inner">
				<view class="list-item" @click="changeAvatar">
					<view class="item-left">
						<image class="avatar-img" :src="avatarUrl" :key="avatarImageKey" mode="aspectFill"></image>
						<text class="item-title">{{ i18n.avatar }}</text>
					</view>
					<text class="item-arrow">></text>
				</view>
				<view class="list-item" @click="changeUsername">
					<text class="item-title">{{ i18n.changeUsername || 'Change Username' }}</text>
					<view class="item-right">
						<text class="item-value">{{ username }}</text>
						<text class="item-arrow">></text>
					</view>
				</view>
			</view>
		</view>

		<view class="section-title">{{ i18n.security }}</view>
		<view class="list-group-wrapper">
			<view class="list-group-inner">
				<view class="list-item" @click="changePassword">
					<text class="item-title">{{ i18n.changePassword }}</text>
					<text class="item-arrow">></text>
				</view>
				<view class="list-item" @click="goTradePassword">
					<text class="item-title">{{ i18n.tradePwdPageTitle }}</text>
					<text class="item-arrow">></text>
				</view>
				<view class="list-item" @click="bindEmail">
					<text class="item-title">{{ i18n.bindEmail || 'Bind Email' }}</text>
					<view class="item-right">
						<text class="item-value">{{ emailMasked || i18n.unbound || 'Unbound' }}</text>
						<text class="item-arrow">></text>
					</view>
				</view>
			</view>
		</view>

		<view class="section-title">{{ i18n.custodyWalletSectionTitle }}</view>
		<view class="list-group-wrapper">
			<view class="list-group-inner">
				<view class="list-item list-item--multi" @click="goCustodyWalletNotice">
					<view class="item-stack">
						<text class="item-title">{{ i18n.custodyWalletTitle }}</text>
						<text class="item-desc">{{ i18n.custodyWalletEntryDesc }}</text>
					</view>
					<text class="item-arrow">></text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { clearAvatarCache, ensureAvatarCached } from '@/utils/avatar-cache.js'
	import { getStaticUrl } from '@/utils/static-resource.js'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				avatarUrl: getStaticUrl('/static/avatar.png'),
				avatarImageKey: 0,
				username: '',
				email: ''
			}
		},
		onShow() {
			this.fetchUserProfile()
		},
		computed: {
			...mapGetters(['i18n', 'themeClass']),
			emailMasked() {
				if (!this.email) return '';
				const parts = this.email.split('@');
				if (parts.length !== 2) return this.email;
				return parts[0].substring(0, 1) + '***@' + parts[1];
			}
		},
		methods: {
			async fetchUserProfile() {
				try {
					const res = await api.getUserProfile()
					if (res && res.user) {
						const remote = res.user.avatar || getStaticUrl('/static/avatar.png')
						this.avatarUrl = await ensureAvatarCached(remote)
						if (res.user.nickName) {
							this.username = res.user.nickName
						}
						if (res.user.email) {
							this.email = res.user.email
						}
					}
				} catch (e) {
					console.error('Failed to fetch user profile', e)
				}
			},
			goBack() {
				uni.navigateBack()
			},
			changeAvatar() {
				uni.chooseImage({
					count: 1,
					success: async (res) => {
						const tempFilePaths = res.tempFilePaths
						if (!tempFilePaths || !tempFilePaths.length) return
						const tempFiles = Array.isArray(res.tempFiles) ? res.tempFiles : []
						uni.showLoading({ title: this.i18n.uploading })
						try {
							const url = await api.uploadAvatar({
								filePath: tempFilePaths[0],
								tempFile: tempFiles[0]
							})
							clearAvatarCache()
							this.avatarUrl = await ensureAvatarCached(url)
							this.avatarImageKey += 1
							this.$store.commit('SET_PROFILE_AVATAR_FORCED', this.avatarUrl)
							uni.showToast({ title: this.i18n.modifySuccess, icon: 'success' })
						} catch (e) {
							console.error('avatar upload', e)
							uni.showToast({ title: this.i18n.uploadFailed, icon: 'none' })
						} finally {
							uni.hideLoading()
						}
					}
				})
			},
			changeUsername() {
				uni.navigateTo({
					url: '/pages/profile/change_username'
				})
			},
			changePassword() {
				uni.navigateTo({
					url: '/pages/profile/change_password'
				})
			},
			goTradePassword() {
				uni.navigateTo({
					url: '/pages/profile/trade_password'
				})
			},
			bindEmail() {
				uni.navigateTo({
					url: '/pages/profile/bind_email'
				})
			},
			goCustodyWalletNotice() {
				uni.navigateTo({
					url: '/pages/profile/custody_wallet_notice'
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
		padding: 0 32rpx;
		box-sizing: border-box;
	}

	.header {
		position: relative;
		display: flex;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
		margin-bottom: 32rpx;
	}

	.back-btn {
		position: relative;
		z-index: 2;
		width: 48rpx;
		height: 48rpx;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.title {
		position: absolute;
		left: 50%;
		transform: translateX(-50%);
		width: 62%;
		text-align: center;
		font-size: 48rpx;
		font-weight: bold;
		color: var(--text-primary);
		pointer-events: none;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.section-title {
		font-size: 24rpx;
		color: var(--text-secondary);
		margin-left: 32rpx;
		margin-bottom: 16rpx;
	}

	.list-group-wrapper {
		background-color: var(--card-bg);
		margin-bottom: 48rpx;
		border-radius: 28rpx;
		overflow: hidden;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.list-group-inner {
		padding: 0 32rpx;
	}

	.list-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 32rpx 0;
		border-bottom: 2rpx solid var(--border-color);
		
		&:last-child {
			border-bottom: none;
		}
	}

	.item-left {
		display: flex;
		align-items: center;
		gap: 24rpx;
	}

	.avatar-img {
		width: 80rpx;
		height: 80rpx;
		border-radius: 50%;
		background-color: var(--control-bg);
	}

	.item-title {
		font-size: 32rpx;
		color: var(--text-primary);
	}

	.item-right {
		display: flex;
		align-items: center;
		gap: 16rpx;
	}

	.item-stack {
		display: flex;
		flex-direction: column;
		gap: 10rpx;
		flex: 1;
		padding-right: 16rpx;
	}

	.item-value {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.item-desc {
		font-size: 24rpx;
		line-height: 1.5;
		color: var(--text-secondary);
	}

	.item-arrow {
		font-size: 32rpx;
		color: var(--text-tertiary);
	}

	.list-item--multi {
		gap: 24rpx;
	}
</style>
