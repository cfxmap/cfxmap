<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="page-shell">
			<view class="top-bar">
				<view class="top-left" @click="goBack">
					<nav-back-btn />
				</view>
				<view class="top-actions">
					<view class="login-top-pill" @click="toggleLanguage">
						<text class="login-top-pill-text">{{ displayLanguageLabel }}</text>
					</view>
					<view class="login-top-pill" @click="toggleTheme">
						<text class="login-top-pill-text">{{ displayTheme }}</text>
					</view>
				</view>
			</view>

			<view class="content-shell">
				<view class="title-block">
					<text class="title">{{ i18n.registerTitle }}</text>
					<text class="desc">{{ i18n.registerDesc }}</text>
				</view>

				<view class="card">
					<view class="input-row">
						<input
							class="input-field"
							type="text"
							:placeholder="i18n.emailAddress"
							v-model="email"
							placeholder-class="placeholder-text"
						/>
						<text class="icon-text">✉️</text>
					</view>

					<view class="input-row">
						<input
							class="input-field"
							type="password"
							:placeholder="i18n.password"
							v-model="password"
							placeholder-class="placeholder-text"
						/>
						<text class="icon-text">🔒</text>
					</view>

					<view class="input-row">
						<input
							class="input-field"
							type="password"
							:placeholder="i18n.confirmPasswordLabel"
							v-model="confirmPassword"
							placeholder-class="placeholder-text"
						/>
						<text class="icon-text">🔒</text>
					</view>

					<button class="btn-primary" @click="handleRegister">
						<text class="btn-text">{{ i18n.registerBtn }}</text>
					</button>

					<view class="helper-row">
						<text class="helper-muted">{{ i18n.haveAccountAlready }}</text>
						<text class="helper-link" @click="goLogin">{{ i18n.loginBtn }}</text>
					</view>
				</view>

				<view v-if="formattedDisplayVersion" class="page-version">
					<text class="page-version-text">{{ versionLabel }} {{ formattedDisplayVersion }}</text>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters, mapState } from 'vuex'
	import { api } from '@/utils/request'
	import { fetchServerVersionName } from '@/utils/app-version-api'
	import { formatDisplayVersion } from '@/config/app-version.js'
	import { markCustodyNoticePending } from '@/utils/custody-notice'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	const EMAIL_RE = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

	export default {
		components: {
			NavBackBtn
		},
		data() {
			return {
				email: '',
				password: '',
				confirmPassword: '',
				displayVersion: ''
			}
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
			versionLabel() {
				return this.language === '中文' ? '版本号' : 'Version'
			},
			formattedDisplayVersion() {
				return formatDisplayVersion(this.displayVersion)
			}
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
			this.loadVersionInfo()
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
			goBack() {
				uni.navigateBack({
					fail: () => {
						this.goLogin()
					}
				})
			},
			goLogin() {
				uni.redirectTo({
					url: '/pages/login/login'
				})
			},
			async handleRegister() {
				const email = (this.email || '').trim()
				if (!email) {
					uni.showToast({ title: this.i18n.pleaseEnterEmail, icon: 'none' })
					return
				}
				if (!EMAIL_RE.test(email)) {
					uni.showToast({ title: this.i18n.emailFormatInvalid, icon: 'none' })
					return
				}
				if (!this.password) {
					uni.showToast({ title: this.i18n.pleaseEnterPassword, icon: 'none' })
					return
				}
				if (this.password.length < 6) {
					uni.showToast({ title: this.i18n.passwordAtLeast6Chars, icon: 'none' })
					return
				}
				if (!this.confirmPassword) {
					uni.showToast({ title: this.i18n.pleaseConfirmPassword, icon: 'none' })
					return
				}
				if (this.password !== this.confirmPassword) {
					uni.showToast({ title: this.i18n.passwordMismatch, icon: 'none' })
					return
				}

				try {
					uni.showLoading({ title: this.i18n.registering })
					const res = await api.registerPassword(email, this.password)
					uni.hideLoading()
					if (!res || !res.access_token) {
						uni.showToast({ title: this.i18n.toastRequestFailed, icon: 'none' })
						return
					}
					uni.setStorageSync('token', res.access_token)
					uni.setStorageSync('is_logged_in', true)
					markCustodyNoticePending()
					uni.showToast({ title: this.i18n.registerSuccess, icon: 'success' })
					setTimeout(() => {
						uni.switchTab({
							url: '/pages/wallet/wallet'
						})
					}, 300)
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
		padding: 72rpx 32rpx 64rpx;
		box-sizing: border-box;
		background:
			radial-gradient(circle at top left, rgba(51, 207, 255, 0.16), transparent 38%),
			radial-gradient(circle at top right, rgba(122, 162, 255, 0.14), transparent 34%),
			var(--page-bg-gradient);
	}

	.page-shell {
		min-height: calc(100vh - 136rpx);
		display: flex;
		flex-direction: column;
	}

	.top-bar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 48rpx;
		padding-top: calc(12rpx + env(safe-area-inset-top));
	}

	.top-left {
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.top-actions {
		display: flex;
		align-items: center;
		gap: 16rpx;
	}

	.login-top-pill {
		padding: 14rpx 22rpx;
		border-radius: 999rpx;
		background-color: var(--nav-icon-bg);
		border: var(--card-border);
		box-shadow: var(--card-shadow-sm);
	}

	.login-top-pill-text {
		font-size: 24rpx;
		font-weight: 600;
		color: var(--text-secondary);
	}

	.content-shell {
		flex: 1;
		display: flex;
		flex-direction: column;
		justify-content: center;
		gap: 36rpx;
	}

	.title-block {
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}

	.title {
		font-size: 56rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.desc {
		font-size: 28rpx;
		line-height: 1.7;
		color: var(--text-secondary);
	}

	.card {
		background: var(--card-bg);
		border: var(--card-border);
		border-radius: 32rpx;
		padding: 32rpx;
		box-shadow: var(--card-shadow);
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.input-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		background-color: var(--input-bg);
		border-radius: 24rpx;
		padding: 24rpx 28rpx;
		border: 1px solid var(--border-color);
	}

	.input-field {
		flex: 1;
		font-size: 32rpx;
		color: var(--text-primary);
	}

	.placeholder-text {
		color: var(--text-secondary);
	}

	.icon-text {
		font-size: 32rpx;
		color: var(--text-secondary);
	}

	.btn-primary {
		width: 100%;
		height: 112rpx;
		background-color: var(--btn-bg, #000000);
		border-radius: 24rpx;
		display: flex;
		justify-content: center;
		align-items: center;
		border: none;
		padding: 0;
		margin-top: 8rpx;

		&::after {
			border: none;
		}
	}

	.btn-text {
		font-size: 36rpx;
		font-weight: 700;
		color: var(--btn-text, #ffffff);
	}

	.helper-row {
		display: flex;
		justify-content: center;
		align-items: center;
		gap: 12rpx;
		flex-wrap: wrap;
	}

	.helper-muted {
		font-size: 26rpx;
		color: var(--text-secondary);
	}

	.helper-link {
		font-size: 26rpx;
		font-weight: 600;
		color: var(--accent-color);
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

	.theme-dark {
		--btn-bg: #f5f7ff;
		--btn-text: #08101f;
		--input-bg: rgba(22, 35, 64, 0.92);
	}

	.theme-light {
		--btn-bg: #33cfff;
		--btn-text: #ffffff;
		--input-bg: rgba(234, 241, 255, 0.96);
	}
</style>
