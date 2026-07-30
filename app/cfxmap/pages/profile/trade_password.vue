<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ pageTitle }}</text>
			<view class="placeholder"></view>
		</view>

		<!-- 与修改密码一致：多次发信需图形验证码 -->
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

		<view class="card-form">
			<template v-if="!hasTradePassword">
				<text class="hint">{{ i18n.tradePwdRequiredBody }}</text>
				<view class="input-group">
					<text class="input-label">{{ i18n.emailAddress }}</text>
					<view class="email-readonly" :class="{ 'email-readonly--empty': !email }">
						<text class="email-readonly-text">{{ email ? emailMasked : i18n.unbound }}</text>
					</view>
				</view>
				<view class="input-group">
					<text class="input-label">{{ i18n.tradePwdNew }}</text>
					<input class="input-field" type="password" v-model="newPwd" :placeholder="i18n.tradePwdNew" />
				</view>
				<view class="input-group">
					<text class="input-label">{{ i18n.tradePwdConfirm }}</text>
					<input class="input-field" type="password" v-model="confirmPwd" :placeholder="i18n.tradePwdConfirm" />
				</view>
				<view class="input-group">
					<text class="input-label">{{ i18n.tradePwdEmailCode }}</text>
					<!-- 发送按钮单独一行：避免小程序原生 input 同层盖住右侧按钮导致无法点击 -->
					<input
						class="input-field"
						type="number"
						v-model="emailCode"
						:placeholder="i18n.tradePwdEmailCode"
						placeholder-class="placeholder-text"
					/>
					<view class="send-code-wrap">
						<view
							class="send-code-btn"
							:class="{ 'send-code-btn--disabled': countdown > 0 }"
							@tap.stop="sendCode"
						>
							<text class="send-code-text">{{ sendCodeButtonLabel(i18n) }}</text>
						</view>
					</view>
				</view>
				<button class="btn-primary" @click="submitSet">{{ i18n.tradePwdSubmitSet }}</button>
			</template>

			<template v-else>
				<view class="input-group">
					<text class="input-label">{{ i18n.tradePwdOld }}</text>
					<input class="input-field" type="password" v-model="oldPwd" :placeholder="i18n.tradePwdOld" />
				</view>
				<view class="input-group">
					<text class="input-label">{{ i18n.tradePwdNew }}</text>
					<input class="input-field" type="password" v-model="newPwd" :placeholder="i18n.tradePwdNew" />
				</view>
				<view class="input-group">
					<text class="input-label">{{ i18n.tradePwdConfirm }}</text>
					<input class="input-field" type="password" v-model="confirmPwd" :placeholder="i18n.tradePwdConfirm" />
				</view>
				<button class="btn-primary" @click="submitUpdate">{{ i18n.tradePwdSubmitUpdate }}</button>
			</template>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import NavBackBtn from '@/components/nav-back-btn.vue'
	import emailCodeCaptchaModal from '@/mixins/email-code-captcha-modal.js'

	export default {
		components: { NavBackBtn },
		mixins: [emailCodeCaptchaModal],
		data() {
			return {
				hasTradePassword: false,
				email: '',
				oldPwd: '',
				newPwd: '',
				confirmPwd: '',
				emailCode: ''
			}
		},
		computed: {
			...mapGetters(['i18n']),
			pageTitle() {
				return this.hasTradePassword ? this.i18n.tradePwdUpdateTitle : this.i18n.tradePwdSetTitle
			},
			emailMasked() {
				if (!this.email) return ''
				const parts = this.email.split('@')
				if (parts.length !== 2) return this.email
				const head = parts[0]
				const show = head.length <= 1 ? head : head.substring(0, 1)
				return `${show}***@${parts[1]}`
			},
		},
		async onLoad() {
			await this.loadStatus()
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			async loadStatus() {
				try {
					const s = await api.getTradePwdStatus()
					this.hasTradePassword = !!(s && s.hasTradePassword)
				} catch (e) {
					this.hasTradePassword = false
				}
				try {
					const prof = await api.getUserProfile()
					if (prof && prof.user && prof.user.email) {
						this.email = (prof.user.email || '').trim()
					}
				} catch (e) {}
			},
			sendCode() {
				if (!this.email) {
					uni.showToast({ title: this.i18n.bindEmail || 'Bind email first', icon: 'none' })
					return
				}
				this.onTapSendEmailCode(this.email, this.i18n)
			},
			onCaptchaModalConfirm() {
				this.confirmCaptchaModalSend(this.email, this.i18n)
			},
			async submitSet() {
				if (!this.newPwd || this.newPwd.length < 6) {
					uni.showToast({ title: this.i18n.tradePwdNew, icon: 'none' })
					return
				}
				if (this.newPwd !== this.confirmPwd) {
					uni.showToast({ title: this.i18n.passwordMismatch, icon: 'none' })
					return
				}
				if (!this.emailCode) {
					uni.showToast({ title: this.i18n.tradePwdEmailCode, icon: 'none' })
					return
				}
				try {
					uni.showLoading({ title: this.i18n.modifying })
					await api.setTradePassword({
						email: this.email,
						code: this.emailCode,
						tradePassword: this.newPwd
					})
					uni.hideLoading()
					this.resetCaptchaAfterEmailVerified()
					uni.showToast({ title: this.i18n.tradePwdSetOk, icon: 'success' })
					setTimeout(() => uni.navigateBack(), 1200)
				} catch (e) {
					uni.hideLoading()
				}
			},
			async submitUpdate() {
				if (!this.oldPwd) {
					uni.showToast({ title: this.i18n.tradePwdOld, icon: 'none' })
					return
				}
				if (!this.newPwd || this.newPwd.length < 6) {
					uni.showToast({ title: this.i18n.tradePwdNew, icon: 'none' })
					return
				}
				if (this.newPwd !== this.confirmPwd) {
					uni.showToast({ title: this.i18n.passwordMismatch, icon: 'none' })
					return
				}
				try {
					uni.showLoading({ title: this.i18n.modifying })
					await api.updateTradePassword({
						oldTradePassword: this.oldPwd,
						newTradePassword: this.newPwd
					})
					uni.hideLoading()
					uni.showToast({ title: this.i18n.tradePwdUpdateOk, icon: 'success' })
					setTimeout(() => uni.navigateBack(), 1200)
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
		padding: 0 32rpx 48rpx;
		box-sizing: border-box;
	}

	.header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
	}

	.back-btn,
	.placeholder {
		width: 48rpx;
		height: 48rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.title {
		font-size: 36rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.card-form {
		background-color: var(--card-bg);
		padding: 40rpx 32rpx;
		border-radius: 28rpx;
		display: flex;
		flex-direction: column;
		gap: 28rpx;
	}

	.hint {
		font-size: 26rpx;
		color: var(--text-secondary);
		line-height: 1.5;
	}

	.input-group {
		display: flex;
		flex-direction: column;
		gap: 12rpx;
	}

	.input-label {
		font-size: 26rpx;
		color: var(--text-secondary);
	}

	.input-field {
		height: 88rpx;
		padding: 0 24rpx;
		border-radius: 20rpx;
		background: var(--control-bg);
		border: 1px solid var(--border-color);
		font-size: 28rpx;
		color: var(--text-primary);
		box-sizing: border-box;
	}

	.placeholder-text {
		color: var(--text-secondary);
		font-size: 26rpx;
	}

	.email-readonly {
		min-height: 88rpx;
		padding: 0 24rpx;
		border-radius: 20rpx;
		background: var(--control-bg);
		border: 1px solid var(--border-color);
		display: flex;
		align-items: center;
		box-sizing: border-box;
	}

	.email-readonly--empty {
		opacity: 0.85;
	}

	.email-readonly-text {
		font-size: 28rpx;
		color: var(--text-primary);
	}

	.send-code-wrap {
		display: flex;
		flex-direction: row;
		justify-content: flex-end;
		margin-top: 4rpx;
	}

	.send-code-btn {
		padding: 16rpx 28rpx;
		border-radius: 16rpx;
		background-color: var(--btn-confirm-bg);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.send-code-btn--disabled {
		opacity: 0.55;
		pointer-events: none;
	}

	.send-code-text {
		color: var(--btn-text-white);
		font-size: 24rpx;
		font-weight: bold;
		line-height: 1.25;
		text-align: center;
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

	.btn-primary {
		margin-top: 16rpx;
		height: 96rpx;
		line-height: 96rpx;
		border-radius: 24rpx;
		background: var(--btn-confirm-bg);
		color: var(--btn-text-white);
		font-size: 30rpx;
		font-weight: 600;
		border: none;
	}

	.btn-primary::after {
		border: none;
	}
</style>
