<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ isNFT ? i18n.sendNft : i18n.sendCrypto }}</text>
			<view class="placeholder"></view>
		</view>

		<view class="card-form">
			<!-- From Wallet Selection -->
			<view class="input-group" v-if="!isNFT">
				<view class="amount-header">
					<text class="input-label">{{ i18n.fromWallet }}</text>
				</view>
				<picker @change="onWalletChange" :value="walletIndex" :range="wallets" range-key="displayTitle">
					<view class="wallet-picker-box">
						<view class="wallet-info">
							<text class="wallet-icon">💳</text>
							<text class="wallet-name">{{ currentWallet ? currentWallet.displayTitle : i18n.selectWalletPlaceholder }}</text>
						</view>
						<text class="picker-arrow">▼</text>
					</view>
				</picker>
			</view>

			<!-- NFT Preview if type is NFT -->
			<view class="nft-preview" v-if="isNFT">
				<image class="preview-img" :src="nftData.image || $staticUrl('/static/nft_detail.png')" mode="aspectFill"></image>
				<view class="preview-info">
					<text class="preview-name">{{ nftData.name || 'Unknown NFT' }}</text>
					<text class="preview-series" v-if="nftData.series">{{ nftData.series }}</text>
					<text class="preview-series" v-if="tokenId">Token ID: {{ tokenId }}</text>
				</view>
			</view>

			<!-- Token Selection and Amount -->
			<view class="crypto-amount" v-else>
				<view class="input-group">
					<view class="amount-header">
						<text class="input-label">{{ i18n.selectToken }}</text>
						<text v-if="tokens.length > 0" class="amount-balance">{{ i18n.balance || 'Balance: ' }}{{ currentToken ? formatTokenBalance(currentToken.amount) : '0.0000' }} {{ currentToken ? currentToken.symbol : '' }}</text>
						<text v-else-if="!tokensLoading" class="amount-balance empty-token-hint">{{ i18n.noAvailableTokens }}</text>
					</view>
					<picker v-if="tokens.length > 0" @change="onTokenChange" :value="tokenIndex" :range="tokens" range-key="symbol">
						<view class="wallet-picker-box">
							<view class="wallet-info">
								<image v-if="currentToken && currentToken.icon" :src="currentToken.icon" class="token-icon" mode="aspectFit"></image>
								<text class="wallet-icon" v-else>🪙</text>
								<text class="wallet-name">{{ currentToken ? currentToken.symbol : i18n.selectTokenPlaceholder }}</text>
							</view>
							<text class="picker-arrow">▼</text>
						</view>
					</picker>
					<view v-else-if="tokensLoading" class="wallet-picker-box token-picker-muted">
						<text class="wallet-name muted-text">{{ i18n.loading }}</text>
					</view>
					<view v-else class="wallet-picker-box token-picker-muted no-token-box">
						<text class="wallet-name muted-text">{{ i18n.noAvailableTokens }}</text>
					</view>
				</view>

				<template v-if="tokens.length > 0">
					<view class="amount-header" style="margin-top: 16rpx;">
						<text class="amount-label">{{ i18n.amount || 'Amount' }}</text>
					</view>
					<view class="amount-input-group">
						<input class="amount-input" type="digit" placeholder="0.00" v-model="amount" />
						<view class="max-btn" @click="setMaxAmount">{{ i18n.max || 'Max' }}</view>
					</view>
				</template>
			</view>

			<!-- Address Input：扫码入口放在标签行，避免 APP 原生 input 盖住同排按钮 -->
			<view class="input-group">
				<view class="address-label-row">
					<text class="input-label">{{ i18n.toAddress }}</text>
					<!-- #ifndef H5 -->
					<view class="scan-btn" hover-class="scan-btn-hover" @tap.stop="scanQRCode">
						<text class="scan-btn-text">{{ i18n.scanQr }}</text>
					</view>
					<!-- #endif -->
				</view>
				<view class="input-box">
					<input class="address-input" type="text" placeholder="0x... or cfx:..." v-model="address" />
				</view>
			</view>
		</view>

		<!-- Action Area -->
		<view class="action-area">
			<view class="transfer-out-hint">
				<text class="transfer-out-hint-text">{{ i18n.sendTransferOutHint }}</text>
			</view>
			<button
				class="btn-confirm"
				@click="handleConfirm"
				:disabled="loading || (!isNFT && (tokensLoading || !tokens.length))"
			>
				<view class="btn-content">{{ loading ? (i18n.sending || 'Sending...') : i18n.confirmSend }}</view>
			</button>
		</view>

		<view v-if="tradeModalVisible" class="trade-mask" @click.self="closeTradeModal">
			<view class="trade-panel" :class="themeClass" @click.stop>
				<text class="trade-title">{{ i18n.tradeAuthTitle }}</text>
				<!-- #ifdef APP-PLUS -->
				<view v-if="canUseBiometricTrade" class="trade-tabs">
					<text class="trade-tab" :class="{ active: tradeAuthMode === 'bio' }" @click="tradeAuthMode = 'bio'">{{ i18n.tradeAuthBio }}</text>
					<text class="trade-tab" :class="{ active: tradeAuthMode === 'pwd' }" @click="tradeAuthMode = 'pwd'">{{ i18n.tradeAuthPwd }}</text>
				</view>
				<!-- #endif -->
				<view v-if="tradeAuthMode === 'pwd'" class="trade-pwd-wrap">
					<input
						class="trade-pwd-input"
						type="password"
						:placeholder="i18n.tradePwdPlaceholder"
						v-model="tradePasswordInput"
						placeholder-class="placeholder-text"
					/>
				</view>
				<view v-else class="trade-bio-hint">
					<text class="trade-bio-text">{{ i18n.tradeAuthBioHint }}</text>
				</view>
				<view class="trade-actions">
					<button class="trade-btn trade-btn-cancel" @click="closeTradeModal">{{ i18n.cancelBtn }}</button>
					<button class="trade-btn trade-btn-ok" @click="confirmTradeUnlock">{{ i18n.confirmBtn }}</button>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { formatFixed4 } from '@/utils/format-money'
	import { invalidateTxRelatedCache, syncTxProgress } from '@/utils/tx-progress'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	const BIOMETRIC_STORAGE_KEY = 'app_biometric_login_info'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				isNFT: false,
				address: '',
				amount: '',
				nftId: null,
				contract: null,
				tokenId: null,
				nftData: {},
				loading: false,
				wallets: [],
				walletIndex: 0,
				tokens: [],
				tokenIndex: 0,
				fromWalletBalance: '0.00',
				hasTradePassword: false,
				tradeSessionToken: '',
				tradeModalVisible: false,
				tradeAuthMode: 'pwd',
				tradePasswordInput: '',
				_tradeUnlockResolver: null,
				tokensLoading: true
			}
		},
		computed: {
			...mapGetters(['i18n']),
			currentWallet() {
				if (this.wallets.length > 0 && this.walletIndex >= 0 && this.walletIndex < this.wallets.length) {
					return this.wallets[this.walletIndex];
				}
				return null;
			},
			currentToken() {
				if (this.tokens.length > 0 && this.tokenIndex >= 0 && this.tokenIndex < this.tokens.length) {
					return this.tokens[this.tokenIndex];
				}
				return null;
			},
			canUseBiometricTrade() {
				let isApp = false
				// #ifdef APP-PLUS
				isApp = true
				// #endif
				if (!isApp) return false
				try {
					const b = uni.getStorageSync(BIOMETRIC_STORAGE_KEY)
					return !!(b && b.biometricToken)
				} catch (e) {
					return false
				}
			}
		},
		onLoad(options) {
			if (options.type === 'nft' || options.id || options.contract) {
				this.isNFT = true;
				this.nftId = options.id;
				this.contract = options.contract;
				this.tokenId = options.tokenId;
				if (this.nftId || (this.contract && this.tokenId)) {
					this.fetchNftDetail();
				}
			}
			if (!this.isNFT) {
				this.fetchWallets();
			}
			this.refreshTradePwdStatus();
		},
		methods: {
			async refreshTradePwdStatus() {
				try {
					const s = await api.getTradePwdStatus();
					this.hasTradePassword = !!(s && s.hasTradePassword);
				} catch (e) {
					this.hasTradePassword = false;
				}
			},
			initTradeModalDefaults() {
				this.tradePasswordInput = '';
				this.tradeAuthMode = this.canUseBiometricTrade ? 'bio' : 'pwd';
			},
			closeTradeModal() {
				this.tradeModalVisible = false;
				if (this._tradeUnlockResolver) {
					this._tradeUnlockResolver(false);
					this._tradeUnlockResolver = null;
				}
			},
			async runSoterAuth() {
				const capability = await new Promise((resolve) => {
					uni.checkIsSupportSoterAuthentication({
						success: resolve,
						fail: () => resolve({ supportMode: [] })
					});
				});
				const modes = Array.isArray(capability.supportMode) ? capability.supportMode : [];
				if (!modes.includes('fingerPrint')) {
					uni.showToast({ title: this.i18n.biometricCapabilityUnavailable, icon: 'none' });
					return false;
				}
				return await new Promise((resolve) => {
					uni.startSoterAuthentication({
						requestAuthModes: ['fingerPrint'],
						challenge: `trade_${Date.now()}`,
						authContent: this.i18n.tradeAuthBioHint,
						success: () => resolve(true),
						fail: () => {
							uni.showToast({ title: this.i18n.fingerprintAuthFailed, icon: 'none' });
							resolve(false);
						}
					});
				});
			},
			async confirmTradeUnlock() {
				try {
					let payload = {};
					if (this.tradeAuthMode === 'bio') {
						const passed = await this.runSoterAuth();
						if (!passed) return;
						const bio = uni.getStorageSync(BIOMETRIC_STORAGE_KEY);
						if (!bio || !bio.biometricToken) {
							uni.showToast({ title: this.i18n.pleaseEnableBiometricFirst, icon: 'none' });
							return;
						}
						payload = { biometricToken: bio.biometricToken };
					} else {
						if (!this.tradePasswordInput) {
							uni.showToast({ title: this.i18n.tradePwdPlaceholder, icon: 'none' });
							return;
						}
						payload = { tradePassword: this.tradePasswordInput };
					}
					uni.showLoading({ title: this.i18n.verifying || '...' });
					const r = await api.unlockTradeSession(payload);
					uni.hideLoading();
					if (!r || !r.tradeSessionToken) {
						uni.showToast({ title: this.i18n.tradeUnlockFailed, icon: 'none' });
						return;
					}
					this.tradeSessionToken = r.tradeSessionToken;
					this.tradeModalVisible = false;
					if (this._tradeUnlockResolver) {
						this._tradeUnlockResolver(true);
						this._tradeUnlockResolver = null;
					}
				} catch (e) {
					uni.hideLoading();
					const msg = (e && e.msg) || this.i18n.tradeUnlockFailed;
					uni.showToast({ title: msg, icon: 'none' });
				}
			},
			async ensureTradeSession() {
				if (this.tradeSessionToken) {
					return true;
				}
				this.initTradeModalDefaults();
				this.tradeModalVisible = true;
				return await new Promise((resolve) => {
					this._tradeUnlockResolver = resolve;
				});
			},
			goBack() {
				uni.navigateBack()
			},
			onWalletChange(e) {
				this.walletIndex = e.detail.value;
				this.updateFromWalletBalance();
			},
			onTokenChange(e) {
				this.tokenIndex = e.detail.value;
			},
			formatTokenBalance(value) {
				return formatFixed4(value);
			},
			setMaxAmount() {
				if (this.currentToken) {
					this.amount = this.currentToken.amount;
				}
			},
			async fetchWallets() {
				try {
					const res = await api.getWalletList();
					if (res && res.length > 0) {
						this.wallets = res.map(w => {
							const shortAddr = w.address.length > 12 ? w.address.substring(0, 6) + '...' + w.address.substring(w.address.length - 4) : w.address;
							return {
								...w,
								displayTitle: w.name ? `${w.name} (${shortAddr})` : shortAddr
							};
						});
					} else {
						let addr = uni.getStorageSync('current_wallet_address') || 'cfx:aamgvyzht7h1zxdghb9ee9w26wrz8rd3gj837392dp';
						const shortAddr = addr.length > 12 ? addr.substring(0, 6) + '...' + addr.substring(addr.length - 4) : addr;
						this.wallets = [{ address: addr, chainId: '1029', name: 'Default', displayTitle: `Default (${shortAddr})` }];
					}
					
					// set to currently selected wallet if possible
					const currentAddr = uni.getStorageSync('current_wallet_address');
					if (currentAddr) {
						const idx = this.wallets.findIndex(w => w.address === currentAddr);
						if (idx >= 0) {
							this.walletIndex = idx;
						}
					}
					
					this.updateFromWalletBalance();
				} catch (e) {
					console.error('Failed to fetch wallets:', e);
				}
			},
			async updateFromWalletBalance() {
				if (!this.currentWallet) return;
				this.tokensLoading = true;
				try {
					const tokensRes = await api.getTokens(this.currentWallet.chainId || '1029', this.currentWallet.address);
					if (tokensRes && tokensRes.length > 0) {
						this.tokens = tokensRes;
						this.tokenIndex = 0; // Reset token selection
						const nativeToken = tokensRes.find(t => t.symbol === 'CFX' || t.type === 'native') || tokensRes[0];
						this.fromWalletBalance = nativeToken.amount;
					} else {
						this.tokens = [];
						this.tokenIndex = 0;
						this.amount = '';
						this.fromWalletBalance = '0.00';
					}
				} catch (e) {
					console.error('Failed to fetch balance:', e);
					this.tokens = [];
					this.tokenIndex = 0;
					this.amount = '';
					this.fromWalletBalance = '0.00';
				} finally {
					this.tokensLoading = false;
				}
			},
			scanQRCode() {
				uni.scanCode({
					success: (res) => {
						if (res.result) {
							this.address = res.result;
						}
					},
					fail: (err) => {
						console.error('Scan failed', err);
						uni.showToast({ title: this.i18n.scanFailed || 'Scan failed', icon: 'none' });
					}
				});
			},
			async fetchNftDetail() {
				try {
					if (this.contract && this.tokenId) {
						const res = await api.getNftDetail(this.nftId, this.contract, this.tokenId);
						if (res) {
							this.nftData = res;
						}
					}
				} catch (e) {
					console.error('Failed to fetch NFT detail:', e);
				}
			},
			async handleConfirm() {
				if (!this.address) {
					uni.showToast({ title: this.i18n.pleaseEnterToAddress || 'Please enter recipient address', icon: 'none' });
					return;
				}
				if (!this.isNFT && (this.tokensLoading || !this.tokens.length || !this.currentToken)) {
					uni.showToast({ title: this.i18n.noAvailableTokens, icon: 'none' });
					return;
				}
				if (!this.isNFT && !this.amount) {
					uni.showToast({ title: this.i18n.pleaseEnterAmount || 'Please enter amount', icon: 'none' });
					return;
				}

				if (!this.isNFT && this.currentToken) {
					const inputAmount = parseFloat(this.amount);
					const maxAmount = parseFloat(this.currentToken.amount);
					if (isNaN(inputAmount) || inputAmount <= 0) {
						uni.showToast({ title: this.i18n.invalidAmount || 'Please enter a valid amount', icon: 'none' });
						return;
					}
					if (inputAmount > maxAmount) {
						uni.showToast({ title: this.i18n.amountExceedsBalance || 'Amount exceeds balance', icon: 'none' });
						return;
					}
				}

				await this.refreshTradePwdStatus();
				if (!this.hasTradePassword) {
					uni.showModal({
						title: this.i18n.tradePwdRequiredTitle,
						content: this.i18n.tradePwdRequiredBody,
						confirmText: this.i18n.goSetTradePwd,
						cancelText: this.i18n.cancelBtn,
						success: (res) => {
							if (res.confirm) {
								uni.navigateTo({ url: '/pages/profile/trade_password' });
							}
						}
					});
					return;
				}

				const unlocked = await this.ensureTradeSession();
				if (!unlocked) {
					return;
				}

				const extra = { tradeSessionToken: this.tradeSessionToken };

				try {
					this.loading = true;
					uni.showLoading({ title: this.i18n.sending || 'Sending...' });
					const chainId = this.currentWallet ? (this.currentWallet.chainId || '1029') : '1029'
					let txHash = ''
					let txRecord = null

					if (this.isNFT) {
						const fromAddress = uni.getStorageSync('current_wallet_address');
						txHash = await api.sendNft(fromAddress, this.address, this.contract, this.tokenId, this.nftData.standard || 'ERC721', {
							tokenName: this.nftData.name,
							tokenSymbol: this.nftData.series,
							tradeSessionToken: extra.tradeSessionToken
						});
						txRecord = {
							txHash,
							chainId,
							fromAddress,
							toAddress: this.address,
							amount: 1,
							isNFT: true,
							tokenName: this.nftData.name || '',
							tokenSymbol: this.nftData.name ? `NFT: ${this.nftData.name}` : 'NFT',
							nftImage: this.nftData.image || '',
							tokenId: this.tokenId || '',
							contract: this.contract || '',
							status: 'Pending',
							createTime: Date.now()
						}
					} else {
						const fromAddress = this.currentWallet ? this.currentWallet.address : uni.getStorageSync('current_wallet_address');
						const t = this.currentToken;
						const isNative =
							!t ||
							t.type === 'native' ||
							t.symbol === 'CFX' ||
							!t.contract;
						if (isNative) {
							txHash = await api.sendCfx(fromAddress, this.address, this.amount, extra);
						} else {
							txHash = await api.sendToken(
								fromAddress,
								this.address,
								this.amount,
								t.contract,
								t.symbol,
								t.decimals != null ? t.decimals : 18,
								extra
							);
						}
						txRecord = {
							txHash,
							chainId,
							fromAddress,
							toAddress: this.address,
							amount: this.amount,
							tokenSymbol: t && t.symbol ? t.symbol : 'CFX',
							status: 'Pending',
							createTime: Date.now()
						}
					}

					this.tradeSessionToken = '';
					if (txRecord && txRecord.txHash) {
						syncTxProgress(txRecord)
						invalidateTxRelatedCache(txRecord)
					}
					uni.hideLoading();
					const payload = encodeURIComponent(JSON.stringify(txRecord || {}))
					uni.navigateTo({
						url: `/pages/wallet/transaction_progress?txHash=${encodeURIComponent(txHash)}&payload=${payload}`
					});
				} catch (e) {
					uni.hideLoading();
					let errorMsg = e.message || '发送失败';
					if (errorMsg && errorMsg.includes('transaction is reverted')) {
						errorMsg = 'Gas不足或合约回滚，请确保余额充足。';
					}
					uni.showToast({ title: errorMsg, icon: 'none', duration: 3000 });
				} finally {
					this.loading = false;
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
		display: flex;
		flex-direction: column;
	}

	.header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
	}

	.back-btn, .placeholder {
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
		padding: 48rpx;
		border-radius: 28rpx;
		display: flex;
		flex-direction: column;
		gap: 32rpx;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.nft-preview {
		display: flex;
		align-items: center;
		background-color: var(--control-bg);
		padding: 24rpx;
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
	}

	.preview-img {
		width: 128rpx;
		height: 128rpx;
		margin-right: 24rpx;
	}

	.preview-info {
		display: flex;
		flex-direction: column;
	}

	.preview-name {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
		margin-bottom: 8rpx;
	}

	.preview-series {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.crypto-amount {
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}

	.amount-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.amount-label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.amount-balance {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.amount-input-group {
		display: flex;
		align-items: center;
		background-color: var(--control-bg);
		padding: 24rpx 32rpx;
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
	}

	.amount-input {
		flex: 1;
		font-size: 48rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.max-btn {
		font-size: 24rpx;
		font-weight: bold;
		color: var(--accent-color);
		background-color: var(--max-btn-bg);
		padding: 12rpx 24rpx;
		border-radius: 16rpx;
	}

	.input-group {
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}

	.input-label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.input-box {
		display: flex;
		align-items: center;
		background-color: var(--control-bg);
		padding: 24rpx;
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
	}

	.address-label-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 16rpx;
	}

	.address-input {
		width: 100%;
		font-size: 32rpx;
		color: var(--text-primary);
	}

	.scan-btn {
		flex-shrink: 0;
		padding: 10rpx 20rpx;
		border-radius: 16rpx;
		border: 1px solid var(--accent-color);
		background-color: var(--max-btn-bg);
	}

	.scan-btn-hover {
		opacity: 0.85;
	}

	.scan-btn-text {
		font-size: 26rpx;
		font-weight: 600;
		color: var(--accent-color);
		white-space: nowrap;
	}
	
	.wallet-picker-box {
		display: flex;
		align-items: center;
		justify-content: space-between;
		background-color: var(--control-bg);
		padding: 24rpx;
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
	}
	
	.wallet-info {
		display: flex;
		align-items: center;
		gap: 12rpx;
	}
	
	.wallet-icon {
		font-size: 36rpx;
	}
	
	.token-icon {
		width: 40rpx;
		height: 40rpx;
		border-radius: 50%;
	}
	
	.wallet-name {
		font-size: 32rpx;
		color: var(--text-primary);
	}
	
	.picker-arrow {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.token-picker-muted {
		justify-content: center;
	}

	.no-token-box {
		border-style: dashed;
	}

	.muted-text {
		color: var(--text-secondary);
		font-size: 28rpx;
	}

	.empty-token-hint {
		color: var(--text-secondary);
	}

	.transfer-out-hint {
		padding: 0 8rpx 24rpx;
	}

	.transfer-out-hint-text {
		font-size: 24rpx;
		line-height: 1.55;
		color: var(--text-secondary);
		display: block;
	}

	.gas-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-top: 16rpx;
		border-top: 2rpx solid var(--border-color);
	}

	.gas-label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.gas-val {
		font-size: 28rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.action-area {
		margin-top: auto;
		padding-bottom: 80rpx;
	}

	.btn-confirm {
		width: 100%;
		height: 112rpx;
		background-color: var(--btn-confirm-bg);
		margin-bottom: 32rpx;
		border-radius: 24rpx;
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
		&::after { border: none; }
	}

	.btn-confirm .btn-content {
		color: var(--btn-text-white);
		font-size: 32rpx;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.trade-mask {
		position: fixed;
		left: 0;
		right: 0;
		top: 0;
		bottom: 0;
		background: rgba(15, 23, 42, 0.45);
		z-index: 1000;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 48rpx;
		box-sizing: border-box;
	}

	.trade-panel {
		width: 100%;
		max-width: 620rpx;
		padding: 40rpx 36rpx 32rpx;
		border-radius: 28rpx;
		background: var(--card-bg-strong, var(--card-bg));
		border: 1px solid var(--border-color);
		box-shadow: var(--card-shadow-lg);
	}

	.trade-title {
		font-size: 32rpx;
		font-weight: 700;
		color: var(--text-primary);
		display: block;
		margin-bottom: 28rpx;
		text-align: center;
	}

	.trade-tabs {
		display: flex;
		gap: 16rpx;
		margin-bottom: 24rpx;
	}

	.trade-tab {
		flex: 1;
		text-align: center;
		padding: 16rpx;
		border-radius: 16rpx;
		font-size: 26rpx;
		color: var(--text-secondary);
		background: var(--control-bg);
	}

	.trade-tab.active {
		color: var(--accent-color);
		font-weight: 700;
		background: var(--segment-active-bg);
	}

	.trade-pwd-wrap {
		margin-bottom: 28rpx;
	}

	.trade-pwd-input {
		width: 100%;
		height: 88rpx;
		padding: 0 24rpx;
		box-sizing: border-box;
		border-radius: 20rpx;
		background: var(--control-bg);
		border: 1px solid var(--border-color);
		font-size: 30rpx;
		color: var(--text-primary);
	}

	.trade-bio-hint {
		padding: 24rpx 0 32rpx;
	}

	.trade-bio-text {
		font-size: 26rpx;
		color: var(--text-secondary);
		line-height: 1.5;
	}

	.trade-actions {
		display: flex;
		gap: 20rpx;
	}

	.trade-btn {
		flex: 1;
		height: 88rpx;
		line-height: 88rpx;
		border-radius: 20rpx;
		font-size: 28rpx;
		font-weight: 600;
		border: none;
	}

	.trade-btn::after {
		border: none;
	}

	.trade-btn-cancel {
		background: var(--control-bg);
		color: var(--text-secondary);
	}

	.trade-btn-ok {
		background: var(--btn-confirm-bg);
		color: var(--btn-text-white);
	}
</style>
