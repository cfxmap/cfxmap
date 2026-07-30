<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.changeUsernameTitle || i18n.changeUsername || 'Change Username' }}</text>
			<view class="placeholder"></view>
		</view>

		<view class="step-card">
			<text class="step-title">{{ i18n.enterNewUsername || 'Enter New Username' }}</text>
			
			<view class="input-row">
				<input class="input-field" type="text" :placeholder="i18n.usernamePlaceholder || 'Please enter new username'" v-model="newUsername" placeholder-class="placeholder-text" />
			</view>

			<button class="btn-primary" @click="submitNewUsername">
				<text class="btn-text">{{ i18n.confirmChange || 'Confirm Change' }}</text>
			</button>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				newUsername: ''
			}
		},
		onLoad() {
			this.fetchUserProfile()
		},
		computed: {
			...mapGetters(['i18n'])
		},
		methods: {
			async fetchUserProfile() {
				try {
					const res = await api.getUserProfile()
					if (res && res.user && res.user.nickName) {
						this.newUsername = res.user.nickName
					}
				} catch (e) {
					console.error('Failed to fetch user profile', e)
				}
			},
			goBack() {
				uni.navigateBack()
			},
			async submitNewUsername() {
				if (!this.newUsername) {
					uni.showToast({ title: this.i18n.pleaseEnterUsername, icon: 'none' })
					return
				}
				try {
					uni.showLoading({ title: this.i18n.modifying })
					await api.updateUserProfile({ nickName: this.newUsername })
					uni.hideLoading()
					uni.showToast({ title: this.i18n.modifySuccess, icon: 'success' })
					
					// 更新全局状态中的用户信息（如果有的话）
					// this.$store.dispatch('GetInfo')
					
					setTimeout(() => {
						uni.navigateBack()
					}, 1500)
				} catch (e) {
					uni.hideLoading()
				}
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
		justify-content: space-between;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
		margin-bottom: 16rpx;
	}

	.back-btn, .placeholder {
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

	.step-card {
		background-color: var(--card-bg);
		border-radius: 16rpx;
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.step-title {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
		margin-bottom: 8rpx;
	}

	.input-row {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: space-between;
		background-color: var(--control-bg);
		border-radius: 16rpx;
		padding: 24rpx 28rpx;
	}

	.input-field {
		flex: 1;
		font-size: 28rpx;
		color: var(--text-primary);
	}

	.placeholder-text {
		color: var(--text-secondary);
	}

	.btn-primary {
		width: 100%;
		height: 112rpx;
		background-color: var(--btn-confirm-bg);
		border-radius: 16rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		border: none;
		margin-top: 16rpx;
		padding: 0;
	}

	.btn-text {
		font-family: 'Inter', sans-serif;
		font-size: 32rpx;
		font-weight: bold;
		color: var(--btn-text-white);
	}
</style>