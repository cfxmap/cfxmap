<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.exportWalletMenu || 'Export Wallet' }}</text>
		</view>

		<view class="card-wrapper">
			<view class="card-inner">
				<text class="ex-title">{{ i18n.selectWalletToExport }}</text>
				<text class="ex-desc">{{ i18n.exportDesc }}</text>

				<text class="label">{{ i18n.selectWallet }}</text>
				<picker
					v-if="wallets.length > 0"
					@change="onWalletChange"
					:value="walletIndex"
					:range="wallets"
					range-key="displayTitle"
				>
					<view class="picker-box">
						<text class="picker-text">{{ currentWallet ? currentWallet.displayTitle : '' }}</text>
						<text class="picker-arrow">></text>
					</view>
				</picker>
				<text v-else class="ex-desc">{{ i18n.noWalletToExport }}</text>

				<view v-if="currentWallet && currentWallet.isNewUserCreated" class="badge-row">
					<text class="wallet-badge">{{ i18n.systemProvisionedWallet }}</text>
					<text class="badge-hint">{{ i18n.systemProvisionedWalletHint }}</text>
				</view>

				<text v-if="currentWallet" class="hint">{{ i18n.securityWarningSecret }}</text>
			</view>
		</view>

		<view
			v-if="currentWallet && (currentWallet.canExportPrivateKey || currentWallet.canExportMnemonic)"
			class="export-btns-row"
		>
			<view v-if="currentWallet.canExportPrivateKey" class="export-btn-cell">
				<button
					class="btn-primary btn-in-row"
					@click="handleExport('privateKey')"
					:disabled="loading || !currentWallet"
				>
					<view class="btn-content">{{ loading ? i18n.loading : i18n.exportPrivateKeyBtn }}</view>
				</button>
			</view>
			<view v-if="currentWallet.canExportMnemonic" class="export-btn-cell">
				<button
					class="btn-secondary btn-in-row"
					@click="handleExport('mnemonic')"
					:disabled="loading || !currentWallet"
				>
					<view class="btn-content">{{ loading ? i18n.loading : i18n.exportMnemonicBtn }}</view>
				</button>
			</view>
		</view>
		<text
			v-if="currentWallet && !currentWallet.canExportPrivateKey && !currentWallet.canExportMnemonic"
			class="no-export-tip"
		>
			{{ i18n.exportNotAvailableForWallet }}
		</text>

		<view class="export-result-wrapper" v-if="exportResult">
			<view class="export-result-inner">
				<text class="ex-title">{{ i18n.exportResultTitle }}</text>
				<view class="result-item" v-if="exportResult.address">
					<text class="result-label">{{ i18n.walletAddressLabel }}</text>
					<text class="result-value">{{ exportResult.address }}</text>
				</view>
				<view class="result-item" v-if="exportType === 'privateKey' && exportResult.privateKey">
					<text class="result-label">{{ i18n.privateKeyLabel }}</text>
					<text class="result-value">{{ exportResult.privateKey }}</text>
				</view>
				<view class="result-item" v-if="exportType === 'mnemonic' && exportResult.mnemonic">
					<text class="result-label">{{ i18n.mnemonicLabel }}</text>
					<text class="result-value">{{ exportResult.mnemonic }}</text>
				</view>
				<button class="btn-secondary" @click="exportResult = null">
					<view class="btn-content">{{ i18n.close }}</view>
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				loading: false,
				wallets: [],
				walletIndex: 0,
				exportType: 'privateKey',
				exportResult: null
			}
		},
		computed: {
			...mapGetters(['i18n']),
			currentWallet() {
				if (!this.wallets || this.wallets.length === 0) return null
				if (this.walletIndex < 0 || this.walletIndex >= this.wallets.length) return this.wallets[0]
				return this.wallets[this.walletIndex]
			}
		},
		onShow() {
			this.fetchWallets()
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			onWalletChange(e) {
				this.walletIndex = Number(e.detail.value || 0)
			},
			normalizeWalletRow(w) {
				const addr = w.address || ''
				const shortAddr =
					addr.length > 16 ? addr.substring(0, 8) + '...' + addr.substring(addr.length - 6) : addr
				const nameProvisioned = typeof w.name === 'string' && w.name.indexOf('主钱包_') === 0
				const systemNew =
					w.isNewUserCreated === true ||
					w.isNewUserCreated === 1 ||
					nameProvisioned
				return {
					...w,
					isNewUserCreated: systemNew,
					canExportPrivateKey: !!(w.canExportPrivateKey ?? (w.hasPrivateKey || systemNew)),
					canExportMnemonic: !!(w.canExportMnemonic ?? (w.hasMnemonic || systemNew)),
					displayTitle: w.name ? `${w.name} (${shortAddr})` : shortAddr
				}
			},
			async fetchWallets() {
				try {
					const res = await api.getExportWalletList2()
					const list = Array.isArray(res) ? res : []
					this.wallets = list.map(w => this.normalizeWalletRow(w))
					const currentAddr = uni.getStorageSync('current_wallet_address')
					if (currentAddr) {
						const idx = this.wallets.findIndex(w => w.address === currentAddr)
						if (idx >= 0) this.walletIndex = idx
					}
					if (this.walletIndex >= this.wallets.length) {
						this.walletIndex = 0
					}
				} catch (e) {
					console.error('Failed to fetch wallets:', e)
					this.wallets = []
					this.walletIndex = 0
				}
			},
			async handleExport(type) {
				if (!this.currentWallet) {
					uni.showToast({ title: this.i18n.selectWalletFirst, icon: 'none' })
					return
				}
				if (this.loading) return
				this.loading = true
				this.exportType = type
				try {
					// 导出前把当前选择的钱包写入本地，兼容后端“导出当前钱包”的实现
					if (this.currentWallet.address) {
						uni.setStorageSync('current_wallet_address', this.currentWallet.address)
					}
					const res = await api.exportWallet({
						accountId: this.currentWallet.accountId,
						address: this.currentWallet.address,
						exportType: type
					})
					if (res && (res.privateKey || res.mnemonic)) {
						this.exportResult = {
							address: res.address || this.currentWallet.address || '',
							privateKey: res.privateKey || '',
							mnemonic: res.mnemonic || ''
						}
						uni.showToast({ title: this.i18n.exportSuccessful, icon: 'success' })
					} else {
						uni.showToast({ title: this.i18n.noWalletToExport, icon: 'none' })
					}
				} catch (e) {
					console.error('Export failed', e)
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

	.card-wrapper {
		background-color: var(--card-bg);
		margin-bottom: 32rpx;
		border-radius: 16rpx;
	}

	.card-inner {
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}

	.label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.ex-title {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.ex-desc {
		font-size: 26rpx;
		color: var(--text-secondary);
		line-height: 1.45;
	}

	.no-export-tip {
		display: block;
		font-size: 26rpx;
		color: var(--text-secondary);
		line-height: 1.5;
		margin-bottom: 32rpx;
	}

	.picker-box {
		background-color: var(--control-bg);
		border-radius: 16rpx;
		padding: 24rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.picker-text {
		font-size: 28rpx;
		color: var(--text-primary);
	}

	.picker-arrow {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.badge-row {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}

	.wallet-badge {
		align-self: flex-start;
		font-size: 22rpx;
		font-weight: 600;
		color: var(--btn-confirm-bg);
		background-color: rgba(88, 101, 242, 0.12);
		padding: 8rpx 16rpx;
		border-radius: 12rpx;
	}

	.badge-hint {
		font-size: 24rpx;
		color: var(--text-secondary);
		line-height: 1.45;
	}

	.hint {
		font-size: 24rpx;
		color: var(--warning-text);
		background-color: var(--warning-bg);
		padding: 16rpx;
		border-radius: 16rpx;
		line-height: 1.5;
	}

	.export-btns-row {
		display: flex;
		flex-direction: row;
		gap: 20rpx;
		align-items: stretch;
		margin-top: 8rpx;
		margin-bottom: 8rpx;
	}

	.export-btn-cell {
		flex: 1;
		min-width: 0;
	}

	.btn-primary {
		width: 100%;
		height: 112rpx;
		background-color: var(--btn-confirm-bg);
		border-radius: 16rpx;
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 24rpx;
		&::after { border: none; }
	}

	.btn-primary[disabled] {
		opacity: 0.6;
	}

	.btn-secondary {
		width: 100%;
		height: 112rpx;
		background-color: var(--card-bg);
		border-radius: 16rpx;
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 32rpx;
		&::after { border: none; }
	}

	.btn-primary.btn-in-row,
	.btn-secondary.btn-in-row {
		width: 100%;
		margin-bottom: 0;
	}

	.btn-content {
		color: var(--btn-text-white);
		font-size: 32rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.btn-secondary .btn-content {
		color: var(--text-primary);
	}

	.export-result-wrapper {
		background-color: var(--card-bg);
		margin-bottom: 32rpx;
		border-radius: 16rpx;
	}

	.export-result-inner {
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 16rpx;
		word-break: break-all;
	}

	.ex-title {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.result-item {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}

	.result-label {
		font-size: 24rpx;
		color: var(--text-secondary);
		font-weight: bold;
	}

	.result-value {
		font-size: 28rpx;
		color: var(--text-primary);
		font-family: monospace;
	}
</style>

