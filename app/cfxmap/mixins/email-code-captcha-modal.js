import { api } from '@/utils/request'

/**
 * 邮箱验证码发送（单通道）：本页独立倒计时与失败计数，与登录/绑定页互不共用状态
 */
export default {
	data() {
		return {
			countdown: 0,
			countdownTimer: null,
			sendCodeFailCount: 0,
			captchaRequired: false,
			captchaModalVisible: false,
			captchaSubmitting: false,
			captchaCode: '',
			captchaImage: '',
			captchaUuid: ''
		}
	},
	beforeDestroy() {
		this.clearEmailCodeCountdown()
	},
	onUnload() {
		this.clearEmailCodeCountdown()
	},
	methods: {
		clearEmailCodeCountdown() {
			if (this.countdownTimer) {
				clearInterval(this.countdownTimer)
				this.countdownTimer = null
			}
		},
		startEmailCodeCountdown() {
			this.clearEmailCodeCountdown()
			this.countdown = 60
			this.countdownTimer = setInterval(() => {
				if (this.countdown <= 1) {
					this.countdown = 0
					this.clearEmailCodeCountdown()
					return
				}
				this.countdown -= 1
			}, 1000)
		},
		async refreshImageCaptcha() {
			try {
				const res = await api.getImageCaptcha()
				if (res && res.captchaEnabled === false) {
					this.captchaImage = ''
					this.captchaUuid = ''
					return
				}
				this.captchaImage = res && res.img ? `data:image/gif;base64,${res.img}` : ''
				this.captchaUuid = res && res.uuid ? res.uuid : ''
			} catch (e) {
				this.captchaImage = ''
				this.captchaUuid = ''
			}
		},
		closeCaptchaModal() {
			if (this.captchaSubmitting) {
				return
			}
			this.captchaModalVisible = false
			this.captchaCode = ''
		},
		/** 邮箱验证码校验通过后：后续重发不再需要图形验证码 */
		resetCaptchaAfterEmailVerified() {
			this.sendCodeFailCount = 0
			this.captchaRequired = false
			this.captchaSubmitting = false
			this.captchaModalVisible = false
			this.captchaCode = ''
			this.captchaImage = ''
			this.captchaUuid = ''
		},
		sendCodeButtonLabel(i18n) {
			if (this.countdown > 0) {
				return (i18n.sendCodeResendIn || '').replace('{n}', this.countdown)
			}
			return i18n.sendCode || 'Send Code'
		},
		/**
		 * @param {string} email
		 * @param {object} i18n - mapGetters i18n
		 * @returns {Promise<boolean>} 是否发送成功
		 */
		async requestSendEmailCode(email, i18n, payload = {}, options = {}) {
			const useGlobalLoading = options.useGlobalLoading !== false
			if (useGlobalLoading) {
				uni.showLoading({ title: i18n.sending, mask: true })
			}
			try {
				await api.sendEmailCode(email, payload)
				if (useGlobalLoading) {
					uni.hideLoading()
				}
				uni.showToast({ title: i18n.emailCodeSent, icon: 'success' })
				this.sendCodeFailCount = 0
				this.captchaRequired = false
				this.captchaCode = ''
				this.captchaUuid = ''
				this.captchaModalVisible = false
				this.startEmailCodeCountdown()
				return true
			} catch (e) {
				if (useGlobalLoading) {
					uni.hideLoading()
				}
				const msg = (e && e.msg) || ''
				const hadCaptchaPayload = !!(payload && payload.code)
				this.sendCodeFailCount += 1
				if (this.sendCodeFailCount >= 3 || (msg && msg.includes('图形验证码')) || hadCaptchaPayload) {
					this.captchaRequired = true
					this.captchaModalVisible = true
					this.captchaCode = ''
					await this.refreshImageCaptcha()
				}
				if (msg) {
					uni.showToast({ title: msg, icon: 'none' })
				}
				return false
			}
		},
		/**
		 * 主按钮「发送验证码」：无图形码要求时直接发；已要求则只打开弹框
		 */
		async onTapSendEmailCode(email, i18n) {
			if (this.countdown > 0) {
				return
			}
			if (!email) {
				uni.showToast({ title: i18n.emailNotBound || i18n.pleaseEnterNewEmailToast || 'Enter email', icon: 'none' })
				return
			}
			if (this.captchaRequired) {
				this.captchaModalVisible = true
				if (!this.captchaUuid) {
					await this.refreshImageCaptcha()
				}
				return
			}
			await this.requestSendEmailCode(email, i18n, {})
		},
		/**
		 * 弹框内确认：带图形验证码发送
		 */
		async confirmCaptchaModalSend(email, i18n) {
			if (this.captchaSubmitting) {
				return
			}
			if (!this.captchaCode) {
				uni.showToast({ title: i18n.pleaseEnterImageCaptcha, icon: 'none' })
				return
			}
			if (!this.captchaUuid) {
				await this.refreshImageCaptcha()
				return
			}
			this.captchaSubmitting = true
			try {
				await this.requestSendEmailCode(
					email,
					i18n,
					{
						code: this.captchaCode,
						uuid: this.captchaUuid
					},
					{ useGlobalLoading: false }
				)
			} finally {
				this.captchaSubmitting = false
			}
		}
	}
}
