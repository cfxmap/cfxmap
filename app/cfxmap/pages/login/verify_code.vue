<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="content-shell">
			<view class="header">
				<view class="header-row">
					<view class="back-btn" @click="goBack">
						<nav-back-btn />
					</view>
					<view class="header-text">
						<text class="title">{{ i18n.verifyCodeTitle || 'Enter verification code' }}</text>
						<text class="subtitle">{{ sentToLine }}</text>
					</view>
				</view>
			</view>

			<view class="code-card" @click="focusInput = true">
				<input
					class="hidden-input"
					type="text"
					:focus="focusInput"
					:value="code"
					maxlength="4"
					:placeholder="i18n.verifyCodePlaceholder || ''"
					placeholder-class="placeholder-text"
					@input="onCodeInput"
					@blur="focusInput = false"
				/>
				<view class="digit-row">
					<view
						v-for="(digit, index) in codeDigits"
						:key="index"
						class="digit-box"
						:class="{ active: code.length === index }"
					>
						<text class="digit-text">{{ digit }}</text>
					</view>
				</view>
				<text class="tip-text">{{ i18n.verifyCodeAutoLoginTip || '' }}</text>
			</view>

			<view v-if="captchaVisible" class="captcha-row">
				<input
					class="captcha-input"
					type="text"
					:placeholder="i18n.imageCaptchaPlaceholder || ''"
					v-model="captchaCode"
					placeholder-class="placeholder-text"
				/>
				<image
					v-if="captchaImage"
					class="captcha-image"
					:src="captchaImage"
					mode="aspectFill"
					@click="refreshImageCaptcha"
				></image>
				<view v-else class="captcha-image captcha-image-empty" @click="refreshImageCaptcha">
					<text class="captcha-image-empty-text">{{ i18n.captchaRefresh || 'Refresh' }}</text>
				</view>
			</view>

			<view class="footer">
				<text class="footer-text">{{ countdown > 0 ? (i18n.sendCodeResendIn || '').replace('{n}', countdown) : (i18n.verifyCodeNotReceived || '') }}</text>
				<text class="resend-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">{{ i18n.resendCode || 'Resend' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	const BIOMETRIC_STORAGE_KEY = 'app_biometric_login_info'
	const DEVICE_INSTALLATION_KEY = 'app_biometric_device_id'

	const getDeviceProfile = () => {
		let installationId = uni.getStorageSync(DEVICE_INSTALLATION_KEY)
		if (!installationId) {
			installationId = `device_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
			uni.setStorageSync(DEVICE_INSTALLATION_KEY, installationId)
		}

		let systemInfo = {}
		try {
			systemInfo = uni.getSystemInfoSync() || {}
		} catch (e) {}

		return {
			deviceId: installationId,
			deviceName: [systemInfo.brand, systemInfo.model].filter(Boolean).join(' ') || systemInfo.model || 'current-device',
			platform: systemInfo.platform || 'unknown'
		}
	}

	export default {
		components: { NavBackBtn },
		data() {
			return {
				email: '',
				code: '',
				countdown: 0,
				countdownTimer: null,
				sendCodeFailCount: 0,
				captchaVisible: false,
				captchaCode: '',
				captchaImage: '',
				captchaUuid: '',
				focusInput: true,
				submitting: false,
				biometricInfo: null,
				biometricAvailable: false
			}
		},
		computed: {
			...mapGetters(['i18n']),
			codeDigits() {
				return Array.from({ length: 4 }, (_, index) => this.code[index] || '')
			},
			sentToLine() {
				const tpl = this.i18n.verifyCodeSentTo || ''
				return tpl.replace(/\{email\}/g, this.email || '')
			}
		},
		async onLoad(options) {
			const token = uni.getStorageSync('token')
			if (token && token !== 'mock_token') {
				uni.switchTab({
					url: '/pages/wallet/wallet'
				})
				return
			}

			this.email = options && options.email ? decodeURIComponent(options.email) : ''
			if (!this.email) {
				uni.showToast({ title: this.i18n.emailCannotBeEmpty || 'Email is required', icon: 'none' })
				uni.reLaunch({
					url: '/pages/login/login'
				})
				return
			}

			await this.initBiometricState()
			this.startCountdown()
		},
		onUnload() {
			this.clearCountdownTimer()
		},
		methods: {
			clearCountdownTimer() {
				if (this.countdownTimer) {
					clearInterval(this.countdownTimer)
					this.countdownTimer = null
				}
			},
			startCountdown() {
				this.clearCountdownTimer()
				this.countdown = 60
				this.countdownTimer = setInterval(() => {
					if (this.countdown <= 1) {
						this.countdown = 0
						this.clearCountdownTimer()
						return
					}
					this.countdown -= 1
				}, 1000)
			},
			async refreshImageCaptcha() {
				try {
					const res = await api.getImageCaptcha()
					if (res && res.captchaEnabled === false) {
						this.captchaVisible = false
						return
					}
					this.captchaImage = res && res.img ? `data:image/gif;base64,${res.img}` : ''
					this.captchaUuid = res && res.uuid ? res.uuid : ''
				} catch (e) {
					this.captchaImage = ''
					this.captchaUuid = ''
				}
			},
			async sendCode() {
				if (this.countdown > 0) {
					return
				}
				if (this.captchaVisible) {
					if (!this.captchaUuid) {
						await this.refreshImageCaptcha()
					}
					if (!this.captchaCode) {
						uni.showToast({ title: this.i18n.pleaseEnterImageCaptcha || '', icon: 'none' })
						return
					}
				}
				try {
					uni.showLoading({ title: this.i18n.sending || '' })
					await api.sendEmailCode(this.email, {
						code: this.captchaCode,
						uuid: this.captchaUuid
					})
					uni.hideLoading()
					uni.showToast({ title: this.i18n.emailCodeSent || '', icon: 'success' })
					this.sendCodeFailCount = 0
					this.captchaVisible = false
					this.captchaCode = ''
					this.captchaUuid = ''
					this.startCountdown()
				} catch (e) {
					uni.hideLoading()
					const msg = (e && e.msg) || ''
					this.sendCodeFailCount += 1
					const needCaptcha =
						this.sendCodeFailCount >= 3 || (msg && msg.includes('图形验证码'))
					if (needCaptcha) {
						this.captchaVisible = true
					}
					if (this.captchaVisible) {
						this.captchaCode = ''
						await this.refreshImageCaptcha()
					}
					if (msg) {
						uni.showToast({ title: msg, icon: 'none' })
					}
				}
			},
			async getBiometricCapability(silent = true) {
				if (
					typeof uni.checkIsSupportSoterAuthentication !== 'function' ||
					typeof uni.startSoterAuthentication !== 'function'
				) {
					if (!silent) {
						uni.showToast({ title: this.i18n.biometricNotSupportedPlatform || '', icon: 'none' })
					}
					return { supported: false, enrolled: false }
				}

				const supportRes = await new Promise(resolve => {
					uni.checkIsSupportSoterAuthentication({
						success: resolve,
						fail: () => resolve({ supportMode: [] })
					})
				})
				const supportModes = Array.isArray(supportRes.supportMode) ? supportRes.supportMode : []
				if (!supportModes.includes('fingerPrint')) {
					if (!silent) {
						uni.showToast({ title: this.i18n.biometricCapabilityUnavailable || '', icon: 'none' })
					}
					return { supported: false, enrolled: false }
				}

				if (typeof uni.checkIsSoterEnrolledInDevice !== 'function') {
					return { supported: true, enrolled: true }
				}

				const enrolledRes = await new Promise(resolve => {
					uni.checkIsSoterEnrolledInDevice({
						checkAuthMode: 'fingerPrint',
						success: resolve,
						fail: () => resolve({ isEnrolled: false })
					})
				})

				if (!enrolledRes.isEnrolled && !silent) {
					uni.showToast({ title: this.i18n.enrollFingerprintInSystem || '', icon: 'none' })
				}

				return {
					supported: true,
					enrolled: !!enrolledRes.isEnrolled
				}
			},
			async initBiometricState() {
				this.biometricInfo = uni.getStorageSync(BIOMETRIC_STORAGE_KEY) || null
				const capability = await this.getBiometricCapability(true)
				this.biometricAvailable = capability.supported && capability.enrolled
			},
			async authenticateWithFingerprint(authContent) {
				const capability = await this.getBiometricCapability(false)
				this.biometricAvailable = capability.supported && capability.enrolled
				if (!this.biometricAvailable) {
					return false
				}

				return new Promise(resolve => {
					uni.startSoterAuthentication({
						requestAuthModes: ['fingerPrint'],
						challenge: `${Date.now()}`,
						authContent,
						success: () => resolve(true),
						fail: () => {
							uni.showToast({ title: this.i18n.fingerprintAuthFailed || '', icon: 'none' })
							resolve(false)
						}
					})
				})
			},
			async promptEnableBiometricLogin() {
				const capability = await this.getBiometricCapability(true)
				this.biometricAvailable = capability.supported && capability.enrolled
				if (!this.biometricAvailable) {
					return
				}
				if (this.biometricInfo && this.biometricInfo.email === this.email) {
					return
				}

				const modalRes = await new Promise(resolve => {
					uni.showModal({
						title: this.i18n.enableFingerprintLoginTitle || '',
						content: this.i18n.enableFingerprintLoginMessage || '',
						confirmText: this.i18n.confirmBtn,
						cancelText: this.i18n.cancelBtn,
						success: resolve,
						fail: () => resolve({ confirm: false })
					})
				})
				if (!modalRes.confirm) {
					return
				}

				const passed = await this.authenticateWithFingerprint(this.i18n.verifyFingerprintToEnableQuickLogin || '')
				if (!passed) {
					return
				}

				try {
					const deviceProfile = getDeviceProfile()
					const res = await api.enableBiometricLogin({
						email: this.email,
						deviceId: deviceProfile.deviceId,
						deviceName: deviceProfile.deviceName,
						platform: deviceProfile.platform,
						authMode: 'fingerPrint'
					})
					const biometricInfo = {
						email: this.email,
						authMode: 'fingerPrint',
						deviceId: deviceProfile.deviceId,
						deviceName: deviceProfile.deviceName,
						biometricToken: res.biometricToken
					}
					uni.setStorageSync(BIOMETRIC_STORAGE_KEY, biometricInfo)
					this.biometricInfo = biometricInfo
					uni.showToast({ title: this.i18n.fingerprintLoginEnabled || '', icon: 'success' })
				} catch (e) {}
			},
			onCodeInput(event) {
				const nextValue = ((event && event.detail && event.detail.value) || '')
					.replace(/\D/g, '')
					.slice(0, 4)
				this.code = nextValue
				this.focusInput = true

				if (nextValue.length === 4) {
					this.handleLogin()
				}
			},
			async handleLogin() {
				if (this.submitting || this.code.length !== 4) {
					return
				}

				this.submitting = true
				uni.showLoading({ title: this.i18n.loggingIn || '' })

				try {
					const res = await api.loginEmail(this.email, this.code)
					uni.hideLoading()
					if (res && res.access_token) {
						uni.setStorageSync('token', res.access_token)
						this.sendCodeFailCount = 0
						this.captchaVisible = false
						this.captchaCode = ''
						this.captchaUuid = ''
					} else {
						uni.showToast({ title: this.i18n.loginSuccessNoCredential || '', icon: 'none' })
						return
					}
					uni.setStorageSync('is_logged_in', true)
					await this.promptEnableBiometricLogin()
					uni.switchTab({
						url: '/pages/wallet/wallet'
					})
				} catch (e) {
					uni.hideLoading()
				} finally {
					this.submitting = false
				}
			},
			goBack() {
				uni.navigateBack()
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		padding: 120rpx 40rpx 72rpx;
		box-sizing: border-box;
	}

	.theme-dark.container {
		background:
			radial-gradient(circle at top left, rgba(51, 207, 255, 0.16), transparent 38%),
			radial-gradient(circle at top right, rgba(122, 162, 255, 0.14), transparent 34%),
			var(--page-bg-gradient);
	}

	.content-shell {
		min-height: calc(100vh - 192rpx);
		display: flex;
		flex-direction: column;
		justify-content: center;
		gap: 40rpx;
	}

	.header {
		width: 100%;
	}

	.header-row {
		display: flex;
		flex-direction: row;
		align-items: flex-start;
		gap: 20rpx;
	}

	.back-btn {
		flex-shrink: 0;
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.header-text {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 12rpx;
		padding-top: 4rpx;
	}

	.title {
		font-size: 52rpx;
		font-weight: 700;
		color: var(--text-primary);
		line-height: 1.25;
	}

	.subtitle {
		font-size: 28rpx;
		line-height: 1.6;
		color: var(--text-secondary);
		word-break: break-all;
	}

	.code-card {
		background-color: var(--card-bg, rgba(255, 255, 255, 0.94));
		border: var(--card-border, 1px solid rgba(18, 20, 29, 0.06));
		border-radius: 32rpx;
		padding: 40rpx 32rpx;
		box-shadow: var(--card-shadow, 0 18rpx 60rpx rgba(18, 20, 29, 0.08));
		display: flex;
		flex-direction: column;
		gap: 28rpx;
	}

	.hidden-input {
		height: 0;
		opacity: 0;
	}

	.digit-row {
		display: flex;
		justify-content: space-between;
		gap: 20rpx;
	}

	.digit-box {
		flex: 1;
		height: 112rpx;
		border-radius: 24rpx;
		border: 2rpx solid var(--border-color);
		background-color: var(--input-bg);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.digit-box.active {
		border-color: var(--accent-color);
		box-shadow: 0 0 0 4rpx rgba(51, 207, 255, 0.12);
	}

	.digit-text {
		font-size: 48rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.tip-text {
		text-align: center;
		font-size: 26rpx;
		color: var(--text-secondary);
	}

	.captcha-row {
		display: flex;
		align-items: center;
		gap: 20rpx;
		background-color: var(--input-bg);
		border-radius: 24rpx;
		padding: 24rpx 20rpx 24rpx 28rpx;
		border: 1px solid var(--border-color);
	}

	.captcha-input {
		flex: 1;
		font-size: 32rpx;
		color: var(--text-primary);
	}

	.placeholder-text {
		color: var(--text-secondary);
	}

	.captcha-image {
		width: 180rpx;
		height: 72rpx;
		border-radius: 16rpx;
		flex-shrink: 0;
	}

	.captcha-image-empty {
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(51, 207, 255, 0.12);
	}

	.captcha-image-empty-text {
		font-size: 24rpx;
		color: var(--accent-color);
	}

	.footer {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 12rpx;
		flex-wrap: wrap;
	}

	.footer-text {
		font-size: 26rpx;
		color: var(--text-secondary);
	}

	.resend-btn {
		font-size: 26rpx;
		font-weight: 600;
		color: var(--accent-color);
	}

	.resend-btn.disabled {
		opacity: 0.5;
	}
</style>
