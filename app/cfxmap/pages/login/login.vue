<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="particle-layer">
			<view
				v-for="particle in particles"
				:key="particle.id"
				class="particle-item"
				:style="particle.style"
			></view>
			<view class="particle-glow particle-glow-left"></view>
			<view class="particle-glow particle-glow-right"></view>
		</view>

		<view class="login-top-bar">
			<view class="login-top-pill" @click="toggleLanguage">
				<text class="login-top-pill-text">{{ displayLanguageLabel }}</text>
			</view>
			<view class="login-top-pill" @click="toggleTheme">
				<text class="login-top-pill-text">{{ displayTheme }}</text>
			</view>
		</view>

		<view class="content-shell">
			<view class="top-area">
				<view class="logo-wrap">
					<image class="logo" :src="$staticUrl('/static/logo.png')" mode="aspectFill"></image>
				</view>
				<text class="slogan">{{ i18n.loginSloganTitle }}</text>
				<text class="sub-slogan">{{ i18n.loginSloganSub }}</text>
			</view>

			<view class="bottom-area">
				<view class="title-row">
					<text class="panel-title">{{ i18n.loginBtn }}</text>
					<text class="panel-desc">{{ i18n.noAccountYet }}</text>
				</view>

				<view class="input-group">
					<view class="input-row">
						<input
							class="input-field"
							type="text"
							:placeholder="i18n.emailAddress || '邮箱地址'"
							v-model="email"
							placeholder-class="placeholder-text"
						/>
						<text class="icon-mail">✉️</text>
					</view>

					<view class="input-row">
						<input
							class="input-field"
							type="password"
							:placeholder="i18n.password || '密码'"
							v-model="password"
							placeholder-class="placeholder-text"
						/>
						<text class="icon-lock">🔒</text>
					</view>

					<view class="options-row">
						<view class="remember-row-wrap">
							<view class="remember-row" @click.stop="rememberAccountChecked = !rememberAccountChecked">
								<text class="remember-box">{{ rememberAccountChecked ? '☑' : '☐' }}</text>
								<text class="remember-text">{{ i18n.saveLoginAccount }}</text>
							</view>
							<view class="remember-row" @click.stop="rememberPasswordChecked = !rememberPasswordChecked">
								<text class="remember-box">{{ rememberPasswordChecked ? '☑' : '☐' }}</text>
								<text class="remember-text">{{ i18n.saveLoginPassword }}</text>
							</view>
						</view>
						<text class="helper-link" @click="goChangePassword">{{ i18n.forgotPassword || '忘记密码？' }}</text>
					</view>

					<view v-if="showImageCaptcha" class="input-row captcha-row">
						<input
							class="input-field"
							type="text"
							:placeholder="i18n.imageCaptchaPlaceholder || 'Image captcha'"
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
				</view>

				<view class="action-row">
					<button class="btn-login action-btn" @click="handleLogin">
						<text class="btn-text">{{ i18n.loginBtn || '登 录' }}</text>
					</button>
					<button class="btn-register action-btn" @click="goRegister">
						<text class="btn-register-text">{{ i18n.registerBtn }}</text>
					</button>
				</view>
				<button v-if="biometricInfo && biometricAvailable" class="btn-biometric" @click="loginWithFingerprint">
					<text class="btn-biometric-text">{{ biometricLoading ? (i18n.verifying || '') : (i18n.signInWithFingerprint || '') }}</text>
				</button>
			</view>
			<view v-if="formattedDisplayVersion" class="page-version">
				<text class="page-version-text">{{ versionLabel }} {{ formattedDisplayVersion }}</text>
			</view>
		</view>
		<!-- #ifdef H5 -->
		<h5-add-to-home-tip />
		<!-- #endif -->
	</view>
</template>

