<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<text class="title">{{ i18n.profileTitle }}</text>
		</view>

		<view class="user-card-wrapper">
			<view class="user-card-inner">
				<image
					class="avatar"
					:src="profileAvatarDisplay"
					:key="profileAvatarImageKey"
					mode="aspectFill"
				></image>
				<view class="user-info">
					<text class="nickname">{{ userInfo.nickname }}</text>
					<text class="uid">UID: {{ userInfo.uid }}</text>
				</view>
			</view>
		</view>

		<view class="section-title">{{ i18n.walletTitle }}</view>
		<view class="list-group-wrapper">
			<view class="list-group-inner">
				<view class="list-item" @click="goWalletManage">
					<text class="item-title">{{ i18n.manageWallet }}</text>
					<text class="item-arrow">></text>
				</view>
				<view class="list-item" @click="goWalletRemove">
					<text class="item-title">{{ i18n.removeWallet }}</text>
					<text class="item-arrow">></text>
				</view>
			</view>
		</view>

		<view class="section-title">{{ i18n.accountSettings }}</view>
		<view class="list-group-wrapper">
			<view class="list-group-inner">
				<view class="list-item" @click="goSecurityCenter">
					<text class="item-title">{{ i18n.securityTitle }}</text>
					<text class="item-arrow">></text>
				</view>
				<view class="list-item" @click="goCustodyWalletNotice">
					<text class="item-title">{{ i18n.custodyWalletFaqTitle }}</text>
					<text class="item-arrow">></text>
				</view>
			</view>
		</view>

		<view class="section-title">{{ i18n.appSettings }}</view>
		<view class="list-group-wrapper">
			<view class="list-group-inner">
				
				<view class="list-item" @click="showLanguageActionSheet">
					<text class="item-title">{{ i18n.language }}</text>
					<text class="item-value">{{ currentLanguage }} ></text>
				</view>
				<view class="list-item" @click="showCurrencyActionSheet">
					<text class="item-title">{{ i18n.currency }}</text>
					<text class="item-value">{{ displayCurrency }} ></text>
				</view>
				<view class="list-item" @click="showThemeActionSheet">
					<text class="item-title">{{ i18n.theme }}</text>
					<text class="item-value">{{ displayTheme }} ></text>
				</view>
			</view>
		</view>

		<!-- Action Area -->
		<view class="action-area">
			<button class="btn-logout" @click="handleLogout">
				<view class="btn-content">{{ i18n.logout }}</view>
			</button>
		</view>
		<view v-if="formattedDisplayVersion" class="page-version">
			<text class="page-version-text">{{ versionLabel }} {{ formattedDisplayVersion }}</text>
		</view>
		<bottom-nav current="profile" />
		<!-- #ifdef H5 -->
		<h5-add-to-home-tip bottomNav />
		<!-- #endif -->
	</view>
</template>

