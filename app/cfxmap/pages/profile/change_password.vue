<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.changePasswordTitle || 'Change Password' }}</text>
		</view>

		<!-- 图形验证码弹框 -->
		<view v-if="captchaModalVisible" class="captcha-modal-mask" @click.self="closeCaptchaModal">
			<view class="captcha-modal" @click.stop>
				
				<view class="captcha-modal-close" @click="closeCaptchaModal">×</view>
				<text class="captcha-modal-title">{{ i18n.imageCaptchaPlaceholder || '图形验证码' }}</text>
				<text class="captcha-modal-desc">{{ i18n.captchaModalHint }}</text>

				<view class="captcha-modal-img-block">
					<view class="captcha-modal-img-card" @click="refreshImageCaptcha">
						<image
							v-if="captchaImage"
							class="captcha-modal-image"
							:src="captchaImage"
							mode="aspectFit"
						/>
						<view v-else class="captcha-modal-image captcha-modal-image--empty">
							<text class="captcha-modal-refresh-text">{{ i18n.captchaRefresh || '点击加载' }}</text>
						</view>
					</view>
					<view class="captcha-modal-refresh-row" @click="refreshImageCaptcha">
						<text class="captcha-modal-refresh-link">{{ i18n.captchaRefresh || '刷新' }}</text>
					</view>
				</view>

				<text class="captcha-field-label">{{ i18n.imageCaptchaPlaceholder || '验证码' }}</text>
				<input
					class="captcha-modal-input"
					type="text"
					:placeholder="i18n.imageCaptchaPlaceholder || '请输入图形验证码'"
					v-model="captchaCode"
					placeholder-class="captcha-placeholder"
				/>

				<view
					class="captcha-modal-submit"
					:class="{ 'captcha-modal-submit--busy': captchaSubmitting }"
					@click="onCaptchaModalConfirm"
				>
					<text class="captcha-modal-submit-text">{{ i18n.confirmBtn }}</text>
				</view>
				<view class="captcha-modal-cancel-link" @click="closeCaptchaModal">
					<text>{{ i18n.cancelBtn }}</text>
				</view>

				<view v-if="captchaSubmitting" class="captcha-modal-busy">
					<view class="captcha-modal-spinner"></view>
					<text class="captcha-modal-busy-text">{{ i18n.sending }}</text>
				</view>
			</view>
		</view>

		<view class="tips-card" v-if="email && step === 1">
			<text class="tips-title">{{ i18n.changePwdStepsTitle || 'Email Verification Required' }}</text>
			<text class="tips-desc">{{ i18n.changePwdStepsDesc || 'You must verify your email code before setting a new password.' }}</text>
		</view>

		<view class="tips-card" v-if="!email && step === 2">
			<text class="tips-title">{{ i18n.changePwdStepsTitle || 'Security Verification' }}</text>
			<text class="tips-desc">{{ i18n.changePwdStepsDesc || 'Please enter your old password to verify your identity.' }}</text>
		</view>

		<!-- Step 1: Verify Email -->
		<view class="step-card" v-if="step === 1 && email">
			<text class="step-title">{{ i18n.step1VerifyEmail || 'Step 1: Verify Email Code' }}</text>
			
			<view class="input-row readonly" v-if="email">
				<text class="input-field">{{ i18n.currentEmail || 'Current Email' }}: {{ emailMasked }}</text>
			</view>

			<view class="input-row">
				<input class="input-field" type="text" :placeholder="i18n.enterEmailCode || 'Enter email verification code'" v-model="code" placeholder-class="placeholder-text" />
				<view
					class="send-code-btn"
					:class="{ 'send-code-btn--disabled': countdown > 0 }"
					@click="sendCode"
				>
					<text class="send-code-text">{{ sendCodeButtonLabel(i18n) }}</text>
				</view>
			</view>

			<button class="btn-primary" @click="verifyEmail">
				<text class="btn-text">{{ i18n.verifyPassed || 'Verify' }}</text>
			</button>
		</view>

		<!-- Step 2: Set New Password -->
		<view class="step-card" v-if="step === 2">
			<text class="step-title">{{ i18n.step2SetPwd || 'Step 2: Set New Password' }}</text>
			
			<view class="input-row" v-if="!email">
				<input class="input-field" type="password" :placeholder="i18n.enterOldPwd || 'Enter old password'" v-model="oldPassword" placeholder-class="placeholder-text" />
			</view>

			<view class="input-row">
				<input class="input-field" type="password" :placeholder="i18n.enterNewPwd || 'Enter new password'" v-model="newPassword" placeholder-class="placeholder-text" />
			</view>

			<view class="input-row">
				<input class="input-field" type="password" :placeholder="i18n.confirmNewPwd || 'Confirm new password'" v-model="confirmPassword" placeholder-class="placeholder-text" />
			</view>

			<button class="btn-primary" @click="submitNewPassword">
				<text class="btn-text">{{ i18n.confirmChange || 'Confirm Change' }}</text>
			</button>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import emailCodeCaptchaModal from '@/mixins/email-code-captcha-modal.js'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		mixins: [emailCodeCaptchaModal],
		data() {
			return {
				step: 1,
				email: '', // 需要从用户信息中获取
				code: '',
				oldPassword: '',
				newPassword: '',
				confirmPassword: ''
			}
		},
		onLoad() {
			const token = uni.getStorageSync('token')
			if (!token || token === 'mock_token') {
				uni.redirectTo({ url: '/pages/login/forgot_password' })
				return
			}
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
					if (res && res.user && res.user.email) {
						this.email = res.user.email
					} else {
						// 如果没有绑定邮箱，直接进入第二步，使用旧密码修改
						this.step = 2
					}
				} catch (e) {
					console.error('Failed to fetch user profile', e)
				}
			},
			goBack() {
				uni.navigateBack()
			},
			sendCode() {
				this.onTapSendEmailCode(this.email, this.i18n)
			},
			onCaptchaModalConfirm() {
				this.confirmCaptchaModalSend(this.email, this.i18n)
			},
			async verifyEmail() {
				if (!this.code) {
					uni.showToast({ title: this.i18n.pleaseEnterVerificationCode, icon: 'none' })
					return
				}
				try {
					uni.showLoading({ title: this.i18n.verifying })
					await api.verifyEmailCode(this.email, this.code)
					uni.hideLoading()
					this.resetCaptchaAfterEmailVerified()
					this.step = 2
				} catch (e) {
					uni.hideLoading()
				}
			},
			async submitNewPassword() {
				if (!this.newPassword || !this.confirmPassword) {
					uni.showToast({ title: this.i18n.fillAllFields, icon: 'none' })
					return
				}
				if (!this.email && !this.oldPassword) {
					uni.showToast({ title: this.i18n.pleaseEnterOldPassword, icon: 'none' })
					return
				}
				if (this.newPassword !== this.confirmPassword) {
					uni.showToast({ title: this.i18n.passwordMismatch, icon: 'none' })
					return
				}
				try {
					uni.showLoading({ title: this.i18n.modifying })
					if (this.email) {
						await api.updatePwdByEmail(this.email, this.code, this.newPassword)
					} else {
						await api.updatePwd(this.oldPassword, this.newPassword)
					}
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
		position: relative;
		display: flex;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
		margin-bottom: 16rpx;
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

	.captcha-modal-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		z-index: 10000;
		background: rgba(15, 23, 42, 0.5);
		backdrop-filter: blur(8rpx);
		-webkit-backdrop-filter: blur(8rpx);
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 40rpx 36rpx;
		box-sizing: border-box;
	}

	.captcha-modal {
		position: relative;
		width: 100%;
		max-width: 600rpx;
		background: var(--page-bg-gradient);
		border-radius: 32rpx;
		padding: 44rpx 36rpx 36rpx;
		box-sizing: border-box;
		box-shadow: 0 28rpx 90rpx rgba(15, 23, 42, 0.18);
		border: 1px solid var(--border-color);
		overflow: hidden;
	}

	.theme-light .captcha-modal {
		box-shadow: 0 28rpx 100rpx rgba(51, 207, 255, 0.2);
	}

	.captcha-modal-accent {
		position: absolute;
		left: 0;
		top: 0;
		right: 0;
		height: 8rpx;
		background: linear-gradient(90deg, var(--accent-color), var(--accent-color-strong));
	}

	.captcha-modal-close {
		position: absolute;
		top: 20rpx;
		right: 24rpx;
		width: 56rpx;
		height: 56rpx;
		line-height: 52rpx;
		text-align: center;
		font-size: 44rpx;
		font-weight: 300;
		color: var(--text-tertiary);
		z-index: 2;
	}

	.captcha-modal-title {
		display: block;
		font-size: 36rpx;
		font-weight: 700;
		color: var(--text-primary);
		text-align: center;
		margin-bottom: 12rpx;
		padding: 0 48rpx;
	}

	.captcha-modal-desc {
		display: block;
		font-size: 24rpx;
		color: var(--text-secondary);
		text-align: center;
		line-height: 1.45;
		margin-bottom: 32rpx;
		padding: 0 8rpx;
	}

	.captcha-modal-img-block {
		width: 100%;
		margin-bottom: 28rpx;
	}

	.captcha-modal-img-card {
		width: 100%;
		height: 176rpx;
		border-radius: 20rpx;
		background: var(--control-bg);
		border: 1px solid var(--border-color);
		overflow: hidden;
	}

	.captcha-modal-image {
		width: 100%;
		height: 100%;
		display: block;
		background: #fff;
	}

	.captcha-modal-image--empty {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100%;
	}

	.captcha-modal-refresh-text {
		font-size: 26rpx;
		color: var(--text-secondary);
	}

	.captcha-modal-refresh-row {
		display: flex;
		align-items: center;
		justify-content: center;
		padding-top: 16rpx;
	}

	.captcha-modal-refresh-link {
		font-size: 28rpx;
		font-weight: 600;
		color: var(--accent-color-strong);
		padding: 8rpx 20rpx;
	}

	.captcha-field-label {
		display: block;
		font-size: 24rpx;
		font-weight: 600;
		color: var(--text-secondary);
		margin-bottom: 12rpx;
	}

	.captcha-modal-input {
		width: 100%;
		height: 96rpx;
		padding: 0 28rpx;
		box-sizing: border-box;
		font-size: 30rpx;
		color: var(--text-primary);
		background: var(--control-bg);
		border-radius: 20rpx;
		border: 1px solid var(--border-color);
		margin-bottom: 36rpx;
	}

	.captcha-placeholder {
		color: var(--text-tertiary);
		font-size: 28rpx;
	}

	.captcha-modal-submit {
		width: 100%;
		height: 96rpx;
		border-radius: 20rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
		box-shadow: 0 12rpx 32rpx rgba(51, 207, 255, 0.28);
	}

	.theme-dark .captcha-modal-submit {
		box-shadow: 0 12rpx 32rpx rgba(91, 140, 255, 0.22);
	}

	.captcha-modal-submit--busy {
		opacity: 0.72;
		pointer-events: none;
	}

	.captcha-modal-submit-text {
		font-size: 30rpx;
		font-weight: 700;
		color: var(--btn-text-white);
	}

	.captcha-modal-cancel-link {
		margin-top: 28rpx;
		text-align: center;
		padding: 12rpx;
	}

	.captcha-modal-cancel-link text {
		font-size: 28rpx;
		color: var(--text-secondary);
		font-weight: 500;
	}

	.captcha-modal-busy {
		position: absolute;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		border-radius: 32rpx;
		background: rgba(255, 255, 255, 0.82);
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 20rpx;
	}

	.theme-dark .captcha-modal-busy {
		background: rgba(16, 26, 48, 0.88);
	}

	.captcha-modal-spinner {
		width: 52rpx;
		height: 52rpx;
		border: 5rpx solid rgba(51, 207, 255, 0.25);
		border-top-color: var(--accent-color-strong);
		border-radius: 50%;
		animation: captcha-spin 0.7s linear infinite;
	}

	.theme-dark .captcha-modal-spinner {
		border-color: rgba(145, 180, 255, 0.2);
		border-top-color: #9db7ff;
	}

	.captcha-modal-busy-text {
		font-size: 26rpx;
		color: var(--text-secondary);
		font-weight: 500;
	}

	@keyframes captcha-spin {
		to {
			transform: rotate(360deg);
		}
	}

	.send-code-btn--disabled {
		opacity: 0.55;
		pointer-events: none;
	}

	.tips-card {
		background-color: var(--card-bg);
		border-radius: 16rpx;
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 16rpx;
		margin-bottom: 32rpx;
	}

	.tips-title {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.tips-desc {
		font-size: 28rpx;
		color: var(--text-secondary);
		line-height: 1.5;
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
		margin-bottom: 24rpx;
		
		&.readonly {
			opacity: 0.8;
		}
	}

	.input-field {
		flex: 1;
		font-size: 28rpx;
		color: var(--text-primary);
	}

	.placeholder-text {
		color: var(--text-secondary);
	}

	.send-code-btn {
		background-color: var(--btn-confirm-bg);
		padding: 16rpx 20rpx;
		border-radius: 12rpx;
	}

	.send-code-text {
		color: var(--btn-text-white);
		font-size: 24rpx;
		font-weight: bold;
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