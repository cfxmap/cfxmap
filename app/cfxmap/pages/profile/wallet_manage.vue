<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.manageWallet }}</text>
		</view>

		<view class="tab-group-wrapper">
			<view class="tab-group-inner">
				<view class="tab-item" :class="{ active: currentTab === 'create' }" @click="currentTab = 'create'">
					<text class="tab-text">{{ i18n.createWallet }}</text>
				</view>
				<view class="tab-item" :class="{ active: currentTab === 'import' }" @click="currentTab = 'import'">
					<text class="tab-text">{{ i18n.importWallet }}</text>
				</view>
				<view class="tab-item" :class="{ active: currentTab === 'export' }" @click="currentTab = 'export'">
					<text class="tab-text">{{ i18n.exportWallet }}</text>
				</view>
			</view>
		</view>

		<view class="notice-card">
			<view class="notice-card__body">
				<text class="notice-card__badge">{{ i18n.custodyWalletBadge }}</text>
				<text class="notice-card__title">{{ i18n.custodyWalletTitle }}</text>
				<text class="notice-card__desc">{{ i18n.walletManageCustodyHint }}</text>
			</view>
			<button class="notice-card__btn" @click="goCustodyWalletNotice">
				<view class="notice-card__btn-text">{{ i18n.custodyWalletViewNoticeBtn }}</view>
			</button>
		</view>

		<!-- Create Wallet Section -->
		<view class="card-wrapper input-card" v-if="currentTab === 'create'">
			<view class="card-inner">
				<text class="label">{{ i18n.selectNetwork }}</text>
				<view class="network-selector-wrapper" @click="showNetworkPicker">
					<view class="network-selector-inner">
						<text class="network-text">{{ selectedNetwork.name }}</text>
						<text class="network-arrow">></text>
					</view>
				</view>

				<text class="label">{{ i18n.walletName }}</text>
				<view class="input-box-wrapper">
					<view class="input-box-inner">
						<input class="input" v-model="walletName" maxlength="20" :placeholder="i18n.walletNamePlaceholder" />
					</view>
				</view>

				<button class="btn-import" @click="handleCreate">
					<view class="btn-content">{{ i18n.createWalletBtn }}</view>
				</button>
			</view>
		</view>

		<!-- Import Wallet Section -->
		<view class="card-wrapper input-card" v-if="currentTab === 'import'">
			<view class="card-inner">
				<view class="sub-tab-group">
					<view class="sub-tab-item" :class="{ active: importType === 'privateKey' }" @click="importType = 'privateKey'">
						<text class="sub-tab-text">{{ i18n.privateKey }}</text>
					</view>
					<view class="sub-tab-item" :class="{ active: importType === 'mnemonic' }" @click="importType = 'mnemonic'">
						<text class="sub-tab-text">{{ i18n.mnemonic }}</text>
					</view>
				</view>

				<text class="ex-desc">{{ i18n.restoreWalletDesc }}</text>

				<text class="label">{{ i18n.selectNetwork }}</text>
				<view class="network-selector-wrapper" @click="showNetworkPicker">
					<view class="network-selector-inner">
						<text class="network-text">{{ selectedNetwork.name }}</text>
						<text class="network-arrow">></text>
					</view>
				</view>

				<text class="label">{{ i18n.walletName }}</text>
				<view class="input-box-wrapper">
					<view class="input-box-inner">
						<input class="input" v-model="walletName" maxlength="20" :placeholder="i18n.walletNamePlaceholder" />
					</view>
				</view>

				<text class="label" v-if="importType === 'privateKey'">{{ i18n.enterPrivateKey }}</text>
				<text class="label" v-else>{{ i18n.enterMnemonic }}</text>

				<view class="input-box-wrapper">
					<view class="input-box-inner">
						<textarea
							class="textarea"
							v-model="importData"
							:placeholder="importType === 'privateKey'
								? i18n.privateKeyPlaceholder
								: i18n.mnemonicPlaceholder"
						/>
					</view>
				</view>

				<text class="ex-desc">
					{{ importType === 'mnemonic' ? i18n.restoreMnemonicDesc : i18n.restorePrivateKeyDesc }}
				</text>

				<button
					v-if="importType === 'mnemonic'"
					class="btn-export"
					:disabled="previewLoading || importLoading"
					@click="handlePreviewMnemonic"
				>
					<view class="btn-content">{{ previewLoading ? i18n.previewingAddresses : i18n.previewAddressesBtn }}</view>
				</button>

				<view v-if="importType === 'mnemonic' && mnemonicPreviewList.length" class="preview-list">
					<text class="ex-title">{{ i18n.selectDerivedAddress }}</text>
					<view
						v-for="(item, index) in mnemonicPreviewList"
						:key="item.derivePath || index"
						class="preview-item"
						:class="{ active: index === selectedPreviewIndex, disabled: item.alreadyImported }"
						@click="selectPreviewItem(index)"
					>
						<view class="preview-item-head">
							<text class="result-label">{{ item.derivePath || '' }}</text>
							<text v-if="item.alreadyImported" class="preview-badge">{{ i18n.addressAlreadyImported }}</text>
						</view>
						<text class="result-value">{{ item.selectedAddress }}</text>
					</view>
				</view>

				<button class="btn-import" :disabled="importLoading" @click="handleImport">
					<view class="btn-content">{{ importLoading ? i18n.importing : i18n.importWalletBtn }}</view>
				</button>
			</view>
		</view>

		<!-- Export Wallet Section -->
		<view class="card-wrapper export-card" v-if="currentTab === 'export'">
			<view class="card-inner">
				<text class="ex-title">{{ i18n.selectWalletToExport }}</text>
				<text class="ex-desc">{{ i18n.exportDesc }}</text>

				<text class="label">{{ i18n.selectWallet }}</text>
				<picker
					v-if="wallets.length > 0"
					@change="onExportWalletChange"
					:value="walletIndex"
					:range="wallets"
					range-key="displayTitle"
				>
					<view class="network-selector-wrapper">
						<view class="network-selector-inner">
							<text class="network-text">{{ currentExportWallet ? currentExportWallet.displayTitle : '' }}</text>
							<text class="network-arrow">></text>
						</view>
					</view>
				</picker>
				<text class="ex-desc" v-else>{{ i18n.noWalletToExport }}</text>

				<view v-if="currentExportWallet && currentExportWallet.isNewUserCreated" class="badge-row">
					<text class="wallet-badge">{{ i18n.systemProvisionedWallet }}</text>
					<text class="badge-hint">{{ i18n.systemProvisionedWalletHint }}</text>
				</view>

				<text v-if="currentExportWallet" class="export-security-hint">{{ i18n.securityWarningSecret }}</text>

				<view
					v-if="
						currentExportWallet &&
						(currentExportWallet.canExportPrivateKey || currentExportWallet.canExportMnemonic)
					"
					class="export-btns-row"
				>
					<view v-if="currentExportWallet.canExportPrivateKey" class="export-btn-cell">
						<button
							class="btn-import btn-in-row"
							:disabled="exportLoading"
							@click="handleExportWallet('privateKey')"
						>
							<view class="btn-content">
								{{ exportLoading ? i18n.loading : i18n.exportPrivateKeyBtn }}
							</view>
						</button>
					</view>
					<view v-if="currentExportWallet.canExportMnemonic" class="export-btn-cell">
						<button
							class="btn-export btn-in-row"
							:disabled="exportLoading"
							@click="handleExportWallet('mnemonic')"
						>
							<view class="btn-content">
								{{ exportLoading ? i18n.loading : i18n.exportMnemonicBtn }}
							</view>
						</button>
					</view>
				</view>
				<text
					v-if="currentExportWallet && !currentExportWallet.canExportPrivateKey && !currentExportWallet.canExportMnemonic"
					class="ex-desc"
				>
					{{ i18n.exportNotAvailableForWallet }}
				</text>
			</view>
		</view>

		<!-- Create wallet summary（仅创建页展示；导入成功不显示此块） -->
		<view class="export-result-wrapper" v-if="currentTab === 'create' && walletSummaryResult">
			<view class="export-result-inner">
				<text class="ex-title">{{ i18n.exportResultTitle }}</text>
				<view class="result-item">
					<text class="result-label">{{ i18n.ethAddress }}</text>
					<text class="result-value">{{ walletSummaryResult.ethAddress }}</text>
				</view>
				<view class="result-item">
					<text class="result-label">{{ i18n.cfxMainnet }}</text>
					<text class="result-value">{{ walletSummaryResult.cfxAddress }}</text>
				</view>
				<view class="result-item">
					<text class="result-label">{{ i18n.cfxTestnet }}</text>
					<text class="result-value">{{ walletSummaryResult.cfxTestAddress }}</text>
				</view>
				<view class="result-item">
					<text class="result-label">{{ i18n.btcAddress }}</text>
					<text class="result-value">{{ walletSummaryResult.btcAddress }}</text>
				</view>
				<view class="result-item">
					<text class="result-label">{{ i18n.privateKeyLabel }}</text>
					<text class="result-value">{{ walletSummaryResult.privateKey }}</text>
				</view>
				<view class="result-item">
					<text class="result-label">{{ i18n.mnemonicLabel }}</text>
					<text class="result-value">{{ walletSummaryResult.mnemonic }}</text>
				</view>
				<button class="btn-export" @click="walletSummaryResult = null">
					<view class="btn-content">{{ i18n.close }}</view>
				</button>
			</view>
		</view>

		<!-- Export Result Modal -->
		<view class="export-result-wrapper" v-if="exportResult">
			<view class="export-result-inner">
				<text class="ex-title">{{ i18n.exportResultTitle }}</text>
				<text class="copy-banner">{{ i18n.tapToCopy }}</text>
				<view
					class="result-item result-item-tappable"
					v-if="exportResult.address"
					@click="copyToClipboard(exportResult.address)"
				>
					<text class="result-label">{{ i18n.walletAddressLabel }}</text>
					<text class="result-value">{{ exportResult.address }}</text>
				</view>
				<view
					class="result-item result-item-tappable"
					v-if="exportResultType === 'privateKey' && exportResult.privateKey"
					@click="copyToClipboard(exportResult.privateKey)"
				>
					<text class="result-label">{{ i18n.privateKeyLabel }}</text>
					<text class="result-value">{{ exportResult.privateKey }}</text>
				</view>
				<view
					class="result-item result-item-tappable"
					v-if="exportResultType === 'mnemonic' && exportResult.mnemonic"
					@click="copyToClipboard(exportResult.mnemonic)"
				>
					<text class="result-label">{{ i18n.mnemonicLabel }}</text>
					<text class="result-value">{{ exportResult.mnemonic }}</text>
				</view>
				<button class="btn-export" @click="closeExportResult">
					<view class="btn-content">{{ i18n.close }}</view>
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
				currentTab: 'create',
				importType: 'privateKey',
				walletName: '',
				importData: '',
				mnemonicPreviewList: [],
				selectedPreviewIndex: -1,
				exportResult: null,
				exportResultType: 'privateKey',
				walletSummaryResult: null,
				exportLoading: false,
				importLoading: false,
				previewLoading: false,
				wallets: [],
				walletIndex: 0,
				selectedNetworkId: 'cfx'
			}
		},
		watch: {
			currentTab(val) {
				if (val === 'export') {
					this.fetchExportWalletList()
				}
			},
			importType() {
				this.resetImportPreview()
			},
			importData() {
				this.resetImportPreview()
			}
		},
		onShow() {
			if (this.currentTab === 'export') {
				this.fetchExportWalletList()
			}
		},
		computed: {
			...mapGetters(['i18n', 'themeClass']),
			currentExportWallet() {
				if (!this.wallets || this.wallets.length === 0) return null
				const i = this.walletIndex
				if (i < 0 || i >= this.wallets.length) return this.wallets[0]
				return this.wallets[i]
			},
			selectedPreviewItem() {
				if (!Array.isArray(this.mnemonicPreviewList)) return null
				const i = this.selectedPreviewIndex
				if (i < 0 || i >= this.mnemonicPreviewList.length) return null
				return this.mnemonicPreviewList[i]
			},
			networks() {
				return [
					{ id: 'cfx', name: this.i18n.confluxCore },
					{ id: 'cfxtest', name: this.i18n.confluxCoreTestnet }
				]
			},
			selectedNetwork() {
				return this.networks.find(n => n.id === this.selectedNetworkId) || this.networks[0]
			}
		},
		methods: {
			copyToClipboard(text) {
				const s = text != null ? String(text).trim() : ''
				if (!s) return
				uni.setClipboardData({
					data: s,
					success: () => {
						uni.showToast({ title: this.i18n.copied, icon: 'none' })
					}
				})
			},
			closeExportResult() {
				this.exportResult = null
			},
			onExportWalletChange(e) {
				this.walletIndex = Number(e.detail.value || 0)
			},
			normalizeExportWalletRow(w) {
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
			async fetchExportWalletList() {
				try {
					const res = await api.getExportWalletList2()
					const list = Array.isArray(res) ? res : []
					this.wallets = list.map(w => this.normalizeExportWalletRow(w))
					const currentAddr = uni.getStorageSync('current_wallet_address')
					if (currentAddr) {
						const idx = this.wallets.findIndex(x => x.address === currentAddr)
						if (idx >= 0) this.walletIndex = idx
					}
					if (this.walletIndex >= this.wallets.length) {
						this.walletIndex = 0
					}
				} catch (e) {
					console.error('fetchExportWalletList failed', e)
					this.wallets = []
					this.walletIndex = 0
				}
			},
			async handleExportWallet(type) {
				const w = this.currentExportWallet
				if (!w) {
					uni.showToast({ title: this.i18n.selectWalletFirst, icon: 'none' })
					return
				}
				if (this.exportLoading) return
				this.exportLoading = true
				this.exportResultType = type
				try {
					if (w.address) {
						uni.setStorageSync('current_wallet_address', w.address)
					}
					const res = await api.exportWallet({
						accountId: w.accountId,
						address: w.address,
						exportType: type
					})
					if (type === 'mnemonic' && res && res.mnemonic) {
						this.exportResult = {
							address: res.address || w.address,
							mnemonic: res.mnemonic
						}
						uni.showToast({ title: this.i18n.exportSuccessful, icon: 'success' })
					} else if (type === 'privateKey' && res && res.privateKey) {
						this.exportResult = {
							address: res.address || w.address,
							privateKey: res.privateKey
						}
						uni.showToast({ title: this.i18n.exportSuccessful, icon: 'success' })
					} else {
						uni.showToast({ title: this.i18n.noWalletToExport, icon: 'none' })
					}
				} catch (err) {
					console.error('Export failed', err)
				} finally {
					this.exportLoading = false
				}
			},
			goBack() {
				uni.navigateBack()
			},
			goCustodyWalletNotice() {
				uni.navigateTo({
					url: '/pages/profile/custody_wallet_notice'
				})
			},
			resetImportPreview() {
				this.mnemonicPreviewList = []
				this.selectedPreviewIndex = -1
			},
			selectPreviewItem(index) {
				const item = this.mnemonicPreviewList[index]
				if (!item) return
				if (item.alreadyImported) {
					uni.showToast({ title: this.i18n.addressAlreadyImportedHint, icon: 'none' })
					return
				}
				this.selectedPreviewIndex = index
			},
			async handlePreviewMnemonic() {
				const mnemonic = (this.importData || '').trim()
				if (!mnemonic) {
					uni.showToast({ title: this.i18n.pleaseEnterData, icon: 'none' })
					return
				}
				if (this.previewLoading) return
				this.previewLoading = true
				try {
					const res = await api.previewImportWallet({
						chainId: this.selectedNetwork.id,
						mnemonic
					})
					const list = Array.isArray(res) ? res : []
					this.mnemonicPreviewList = list
					const firstAvailable = list.findIndex(item => !item.alreadyImported)
					this.selectedPreviewIndex = firstAvailable >= 0 ? firstAvailable : (list.length ? 0 : -1)
					if (!list.length) {
						uni.showToast({ title: this.i18n.addressPreviewEmpty, icon: 'none' })
					}
				} catch (err) {
					console.error('Preview mnemonic failed', err)
				} finally {
					this.previewLoading = false
				}
			},
			showNetworkPicker() {
				const networkNames = this.networks.map(n => n.name);
				uni.showActionSheet({
					itemList: networkNames,
					success: (res) => {
						this.selectedNetworkId = this.networks[res.tapIndex].id;
						if (this.currentTab === 'import') {
							this.resetImportPreview()
						}
					}
				});
			},
			async handleCreate() {
				if (!this.walletName.trim()) {
					uni.showToast({ title: this.i18n.pleaseEnterWalletName, icon: 'none' })
					return
				}
				if (this.walletName.trim().length > 20) {
					uni.showToast({ title: this.i18n.walletNameMaxLen || 'Wallet name max 20 characters', icon: 'none' })
					return
				}
				try {
					const accountData = {
						chainId: this.selectedNetwork.id,
						name: this.walletName.trim()
					};

					const res = await api.createWallet(accountData)
					
					uni.showToast({ title: this.i18n.createSuccessful, icon: 'success' })
					this.walletName = ''

					if (res) {
						let primaryAddress = res.ethAddress;
						if (this.selectedNetwork.id === 'cfx') {
							primaryAddress = res.cfxAddress;
						} else if (this.selectedNetwork.id === 'cfxtest') {
							primaryAddress = res.cfxTestAddress;
						}
						
						if (this.selectedNetwork.id === 'cfx' || this.selectedNetwork.id === 'cfxtest') {
							uni.setStorageSync('current_wallet_address', primaryAddress);
						} else {
							uni.setStorageSync('current_wallet_address', res.ethAddress);
						}
						
						// 清空代币和NFT缓存
						clearWalletCache()
						markWalletAssetsChanged()
						
						this.walletSummaryResult = {
							ethAddress: res.ethAddress || 'N/A',
							cfxAddress: res.cfxAddress || 'N/A',
							cfxTestAddress: res.cfxTestAddress || 'N/A',
							btcAddress: 'N/A',
							privateKey: this.i18n.createdNewWallet,
							mnemonic: this.i18n.createdNewWallet
						}
					}
				} catch (err) {
					console.error('Create failed', err)
					uni.showToast({ title: this.i18n.createFailed, icon: 'none' })
				}
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
				if (this.importLoading) return
				this.importLoading = true
				uni.showLoading({ title: this.i18n.importing || this.i18n.loading, mask: true })
				try {
					const accountData = {
						chainId: this.selectedNetwork.id,
						name: this.walletName.trim()
					};

					if (this.importType === 'privateKey') {
						accountData.privateKey = this.importData.trim();
						if (!accountData.privateKey.startsWith('0x')) {
							accountData.privateKey = '0x' + accountData.privateKey;
						}
					} else {
						if (!this.mnemonicPreviewList.length) {
							uni.showToast({ title: this.i18n.previewAddressesFirst, icon: 'none' })
							return
						}
						const selectedPreview = this.selectedPreviewItem
						if (!selectedPreview) {
							uni.showToast({ title: this.i18n.selectDerivedAddressFirst, icon: 'none' })
							return
						}
						if (selectedPreview.alreadyImported) {
							uni.showToast({ title: this.i18n.addressAlreadyImportedHint, icon: 'none' })
							return
						}
						accountData.mnemonic = this.importData.trim();
						accountData.deriveIndex = selectedPreview.deriveIndex
					}

					// Call Java backend to parse the mnemonic/private key
					const res = await api.importWallet(accountData)

					this.walletSummaryResult = null
					uni.showToast({ title: this.i18n.importSuccessful, icon: 'success' })
					this.importData = ''
					this.resetImportPreview()

					if (res) {
						// Store the primary address to local storage so Wallet and NFT pages can use it
						let primaryAddress = res.selectedAddress || res.ethAddress;
						if (!res.selectedAddress) {
							if (this.selectedNetwork.id === 'cfx') {
								primaryAddress = res.cfxAddress;
							} else if (this.selectedNetwork.id === 'cfxtest') {
								primaryAddress = res.cfxTestAddress;
							}
						}
						
						// Always prefer cfx/cfxtest base32 address if available when network is cfx
						if (this.selectedNetwork.id === 'cfx' || this.selectedNetwork.id === 'cfxtest') {
							uni.setStorageSync('current_wallet_address', primaryAddress);
						} else {
							uni.setStorageSync('current_wallet_address', res.ethAddress);
						}
						
						// 清空代币和NFT缓存
						clearWalletCache()
						markWalletAssetsChanged()
					}
				} catch (err) {
					console.error('Import failed', err)
					// 业务/网络错误已在 utils/request 中展示接口 msg 或通用提示，此处勿再 toast 覆盖
				} finally {
					uni.hideLoading()
					this.importLoading = false
				}
			},
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		background: var(--page-bg-gradient);
		padding: 0 32rpx 48rpx;
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
		border-radius: 24rpx;
		overflow: hidden;
		border: 1px solid var(--border-color);
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

	.sub-tab-group {
		display: flex;
		background-color: var(--control-bg);
		border-radius: 12rpx;
		padding: 4rpx;
		margin-bottom: 24rpx;
	}

	.sub-tab-item {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 12rpx 0;
		border-radius: 12rpx;
		
		&.active {
			background-color: var(--card-bg);
		}
	}

	.sub-tab-text {
		font-size: 26rpx;
		font-weight: bold;
		color: var(--text-primary);
	}
    
    .sub-tab-item:not(.active) .sub-tab-text {
        color: var(--text-secondary);
        font-weight: normal;
    }

	.card-wrapper {
		background-color: var(--card-bg);
		margin-bottom: 32rpx;
		border-radius: 28rpx;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.notice-card {
		background-color: var(--card-bg);
		border-radius: 28rpx;
		padding: 28rpx 32rpx;
		margin-bottom: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 20rpx;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.notice-card__body {
		display: flex;
		flex-direction: column;
		gap: 12rpx;
	}

	.notice-card__badge {
		align-self: flex-start;
		font-size: 22rpx;
		font-weight: 600;
		color: var(--btn-confirm-bg);
		background-color: rgba(88, 101, 242, 0.12);
		padding: 8rpx 16rpx;
		border-radius: 999rpx;
	}

	.notice-card__title {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.notice-card__desc {
		font-size: 26rpx;
		line-height: 1.6;
		color: var(--text-secondary);
	}

	.notice-card__btn {
		background-color: var(--control-bg);
		border: none;
		border-radius: 22rpx;
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: center;

		&::after {
			border: none;
		}
	}

	.notice-card__btn-text {
		font-size: 28rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.card-inner {
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.label, .ex-desc {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.ex-title {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
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

	.export-security-hint {
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
		margin-top: 16rpx;
	}

	.export-btn-cell {
		flex: 1;
		min-width: 0;
	}

	.btn-import.btn-in-row,
	.btn-export.btn-in-row {
		width: 100%;
		margin-top: 0;
	}

	.network-selector-wrapper {
		background-color: var(--control-bg);
		margin-bottom: 16rpx;
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
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
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
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

	.btn-import {
		background-color: var(--btn-confirm-bg);
		margin-top: 16rpx;
		border: none;
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100rpx;
		&::after { border: none; }
	}

	.btn-import[disabled] {
		opacity: 0.6;
	}

	.btn-import .btn-content {
		color: var(--btn-text-white);
		font-size: 32rpx;
		font-weight: bold;
	}

	.btn-export {
		background-color: var(--control-bg);
		margin-top: 16rpx;
		border: none;
		border-radius: 24rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100rpx;
		&::after { border: none; }
	}

	.btn-export[disabled] {
		opacity: 0.6;
	}

	.btn-export .btn-content {
		color: var(--text-primary);
		font-size: 32rpx;
		font-weight: bold;
	}

    .export-result-wrapper {
        background-color: var(--card-bg);
        margin-bottom: 32rpx;
		border-radius: 28rpx;
    }

	.export-result-inner {
		padding: 32rpx;
        display: flex;
        flex-direction: column;
        gap: 16rpx;
        word-break: break-all;
	}
    
    .result-item {
        display: flex;
        flex-direction: column;
        gap: 8rpx;
        margin-bottom: 16rpx;
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

	.copy-banner {
		font-size: 24rpx;
		color: var(--text-secondary);
		margin-bottom: 8rpx;
	}

	.result-item-tappable {
		padding: 16rpx;
		margin-left: -16rpx;
		margin-right: -16rpx;
		border-radius: 16rpx;
		background-color: var(--control-bg);
	}

	.preview-list {
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}

	.preview-item {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
		padding: 20rpx 24rpx;
		border-radius: 20rpx;
		background-color: var(--control-bg);
		border: 1px solid transparent;
	}

	.preview-item.active {
		border-color: var(--btn-confirm-bg);
	}

	.preview-item.disabled {
		opacity: 0.72;
	}

	.preview-item-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.preview-badge {
		font-size: 22rpx;
		color: var(--warning-text);
		background-color: var(--warning-bg);
		padding: 6rpx 12rpx;
		border-radius: 999rpx;
	}
</style>
