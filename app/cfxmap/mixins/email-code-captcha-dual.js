import { api } from '@/utils/request'

/**
 * 双通道：当前邮箱 / 新邮箱 各自 60s 倒计时、各自失败计数、各自图形验证码（图/码/uuid 互不共用）
 */
export default {
	data() {
		return {
			countdownOld: 0,
			countdownNew: 0,
			timerOld: null,
			timerNew: null,
			failOld: 0,
			failNew: 0,
			captchaRequiredOld: false,
			captchaRequiredNew: false,
			captchaTarget: 'old',
			captchaModalVisible: false,
			captchaSubmitting: false,
			captchaCodeOld: '',
			captchaCodeNew: '',
			captchaImageOld: '',
			captchaImageNew: '',
			captchaUuidOld: '',
			captchaUuidNew: ''
		}
	},
	computed: {
		modalCaptchaImage() {
			return this.captchaTarget === 'old' ? this.captchaImageOld : this.captchaImageNew
		},
		modalCaptchaCode: {
			get() {
				return this.captchaTarget === 'old' ? this.captchaCodeOld : this.captchaCodeNew
			},
			set(v) {
				if (this.captchaTarget === 'old') {
					this.captchaCodeOld = v
				} else {
					this.captchaCodeNew = v
				}
			}
		}
	},
	beforeDestroy() {
		this.clearDualTimers()
	},
	onUnload() {
		this.clearDualTimers()
	},
	methods: {
		clearDualTimers() {
			if (this.timerOld) {
				clearInterval(this.timerOld)
				this.timerOld = null
			}
			if (this.timerNew) {
				clearInterval(this.timerNew)
				this.timerNew = null
			}
		},
		startCountdownOld() {
			if (this.timerOld) {
				clearInterval(this.timerOld)
				this.timerOld = null
			}
			this.countdownOld = 60
			this.timerOld = setInterval(() => {
				if (this.countdownOld <= 1) {
					this.countdownOld = 0
					clearInterval(this.timerOld)
					this.timerOld = null
					return
				}
				this.countdownOld -= 1
			}, 1000)
		},
		startCountdownNew() {
			if (this.timerNew) {
				clearInterval(this.timerNew)
				this.timerNew = null
			}
			this.countdownNew = 60
			this.timerNew = setInterval(() => {
				if (this.countdownNew <= 1) {
					this.countdownNew = 0
					clearInterval(this.timerNew)
					this.timerNew = null
					return
				}
				this.countdownNew -= 1
			}, 1000)
		},
		async refreshImageCaptchaFor(channel) {
			const isOld = channel === 'old'
			try {
				const res = await api.getImageCaptcha()
				if (res && res.captchaEnabled === false) {
					if (isOld) {
						this.captchaImageOld = ''
						this.captchaUuidOld = ''
					} else {
						this.captchaImageNew = ''
						this.captchaUuidNew = ''
					}
					return
				}
				const img = res && res.img ? `data:image/gif;base64,${res.img}` : ''
				const uuid = res && res.uuid ? res.uuid : ''
				if (isOld) {
					this.captchaImageOld = img
					this.captchaUuidOld = uuid
				} else {
					this.captchaImageNew = img
					this.captchaUuidNew = uuid
				}
			} catch (e) {
				if (isOld) {
					this.captchaImageOld = ''
					this.captchaUuidOld = ''
				} else {
					this.captchaImageNew = ''
					this.captchaUuidNew = ''
				}
			}
		},
		async refreshImageCaptcha() {
			await this.refreshImageCaptchaFor(this.captchaTarget)
		},
		closeCaptchaModal() {
			if (this.captchaSubmitting) {
				return
			}
			this.captchaModalVisible = false
			if (this.captchaTarget === 'old') {
				this.captchaCodeOld = ''
			} else {
				this.captchaCodeNew = ''
			}
		},
		/** 当前邮箱验证码校验通过后：该通道后续重发不再需要图形验证码 */
		resetCaptchaAfterOldEmailVerified() {
			this.failOld = 0
			this.captchaRequiredOld = false
			this.captchaCodeOld = ''
			this.captchaImageOld = ''
			this.captchaUuidOld = ''
			if (this.captchaModalVisible && this.captchaTarget === 'old') {
				this.captchaModalVisible = false
			}
		},
		/** 新邮箱验证码已用于完成绑定后：清理新通道图形码状态 */
		resetCaptchaAfterNewEmailVerified() {
			this.failNew = 0
			this.captchaRequiredNew = false
			this.captchaCodeNew = ''
			this.captchaImageNew = ''
			this.captchaUuidNew = ''
			if (this.captchaModalVisible && this.captchaTarget === 'new') {
				this.captchaModalVisible = false
			}
		},
		labelSendOld(i18n) {
			if (this.countdownOld > 0) {
				return (i18n.sendCodeResendIn || '').replace('{n}', this.countdownOld)
			}
			return i18n.sendCode || 'Send Code'
		},
		labelSendNew(i18n) {
			if (this.countdownNew > 0) {
				return (i18n.sendCodeResendIn || '').replace('{n}', this.countdownNew)
			}
			return i18n.sendCode || 'Send Code'
		},
		clearCaptchaFieldsForChannel(channel) {
			if (channel === 'old') {
				this.captchaCodeOld = ''
				this.captchaImageOld = ''
				this.captchaUuidOld = ''
			} else {
				this.captchaCodeNew = ''
				this.captchaImageNew = ''
				this.captchaUuidNew = ''
			}
		},
		async requestSendTo(email, i18n, payload, channel, options = {}) {
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
				if (channel === 'old') {
					this.failOld = 0
					this.captchaRequiredOld = false
					this.startCountdownOld()
				} else {
					this.failNew = 0
					this.captchaRequiredNew = false
					this.startCountdownNew()
				}
				this.clearCaptchaFieldsForChannel(channel)
				this.captchaModalVisible = false
				return true
			} catch (e) {
				if (useGlobalLoading) {
					uni.hideLoading()
				}
				const msg = (e && e.msg) || ''
				const hadCaptchaPayload = !!(payload && payload.code)
				if (channel === 'old') {
					this.failOld += 1
					const needCaptchaUi =
						this.failOld >= 3 || (msg && msg.includes('图形验证码')) || hadCaptchaPayload
					if (needCaptchaUi) {
						this.captchaRequiredOld = true
						this.captchaTarget = 'old'
						this.captchaModalVisible = true
						this.captchaCodeOld = ''
						await this.refreshImageCaptchaFor('old')
					}
				} else {
					this.failNew += 1
					const needCaptchaUi =
						this.failNew >= 3 || (msg && msg.includes('图形验证码')) || hadCaptchaPayload
					if (needCaptchaUi) {
						this.captchaRequiredNew = true
						this.captchaTarget = 'new'
						this.captchaModalVisible = true
						this.captchaCodeNew = ''
						await this.refreshImageCaptchaFor('new')
					}
				}
				if (msg) {
					uni.showToast({ title: msg, icon: 'none' })
				}
				return false
			}
		},
		async onTapSendOldCode(currentEmail, i18n) {
			if (this.countdownOld > 0) {
				return
			}
			if (!currentEmail) {
				return
			}
			if (this.captchaRequiredOld) {
				this.captchaTarget = 'old'
				this.captchaModalVisible = true
				if (!this.captchaUuidOld) {
					await this.refreshImageCaptchaFor('old')
				}
				return
			}
			await this.requestSendTo(currentEmail, i18n, {}, 'old')
		},
		async onTapSendNewCode(newEmail, i18n) {
			if (this.countdownNew > 0) {
				return
			}
			if (!newEmail) {
				uni.showToast({ title: i18n.pleaseEnterNewEmailToast, icon: 'none' })
				return
			}
			if (this.captchaRequiredNew) {
				this.captchaTarget = 'new'
				this.captchaModalVisible = true
				if (!this.captchaUuidNew) {
					await this.refreshImageCaptchaFor('new')
				}
				return
			}
			await this.requestSendTo(newEmail, i18n, {}, 'new')
		},
		async confirmCaptchaModalSend(currentEmail, newEmail, i18n) {
			if (this.captchaSubmitting) {
				return
			}
			const channel = this.captchaTarget
			const code = channel === 'old' ? this.captchaCodeOld : this.captchaCodeNew
			const uuid = channel === 'old' ? this.captchaUuidOld : this.captchaUuidNew
			if (!code) {
				uni.showToast({ title: i18n.pleaseEnterImageCaptcha, icon: 'none' })
				return
			}
			if (!uuid) {
				await this.refreshImageCaptchaFor(channel)
				return
			}
			const email = channel === 'old' ? currentEmail : newEmail
			this.captchaSubmitting = true
			try {
				await this.requestSendTo(
					email,
					i18n,
					{ code, uuid },
					channel,
					{ useGlobalLoading: false }
				)
			} finally {
				this.captchaSubmitting = false
			}
		}
	}
}