<script>
	import { mapGetters, mapState } from 'vuex'
	import { api } from '@/utils/request'
	import { fetchServerVersionName } from '@/utils/app-version-api'
	import { formatDisplayVersion } from '@/config/app-version.js'
	import { saveLoginRemember, clearLoginRemember, loadLoginRemember } from '@/utils/login-remember'
	// #ifdef H5
	import H5AddToHomeTip from '@/components/h5-add-to-home-tip.vue'
	// #endif

	const BIOMETRIC_STORAGE_KEY = 'app_biometric_login_info'
	const DEVICE_INSTALLATION_KEY = 'app_biometric_device_id'

	const buildParticles = () =>
		Array.from({ length: 18 }, (_, index) => {
			const size = 12 + (index % 4) * 8 + Math.round(Math.random() * 6)
			const left = (index * 13 + Math.random() * 20) % 100
			const top = (index * 17 + Math.random() * 18) % 100
			const duration = 12 + (index % 5) * 3
			const delay = -(index * 1.1)
			/* 浅色底需要更高不透明度，深色下仍自然 */
			const opacity = 0.28 + (index % 4) * 0.07

			return {
				id: `particle-${index}`,
				style: {
					width: `${size}rpx`,
					height: `${size}rpx`,
					left: `${left}%`,
					top: `${top}%`,
					opacity: `${opacity}`,
					animationDuration: `${duration}s`,
					animationDelay: `${delay}s`
				}
			}
		})

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
		components: {
			// #ifdef H5
			H5AddToHomeTip
			// #endif
		},
		data() {
			return {
				email: '',
				password: '',
				particles: buildParticles(),
				passwordLoginFailCount: 0,
				captchaCode: '',
				captchaImage: '',
				captchaUuid: '',
				biometricInfo: null,
				biometricAvailable: false,
				biometricLoading: false,
				rememberAccountChecked: false,
				rememberPasswordChecked: false,
				displayVersion: ''
			}
		},
		async onLoad() {
			const token = uni.getStorageSync('token')
			if (token && token !== 'mock_token') {
				uni.switchTab({
					url: '/pages/wallet/wallet'
				})
				return
			}
			if (token === 'mock_token') {
				uni.removeStorageSync('token')
			}
			await this.initBiometricState()
			const remembered = loadLoginRemember()
			if (remembered) {
				if (remembered.email) {
					this.email = remembered.email
				}
				if (remembered.password) {
					this.password = remembered.password
				}
				this.rememberAccountChecked = !!remembered.saveEmail
				this.rememberPasswordChecked = !!remembered.savePassword
			}
			this.loadVersionInfo()
		},
		computed: {
			...mapGetters(['i18n', 'themeClass']),
			...mapState(['language', 'theme']),
			displayLanguageLabel() {
				return this.language === '中文' ? this.i18n.languageOptionChinese : this.i18n.languageOptionEnglish
			},
			displayTheme() {
				if (this.theme === 'Auto') return this.i18n.themeAuto
				if (this.theme === 'Light') return this.i18n.themeLight
				if (this.theme === 'Dark') return this.i18n.themeDark
				return this.theme
			},
			/** 密码登录失败累计后显示图形验证码 */
			showImageCaptcha() {
				return this.passwordLoginFailCount >= 3
			},
			versionLabel() {
				return this.language === '中文' ? '版本号' : 'Version'
			},
			formattedDisplayVersion() {
				return formatDisplayVersion(this.displayVersion)
			}
		},
		methods: {
			async loadVersionInfo() {
				this.displayVersion = await fetchServerVersionName()
			},
			toggleLanguage() {
				const nextLang = this.language === '中文' ? 'English' : '中文'
				this.$store.commit('SET_LANGUAGE', nextLang)
			},
			toggleTheme() {
				const themes = ['Auto', 'Light', 'Dark']
				const currentIndex = themes.indexOf(this.theme)
				const nextIndex = currentIndex === -1 ? 1 : (currentIndex + 1) % themes.length
				this.$store.commit('SET_THEME', themes[nextIndex])
			},
			async refreshImageCaptcha() {
				try {
					const res = await api.getImageCaptcha()
					if (res && res.captchaEnabled === false) {
						return
					}
					this.captchaImage = res && res.img ? `data:image/gif;base64,${res.img}` : ''
					this.captchaUuid = res && res.uuid ? res.uuid : ''
				} catch (e) {
					this.captchaImage = ''
					this.captchaUuid = ''
				}
			},
			async getBiometricCapability(silent = true) {
				if (
					typeof uni.checkIsSupportSoterAuthentication !== 'function' ||
					typeof uni.startSoterAuthentication !== 'function'
				) {
					if (!silent) {
						uni.showToast({ title: this.i18n.biometricNotSupportedPlatform, icon: 'none' })
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
						uni.showToast({ title: this.i18n.biometricCapabilityUnavailable, icon: 'none' })
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
					uni.showToast({ title: this.i18n.enrollFingerprintInSystem, icon: 'none' })
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
							uni.showToast({ title: this.i18n.fingerprintAuthFailed, icon: 'none' })
							resolve(false)
						}
					})
				})
			},
			clearBiometricLogin() {
				uni.removeStorageSync(BIOMETRIC_STORAGE_KEY)
				this.biometricInfo = null
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
						title: this.i18n.enableFingerprintLoginTitle,
						content: this.i18n.enableFingerprintLoginMessage,
						confirmText: this.i18n.confirmBtn,
						cancelText: this.i18n.cancelBtn,
						success: resolve,
						fail: () => resolve({ confirm: false })
					})
				})
				if (!modalRes.confirm) {
					return
				}

				const passed = await this.authenticateWithFingerprint(this.i18n.verifyFingerprintToEnableQuickLogin)
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
					uni.showToast({ title: this.i18n.fingerprintLoginEnabled, icon: 'success' })
				} catch (e) {}
			},
			async loginWithFingerprint() {
				if (this.biometricLoading) {
					return
				}
				if (!this.biometricInfo || !this.biometricInfo.biometricToken) {
					uni.showToast({ title: this.i18n.pleaseEnableBiometricFirst, icon: 'none' })
					return
				}

				this.biometricLoading = true
				try {
					const passed = await this.authenticateWithFingerprint(this.i18n.verifyFingerprintToLogin)
					if (!passed) {
						return
					}

					uni.showLoading({ title: this.i18n.loggingIn })
					const res = await api.loginBiometric(this.biometricInfo.biometricToken)
					uni.hideLoading()
					if (!res || !res.access_token) {
						uni.showToast({ title: this.i18n.fingerprintLoginFailed, icon: 'none' })
						return
					}
					uni.setStorageSync('token', res.access_token)
					uni.setStorageSync('is_logged_in', true)
					uni.switchTab({
						url: '/pages/wallet/wallet'
					})
				} catch (e) {
					uni.hideLoading()
					const msg = e && e.msg ? e.msg : ''
					if (msg && (msg.includes('失效') || msg.includes('不存在') || msg.includes('停用'))) {
						this.clearBiometricLogin()
					}
				} finally {
					this.biometricLoading = false
				}
			},
			async handleLogin() {
				if (!this.email) {
					uni.showToast({ title: this.i18n.pleaseEnterEmail, icon: 'none' })
					return
				}
				if (!this.password) {
					uni.showToast({ title: this.i18n.pleaseEnterPassword, icon: 'none' })
					return
				}

				if (this.showImageCaptcha) {
					if (!this.captchaUuid) {
						await this.refreshImageCaptcha()
					}
					if (!this.captchaCode) {
						uni.showToast({ title: this.i18n.pleaseEnterImageCaptcha, icon: 'none' })
						return
					}
				}

				uni.showLoading({ title: this.i18n.loggingIn })

				try {
					const extra = {}
					if (this.showImageCaptcha && this.captchaUuid && this.captchaCode) {
						extra.uuid = this.captchaUuid
						extra.code = this.captchaCode
					}
					const res = await api.loginPassword(this.email, this.password, extra)
					uni.hideLoading()
					if (res && res.access_token) {
						uni.setStorageSync('token', res.access_token)
						this.passwordLoginFailCount = 0
						this.captchaCode = ''
						this.captchaUuid = ''
					} else {
						uni.showToast({ title: this.i18n.loginSuccessNoCredential, icon: 'none' })
						return
					}
					uni.setStorageSync('is_logged_in', true)
					if (this.rememberAccountChecked || this.rememberPasswordChecked) {
						saveLoginRemember({
							email: this.email,
							password: this.password,
							saveEmail: this.rememberAccountChecked,
							savePassword: this.rememberPasswordChecked
						})
					} else {
						clearLoginRemember()
					}
					await this.promptEnableBiometricLogin()
					uni.switchTab({
						url: '/pages/wallet/wallet'
					})
				} catch (e) {
					uni.hideLoading()
					const msg = (e && e.msg) || ''
					if (msg.includes('登录失败次数过多') || msg.includes('请输入图形验证码')) {
						this.passwordLoginFailCount = Math.max(this.passwordLoginFailCount, 3)
						this.captchaCode = ''
						await this.refreshImageCaptcha()
					} else if (msg.includes('邮箱或密码错误')) {
						this.passwordLoginFailCount += 1
						if (this.showImageCaptcha) {
							this.captchaCode = ''
							await this.refreshImageCaptcha()
						}
					} else if (
						msg.includes('图形验证码') &&
						(msg.includes('错误') || msg.includes('过期'))
					) {
						this.passwordLoginFailCount = Math.max(this.passwordLoginFailCount, 3)
						this.captchaCode = ''
						await this.refreshImageCaptcha()
					}
				}
			},
			goChangePassword() {
				const e = (this.email || '').trim()
				const q = e ? `?email=${encodeURIComponent(e)}` : ''
				uni.navigateTo({
					url: '/pages/login/forgot_password' + q
				})
			},
			goRegister() {
				const e = (this.email || '').trim()
				const q = e ? `?email=${encodeURIComponent(e)}` : ''
				uni.navigateTo({
					url: '/pages/login/register' + q
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		padding: 132rpx 40rpx 72rpx;
		box-sizing: border-box;
		position: relative;
		overflow: hidden;
	}

	.theme-light.container {
		background:
			radial-gradient(circle at 12% 8%, rgba(51, 207, 255, 0.22), transparent 42%),
			radial-gradient(circle at 92% 12%, rgba(120, 200, 255, 0.18), transparent 38%),
			radial-gradient(circle at 50% 88%, rgba(180, 230, 255, 0.16), transparent 45%),
			var(--page-bg-gradient);
	}

	.theme-dark.container {
		background:
			radial-gradient(circle at top left, rgba(51, 207, 255, 0.16), transparent 38%),
			radial-gradient(circle at top right, rgba(122, 162, 255, 0.14), transparent 34%),
			var(--page-bg-gradient);
	}

	.content-shell {
		position: relative;
		z-index: 2;
		min-height: calc(100vh - 204rpx);
		display: flex;
		flex-direction: column;
		justify-content: center;
		gap: 56rpx;
	}

	.page-version {
		display: flex;
		justify-content: center;
		padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
	}

	.page-version-text {
		font-size: 24rpx;
		color: var(--text-tertiary);
		letter-spacing: 1rpx;
	}

	.login-top-bar {
		position: absolute;
		top: calc(72rpx + env(safe-area-inset-top));
		left: 32rpx;
		right: 32rpx;
		display: flex;
		flex-direction: row;
		justify-content: space-between;
		align-items: center;
		z-index: 20;
	}

	.login-top-pill {
		max-width: 48%;
		padding: 14rpx 22rpx;
		border-radius: 999rpx;
		background-color: var(--nav-icon-bg);
		border: var(--card-border);
		box-shadow: var(--card-shadow-sm);
	}

	.login-top-pill:active {
		opacity: 0.88;
		transform: scale(0.98);
	}

	.login-top-pill-text {
		font-size: 24rpx;
		font-weight: 600;
		color: var(--text-secondary);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.particle-layer {
		position: absolute;
		inset: 0;
		overflow: hidden;
		pointer-events: none;
		z-index: 1;
	}

	.particle-item {
		position: absolute;
		border-radius: 50%;
		background: radial-gradient(circle at 30% 30%, rgba(255, 255, 255, 0.95), rgba(51, 207, 255, 0.4) 55%, rgba(51, 207, 255, 0));
		box-shadow: 0 0 24rpx rgba(51, 207, 255, 0.22);
		animation-name: particleFloat;
		animation-timing-function: ease-in-out;
		animation-iteration-count: infinite;
	}

	/* 浅色底上提高对比：芯更实、青边更浓，动态气泡才看得见 */
	.theme-light .particle-item {
		background: radial-gradient(
			circle at 32% 32%,
			rgba(255, 255, 255, 1) 0%,
			rgba(51, 207, 255, 0.55) 42%,
			rgba(51, 207, 255, 0.2) 68%,
			rgba(51, 207, 255, 0)
		);
		box-shadow: 0 0 32rpx rgba(51, 207, 255, 0.42), 0 0 8rpx rgba(255, 255, 255, 0.9);
	}

	.particle-glow {
		position: absolute;
		width: 420rpx;
		height: 420rpx;
		border-radius: 50%;
		filter: blur(48rpx);
		opacity: 0.4;
	}

	.theme-light .particle-glow {
		opacity: 0.52;
		filter: blur(52rpx);
	}

	.particle-glow-left {
		top: -100rpx;
		left: -120rpx;
		background: rgba(51, 207, 255, 0.22);
	}

	.particle-glow-right {
		right: -120rpx;
		bottom: 160rpx;
		background: rgba(122, 162, 255, 0.18);
	}

	.theme-light .particle-glow-left {
		background: rgba(51, 207, 255, 0.38);
	}

	.theme-light .particle-glow-right {
		background: rgba(100, 190, 255, 0.32);
	}

	/* 深色模式变量兼容 */
	.theme-dark {
		--btn-bg: #F5F7FF;
		--btn-text: #08101F;
		--input-bg: rgba(22, 35, 64, 0.92);
		--tab-bg: rgba(18, 28, 48, 0.92);
		--tab-active-bg: #162340;
	}
	.theme-light {
		--btn-bg: #33cfff;
		--btn-text: #FFFFFF;
		--input-bg: rgba(234, 241, 255, 0.96);
		--tab-bg: rgba(234, 241, 255, 0.96);
		--tab-active-bg: #FFFFFF;
	}

	.top-area {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 32rpx;
		text-align: center;
	}

	.logo-wrap {
		width: 160rpx;
		height: 160rpx;
		border-radius: 40rpx;
		margin-bottom: 16rpx;
		border: 4rpx solid rgba(255, 255, 255, 0.48);
		background-color: var(--card-bg);
		overflow: hidden;
		box-sizing: border-box;
	}

	.logo {
		width: 100%;
		height: 100%;
		display: block;
	}

	.theme-dark .logo-wrap .logo {
		filter: brightness(0) invert(1);
	}

	.slogan {
		font-family: 'Inter', sans-serif;
		font-size: 56rpx;
		font-weight: bold;
		color: var(--text-primary, #000000);
	}

	.sub-slogan {
		font-family: 'Inter', sans-serif;
		font-size: 32rpx;
		color: var(--text-secondary, #888888);
	}

	.bottom-area {
		display: flex;
		flex-direction: column;
		width: 100%;
		max-width: 670rpx;
		margin: 0 auto;
		gap: 24rpx;
		padding: 32rpx;
		background: var(--card-bg);
		border: var(--card-border);
		border-radius: 32rpx;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
		box-sizing: border-box;
		position: relative;
		z-index: 1;
	}

	.title-row {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
		margin-bottom: 8rpx;
	}

	.panel-title {
		font-size: 40rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.panel-desc {
		font-size: 26rpx;
		color: var(--text-secondary);
	}

	.input-group {
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.input-row {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: space-between;
		background-color: var(--input-bg);
		border-radius: 24rpx;
		padding: 24rpx 28rpx;
		border: 1px solid var(--border-color);
		transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
	}

	.input-field {
		flex: 1;
		font-size: 32rpx;
		color: var(--text-primary);
	}

	.placeholder-text {
		color: var(--text-secondary);
	}

	.icon-mail, .icon-lock {
		font-size: 32rpx;
		color: var(--text-secondary);
	}

	.options-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 20rpx;
		margin-top: 16rpx;
		padding: 0 8rpx;
	}

	.remember-row-wrap {
		display: flex;
		flex-direction: row;
		flex: 1;
		flex-wrap: nowrap;
		align-items: center;
		min-width: 0;
		gap: 8rpx 28rpx;
	}

	.remember-row {
		display: flex;
		flex-direction: row;
		align-items: center;
	}

	.remember-box {
		font-size: 36rpx;
		color: var(--accent-color);
		margin-right: 12rpx;
		line-height: 1;
	}

	.remember-text {
		font-size: 26rpx;
		color: var(--text-secondary);
	}

	.captcha-row {
		padding-right: 16rpx;
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

	.helper-link {
		font-size: 26rpx;
		color: var(--accent-color);
		flex-shrink: 0;
	}

	.action-row {
		display: flex;
		align-items: center;
		gap: 20rpx;
		margin-top: 16rpx;
	}

	.action-btn {
		flex: 1;
		height: 112rpx;
		border-radius: 24rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		padding: 0;

		&::after {
			border: none;
		}
	}

	.btn-login {
		background-color: var(--btn-bg, #000000);
		border: none;
	}

	.btn-text {
		font-family: 'Inter', sans-serif;
		font-size: 36rpx;
		font-weight: bold;
		color: var(--btn-text, #FFFFFF);
	}

	.btn-register {
		background: #ffffff;
		border: 1px solid rgba(8, 16, 31, 0.08);
		box-shadow: 0 10rpx 24rpx rgba(8, 16, 31, 0.06);
	}

	.btn-register-text {
		font-family: 'Inter', sans-serif;
		font-size: 36rpx;
		font-weight: bold;
		color: #08101F;
	}

	.btn-biometric {
		width: 100%;
		height: 96rpx;
		border-radius: 24rpx;
		background: transparent;
		border: 1px solid var(--border-color);
		display: flex;
		justify-content: center;
		align-items: center;
		padding: 0;

		&::after {
			border: none;
		}
	}

	.btn-biometric-text {
		font-size: 30rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	@keyframes particleFloat {
		0% {
			transform: translate3d(0, 0, 0) scale(0.9);
		}
		25% {
			transform: translate3d(14rpx, -22rpx, 0) scale(1);
		}
		50% {
			transform: translate3d(-10rpx, -48rpx, 0) scale(1.08);
		}
		75% {
			transform: translate3d(20rpx, -24rpx, 0) scale(0.96);
		}
		100% {
			transform: translate3d(0, 0, 0) scale(0.9);
		}
	}
</style>
