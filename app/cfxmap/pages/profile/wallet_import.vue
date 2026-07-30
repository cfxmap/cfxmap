<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.importWalletMenu }}</text>
		</view>

		<view class="tab-group-wrapper">
			<view class="tab-group-inner">
				<view class="tab-item" :class="{ active: currentTab === 'privateKey' }" @click="currentTab = 'privateKey'">
					<text class="tab-text">{{ i18n.privateKey }}</text>
				</view>
				<view class="tab-item" :class="{ active: currentTab === 'mnemonic' }" @click="currentTab = 'mnemonic'">
					<text class="tab-text">{{ i18n.mnemonic }}</text>
				</view>
			</view>
		</view>

		<view class="card-wrapper input-card">
			<view class="card-inner">
				<text class="label">{{ i18n.selectNetwork }}</text>
				<picker @change="onNetworkChange" :value="networkIndex" :range="networks" range-key="name">
					<view class="network-selector-wrapper">
						<view class="network-selector-inner">
							<text class="network-text">{{ networks[networkIndex].name }}</text>
							<text class="network-arrow">></text>
						</view>
					</view>
				</picker>

				<text class="label">{{ i18n.walletName }}</text>
				<view class="input-box-wrapper">
					<view class="input-box-inner">
						<input class="input" v-model="walletName" maxlength="20" :placeholder="i18n.walletNamePlaceholder" />
					</view>
				</view>

				<text class="label" v-if="currentTab === 'privateKey'">{{ i18n.enterPrivateKey }}</text>
				<text class="label" v-else>{{ i18n.enterMnemonic }}</text>

				<view class="input-box-wrapper">
					<view class="input-box-inner">
						<textarea
							class="textarea"
							v-model="importData"
							:placeholder="currentTab === 'privateKey'
								? i18n.privateKeyPlaceholder
								: i18n.mnemonicPlaceholder"
						/>
					</view>
				</view>

				<button class="btn-primary" @click="handleImport" :disabled="loading">
					<view class="btn-content">{{ loading ? i18n.loading : i18n.importWalletBtn }}</view>
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { clearWalletCache } from '@/utils/cache'
	import { markWalletAssetsChanged } from '@/utils/wallet-refresh'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				currentTab: 'privateKey',
				walletName: '',
				importData: '',
				loading: false,
				// 仅支持 Conflux
				networks: [
					{ id: 'cfx', name: 'Conflux Core (CFX)' },
					{ id: 'cfxtest', name: 'Conflux Core Testnet' }
				],
				networkIndex: 0
			}
		},
		computed: {
			...mapGetters(['i18n']),
			selectedNetwork() {
				return this.networks[this.networkIndex] || this.networks[0]
			}
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			onNetworkChange(e) {
				this.networkIndex = Number(e.detail.value || 0)
			},
			async handleImport() {
				if (!this.walletName || !this.walletName.trim()) {
					uni.showToast({ title: this.i18n.pleaseEnterWalletName, icon: 'none' })
					return
				}
				if (!this.importData) {
					uni.showToast({ title: this.i18n.pleaseEnterData, icon: 'none' })
					return
				}
				if (this.walletName.trim().length > 20) {
					uni.showToast({ title: this.i18n.walletNameMaxLen || 'Wallet name max 20 characters', icon: 'none' })
					return
				}
				if (this.loading) return
				this.loading = true
				try {
					const accountData = {
						chainId: this.selectedNetwork.id,
						name: this.walletName.trim()
					}
					if (this.currentTab === 'privateKey') {
						accountData.privateKey = this.importData.trim()
						if (accountData.privateKey && !accountData.privateKey.startsWith('0x')) {
							accountData.privateKey = '0x' + accountData.privateKey
						}
					} else {
						accountData.mnemonic = this.importData.trim()
					}

					const res = await api.importWallet(accountData)
					uni.showToast({ title: this.i18n.importSuccessful, icon: 'success' })
					this.importData = ''

					// 设置当前钱包地址（Conflux Base32 优先）
					if (res) {
						let primaryAddress = res.cfxAddress
						if (this.selectedNetwork.id === 'cfxtest') primaryAddress = res.cfxTestAddress
						if (primaryAddress) uni.setStorageSync('current_wallet_address', primaryAddress)
						
						// 清空代币和NFT缓存
						clearWalletCache()
						markWalletAssetsChanged()
					}
				} catch (err) {
					console.error('Import failed', err)
					// 业务/网络错误已在 utils/request 中展示接口 msg 或通用提示，此处勿再 toast 覆盖
				} finally {
					this.loading = false
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
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
		gap: 32rpx;
	}

	.back-btn {
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

	.tab-group-wrapper {
		background-color: var(--control-bg);
		margin-bottom: 48rpx;
		border-radius: 16rpx;
		overflow: hidden;
	}

	.tab-group-inner {
		display: flex;
		padding: 4rpx;
	}

	.tab-item {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 16rpx 0;
		border-radius: 16rpx;
		&.active {
			background-color: var(--card-bg);
		}
	}

	.tab-text {
		font-size: 28rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.tab-item:not(.active) .tab-text {
		color: var(--text-secondary);
		font-weight: normal;
	}

	.card-wrapper {
		background-color: var(--card-bg);
		margin-bottom: 32rpx;
		border-radius: 16rpx;
	}

	.card-inner {
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.network-selector-wrapper {
		background-color: var(--control-bg);
		border-radius: 16rpx;
	}

	.network-selector-inner {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 24rpx;
	}

	.network-text {
		font-size: 28rpx;
		color: var(--text-primary);
	}

	.network-arrow {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.input-box-wrapper {
		background-color: var(--control-bg);
		border-radius: 16rpx;
	}

	.input-box-inner {
		padding: 24rpx;
	}

	.textarea {
		width: 100%;
		height: 200rpx;
		font-size: 28rpx;
		color: var(--text-primary);
		background-color: transparent;
	}

	.input {
		width: 100%;
		font-size: 28rpx;
		color: var(--text-primary);
		background-color: transparent;
	}

	.btn-primary {
		background-color: var(--text-primary);
		margin-top: 16rpx;
		border: none;
		border-radius: 16rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100rpx;
		&::after { border: none; }
	}

	.btn-primary[disabled] {
		opacity: 0.6;
	}

	.btn-primary .btn-content {
		color: var(--bg-color);
		font-size: 32rpx;
		font-weight: bold;
	}
</style>
