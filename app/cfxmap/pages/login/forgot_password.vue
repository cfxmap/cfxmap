<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.forgotPasswordTitle }}</text>
		</view>

		<view v-if="captchaModalVisible" class="captcha-modal-mask" @click.self="closeCaptchaModal">
			<view class="captcha-modal" @click.stop>
				<view class="captcha-modal-close" @click="closeCaptchaModal">×</view>
				<text class="captcha-modal-title">{{ i18n.imageCaptchaPlaceholder }}</text>
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
							<text class="captcha-modal-refresh-text">{{ i18n.captchaRefresh }}</text>
						</view>
					</view>
					<view class="captcha-modal-refresh-row" @click="refreshImageCaptcha">
						<text class="captcha-modal-refresh-link">{{ i18n.captchaRefresh }}</text>
					</view>
				</view>

				<text class="captcha-field-label">{{ i18n.imageCaptchaPlaceholder }}</text>
				<input
					class="captcha-modal-input"
					type="text"
					:placeholder="i18n.imageCaptchaPlaceholder"
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

		<view class="tips-card">
			<text class="tips-title">{{ i18n.forgotPasswordTitle }}</text>
			<text class="tips-desc">{{ i18n.forgotPasswordDesc }}</text>
		</view>

		<view class="step-card">
			<view class="input-row">
				<input
					class="input-field"
					type="text"
					:placeholder="i18n.emailAddress"
					v-model="email"
					placeholder-class="placeholder-text"
				/>
			</view>

			<view class="input-row">
				<input
					class="input-field"
					type="text"
					:placeholder="i18n.enterEmailCode"
					v-model="code"
					placeholder-class="placeholder-text"
				/>
				<view class="send-code-btn" :class="{ 'send-code-btn--disabled': countdown > 0 }" @click="sendCode">
					<text class="send-code-text">{{ sendCodeButtonLabel(i18n) }}</text>
				</view>
			</view>

			<view class="input-row">
				<input
					class="input-field"
					type="password"
					:placeholder="i18n.enterNewPwd"
					v-model="newPassword"
					placeholder-class="placeholder-text"
				/>
			</view>

			<view class="input-row">
				<input
					class="input-field"
					type="password"
					:placeholder="i18n.confirmNewPwd"
					v-model="confirmPassword"
					placeholder-class="placeholder-text"
				/>
			</view>

			<button class="btn-primary" @click="submitReset">
				<text class="btn-text">{{ i18n.resetPwdBtn }}</text>
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
				email: '',
				code: '',
				newPassword: '',
				confirmPassword: ''
			}
		},
		computed: {
			...mapGetters(['i18n', 'themeClass'])
		},
		onLoad(options) {
			const token = uni.getStorageSync('token')
			if (token && token !== 'mock_token') {
				uni.switchTab({ url: '/pages/wallet/wallet' })
				return
			}
			if (options && options.email) {
				try {
					this.email = decodeURIComponent(options.email)
				} catch (e) {
					this.email = options.email
				}
			}
		},
		methods: {
			goBack() {
				uni.navigateBack({
					fail: () => {
						uni.reLaunch({ url: '/pages/login/login' })
					}
				})
			},
			sendCode() {
				const em = (this.email || '').trim()
				if (!em) {
					uni.showToast({ title: this.i18n.pleaseEnterEmail, icon: 'none' })
					return
				}
				this.onTapSendEmailCode(em, this.i18n)
			},
			onCaptchaModalConfirm() {
				const em = (this.email || '').trim()
				if (!em) {
					uni.showToast({ title: this.i18n.pleaseEnterEmail, icon: 'none' })
					return
				}
				this.confirmCaptchaModalSend(em, this.i18n)
			},
			async submitReset() {
				const em = (this.email || '').trim()
				if (!em) {
					uni.showToast({ title: this.i18n.pleaseEnterEmail, icon: 'none' })
					return
				}
				if (!(this.code || '').trim()) {
					uni.showToast({ title: this.i18n.pleaseEnterVerificationCode, icon: 'none' })
					return
				}
				if (!this.newPassword || !this.confirmPassword) {
					uni.showToast({ title: this.i18n.fillAllFields, icon: 'none' })
					return
				}
				if (this.newPassword.length < 6) {
					uni.showToast({ title: this.i18n.passwordAtLeast6Chars, icon: 'none' })
					return
				}
				if (this.newPassword !== this.confirmPassword) {
					uni.showToast({ title: this.i18n.passwordMismatch, icon: 'none' })
					return
				}
				try {
					uni.showLoading({ title: this.i18n.modifying })
					await api.resetPasswordByEmail(em, (this.code || '').trim(), this.newPassword)
					uni.hideLoading()
					uni.showToast({ title: this.i18n.modifySuccess, icon: 'success' })
					setTimeout(() => {
						uni.reLaunch({ url: '/pages/login/login' })
					}, 1200)
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

	.input-row {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: space-between;
		background-color: var(--control-bg);
		border-radius: 16rpx;
		padding: 24rpx 28rpx;
		margin-bottom: 24rpx;
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