<script>
	import { mapState, mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { fetchServerVersionName } from '@/utils/app-version-api'
	import { getClientVersionName, formatDisplayVersion } from '@/config/app-version.js'
	import { clearAvatarCache, ensureAvatarCached } from '@/utils/avatar-cache.js'
	import { getStaticUrl } from '@/utils/static-resource.js'
	import BottomNav from '@/components/bottom-nav.vue'
	// #ifdef H5
	import H5AddToHomeTip from '@/components/h5-add-to-home-tip.vue'
	// #endif

	export default {
		components: {
			BottomNav
			// #ifdef H5
			,
			H5AddToHomeTip
			// #endif
		},
		data() {
			return {
				userInfo: {
					nickname: 'Web3 User',
					uid: '88888888',
					avatar: getStaticUrl('/static/avatar.png')
				},
				displayVersion: ''
			}
		},
		computed: {
			...mapState(['language', 'currency', 'theme', 'profileAvatarForcedSrc']),
			...mapGetters(['i18n', 'themeClass']),
			profileAvatarDisplay() {
				return this.profileAvatarForcedSrc || this.userInfo.avatar
			},
			profileAvatarImageKey() {
				return `${this.profileAvatarForcedSrc || ''}|${this.userInfo.avatar || ''}`
			},
			currentLanguage() {
				return this.language
			},
			displayCurrency() {
				return this.currency === 'USD' ? this.i18n.currencyUSD : this.i18n.currencyCNY
			},
			displayTheme() {
				if (this.theme === 'Auto') return this.i18n.themeAuto
				if (this.theme === 'Light') return this.i18n.themeLight
				if (this.theme === 'Dark') return this.i18n.themeDark
				return this.theme
			},
			versionLabel() {
				return this.language === '中文' ? '当前版本' : 'Current Version'
			},
			formattedDisplayVersion() {
				return formatDisplayVersion(this.displayVersion)
			}
		},
		onShow() {
			uni.hideTabBar()
			this.fetchUserProfile();
			this.loadVersionInfo()
		},
		methods: {
			async loadVersionInfo() {
				const clientVersion = getClientVersionName()
				this.displayVersion = clientVersion || await fetchServerVersionName()
			},
			async fetchUserProfile() {
				try {
					const res = await api.getUserProfile();
					if (res && res.user) {
						this.userInfo.nickname = res.user.nickName || 'Web3 User';
						this.userInfo.uid = res.user.userId || '88888888';
						const remote = res.user.avatar || getStaticUrl('/static/avatar.png')
						this.userInfo.avatar = await ensureAvatarCached(remote)
						this.$store.commit('CLEAR_PROFILE_AVATAR_FORCED')
					}
				} catch (err) {
					console.error('Failed to fetch profile:', err);
				}
			},
			async syncSettings() {
				try {
					await api.updateSettings({
						language: this.language,
						fiatCurrency: this.currency,
						theme: this.theme
					});
				} catch (err) {
					console.error('Failed to sync settings:', err);
				}
			},
			goSecurityCenter() {
				uni.navigateTo({
					url: '/pages/profile/security_center'
				})
			},
			goCustodyWalletNotice() {
				uni.navigateTo({
					url: '/pages/profile/custody_wallet_notice'
				})
			},
			goWalletManage() {
				uni.navigateTo({
					url: '/pages/profile/wallet_manage'
				})
			},
			goWalletRemove() {
				uni.navigateTo({
					url: '/pages/profile/wallet_remove'
				})
			},
			goApiSettings() {
				uni.navigateTo({
					url: '/pages/profile/api_settings'
				})
			},
			showLanguageActionSheet() {
				const languages = ['English', '中文'];
				uni.showActionSheet({
					itemList: [this.i18n.languageOptionEnglish, this.i18n.languageOptionChinese],
					success: (res) => {
						this.$store.commit('SET_LANGUAGE', languages[res.tapIndex]);
						this.syncSettings();
					}
				});
			},
			showCurrencyActionSheet() {
				const currencies = ['USD', 'CNY'];
				const itemList = [this.i18n.currencyUSD, this.i18n.currencyCNY];
				uni.showActionSheet({
					itemList: itemList,
					success: (res) => {
						this.$store.commit('SET_CURRENCY', currencies[res.tapIndex]);
						this.syncSettings();
					}
				});
			},
			showThemeActionSheet() {
				const themes = ['Auto', 'Light', 'Dark'];
				const itemList = [this.i18n.themeAuto, this.i18n.themeLight, this.i18n.themeDark];
				uni.showActionSheet({
					itemList: itemList,
					success: (res) => {
						this.$store.commit('SET_THEME', themes[res.tapIndex]);
						this.syncSettings();
					}
				});
			},
			handleLogout() {
				uni.showModal({
					title: this.i18n.logoutTitle,
					content: this.i18n.logoutConfirm,
					confirmText: this.i18n.confirmBtn,
					cancelText: this.i18n.cancelBtn,
					success: (res) => {
						if (res.confirm) {
							uni.removeStorageSync('token');
							clearAvatarCache()
							this.$store.commit('CLEAR_PROFILE_AVATAR_FORCED')
							uni.reLaunch({
								url: '/pages/login/login'
							})
						}
					}
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		padding: 0 32rpx 220rpx;
		box-sizing: border-box;
	}

	.header {
		padding-top: 100rpx;
		padding-bottom: 32rpx;
	}

	.title {
		font-size: 48rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.user-card-wrapper {
		background-color: var(--card-bg);
		margin-bottom: 48rpx;
		border-radius: 28rpx;
		overflow: hidden;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.user-card-inner {
		display: flex;
		align-items: center;
		padding: 40rpx;
	}

	.avatar {
		width: 120rpx;
		height: 120rpx;
		background-color: var(--control-bg);
		margin-right: 32rpx;
		border-radius: 50%;
	}

	.user-info {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}

	.nickname {
		font-size: 36rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.uid {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.section-title {
		font-size: 28rpx;
		color: var(--text-secondary);
		margin-left: 16rpx;
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

	.item-title {
		font-size: 32rpx;
		color: var(--text-primary);
	}

	.item-arrow {
		font-size: 32rpx;
		color: var(--text-tertiary);
	}

	.item-value {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.action-area {
		margin-top: 48rpx;
		padding-bottom: 48rpx;
	}

	.page-version {
		display: flex;
		justify-content: center;
		padding-bottom: 88rpx;
	}

	.page-version-text {
		font-size: 24rpx;
		color: var(--text-tertiary);
		letter-spacing: 1rpx;
	}

	.btn-logout {
		width: 100%;
		height: 112rpx;
		background-color: var(--card-bg);
		border-radius: 24rpx;
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
		&::after { border: none; }
	}

	.btn-logout .btn-content {
		color: var(--warning-text);
		font-size: 32rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}
</style>
