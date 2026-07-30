<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.receiveTitle }}</text>
			<view class="placeholder"></view>
		</view>

		<view class="card-qr">
			<view v-if="wallets.length > 0" class="wallet-select-block">
				<text class="wallet-select-label">{{ i18n.selectWallet }}</text>
				<picker
					@change="onWalletChange"
					:value="walletIndex"
					:range="wallets"
					range-key="displayTitle"
				>
					<view class="picker-box">
						<text class="picker-text">{{ currentWallet ? currentWallet.displayTitle : '' }}</text>
						<text class="picker-arrow">›</text>
					</view>
				</picker>
			</view>

			<view class="qr-wrapper">
				<canvas canvas-id="qrcode" :style="{ width: qrSize + 'px', height: qrSize + 'px' }"></canvas>
			</view>
			
			<view class="address-group">
				<text class="address-label">{{ i18n.yourAddress }}</text>
				<text class="address-val">{{ formatAddress(address) }}</text>
			</view>

			<view class="btn-row">
				<view class="btn-action-wrapper" @click="copyAddress">
					<view class="btn-action-inner">
						<image class="btn-icon-img" :src="$staticUrl('/static/icons/icon_copy.png')" mode="aspectFit"></image>
						<text class="btn-text">{{ i18n.copy }}</text>
					</view>
				</view>
			</view>
		</view>

		<view class="warning-box">
			<text class="warning-icon">⚠️</text>
			<text class="warning-text">{{ i18n.receiveWarning }}</text>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import UQRCode from 'uqrcodejs'
	import NavBackBtn from '@/components/nav-back-btn.vue'
	import { api } from '@/utils/request'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				address: '',
				qrSize: 200,
				wallets: [],
				walletIndex: 0,
				initialQueryAddress: ''
			}
		},
		computed: {
			...mapGetters(['i18n']),
			currentWallet() {
				if (!this.wallets || this.wallets.length === 0) return null
				const i = this.walletIndex < 0 || this.walletIndex >= this.wallets.length ? 0 : this.walletIndex
				return this.wallets[i]
			}
		},
		onLoad(options) {
			// Calculate QR code size based on 336rpx (400rpx wrapper - 32rpx padding * 2)
			this.qrSize = uni.upx2px(336);

			if (options.address) {
				this.initialQueryAddress = decodeURIComponent(options.address)
			}
		},
		onShow() {
			this.fetchWallets()
		},
		onReady() {
			this.$nextTick(() => this.makeQRCode())
		},
		methods: {
			normalizeWalletRow(w) {
				const addr = (w && w.address) ? String(w.address).trim() : ''
				const shortAddr =
					addr.length > 16 ? addr.substring(0, 8) + '...' + addr.substring(addr.length - 6) : addr
				return {
					...w,
					address: addr,
					displayTitle: w && w.name ? `${w.name} (${shortAddr})` : shortAddr
				}
			},
			syncAddressFromWalletIndex() {
				const w = this.currentWallet
				if (w && w.address) {
					this.address = w.address
					return
				}
				if (this.initialQueryAddress) {
					this.address = this.initialQueryAddress
					return
				}
				const currentAddr = uni.getStorageSync('current_wallet_address')
				this.address = currentAddr || ''
			},
			async fetchWallets() {
				try {
					const res = await api.getWalletListRaw()
					const list = Array.isArray(res) ? res : []
					const withAddr = list.filter((w) => w && w.address && String(w.address).trim())
					this.wallets = withAddr.map((w) => this.normalizeWalletRow(w))

					const q = this.initialQueryAddress
					if (q && !this.wallets.some((x) => x.address === q)) {
						this.wallets.unshift(this.normalizeWalletRow({ address: q, name: '' }))
					}

					let idx = 0
					if (this.initialQueryAddress) {
						const i = this.wallets.findIndex((x) => x.address === this.initialQueryAddress)
						if (i >= 0) idx = i
					} else {
						const currentAddr = uni.getStorageSync('current_wallet_address')
						if (currentAddr) {
							const i = this.wallets.findIndex((x) => x.address === currentAddr)
							if (i >= 0) idx = i
						}
					}
					this.walletIndex = idx
					if (this.walletIndex >= this.wallets.length) {
						this.walletIndex = 0
					}
					this.syncAddressFromWalletIndex()
				} catch (e) {
					console.error('fetchWallets failed', e)
					this.wallets = []
					this.syncAddressFromWalletIndex()
				}
				this.$nextTick(() => this.makeQRCode())
			},
			onWalletChange(e) {
				this.walletIndex = Number(e.detail.value || 0)
				this.syncAddressFromWalletIndex()
				this.$nextTick(() => this.makeQRCode())
			},
			formatAddress(addr) {
				if (!addr) return '';
				if (addr.length > 20) {
					return addr.substring(0, 10) + '...' + addr.substring(addr.length - 8);
				}
				return addr;
			},
			makeQRCode() {
				if (!this.address) return
				const qr = new UQRCode();
				qr.data = this.address;
				qr.size = this.qrSize;
				qr.make();
				
				const ctx = uni.createCanvasContext('qrcode', this);
				qr.canvasContext = ctx;
				qr.drawCanvas();
			},
			goBack() {
				uni.navigateBack()
			},
			copyAddress() {
				uni.setClipboardData({
					data: this.address,
					success: () => {
						uni.showToast({
							title: this.i18n.copied,
							icon: 'success'
						})
					}
				})
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

	.card-qr {
		background-color: var(--card-bg);
		padding: 64rpx 48rpx;
		border-radius: 28rpx;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 48rpx;
		margin-bottom: 48rpx;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.wallet-select-block {
		width: 100%;
		display: flex;
		flex-direction: column;
		gap: 16rpx;
	}

	.wallet-select-label {
		font-size: 28rpx;
		color: var(--text-secondary);
		align-self: flex-start;
	}

	.picker-box {
		width: 100%;
		box-sizing: border-box;
		background-color: var(--control-bg);
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
		padding: 24rpx 28rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.picker-text {
		flex: 1;
		font-size: 28rpx;
		color: var(--text-primary);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		padding-right: 16rpx;
	}

	.picker-arrow {
		font-size: 32rpx;
		color: var(--text-secondary);
		line-height: 1;
	}

	.qr-wrapper {
		width: 400rpx;
		height: 400rpx;
		background-color: var(--card-bg-strong);
		padding: 32rpx;
		border-radius: 24rpx;
		box-sizing: border-box;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: var(--qr-shadow);
	}

	.qr-img {
		width: 100%;
		height: 100%;
	}

	.address-group {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 16rpx;
	}

	.address-label {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.address-val {
		font-size: 40rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.btn-row {
		display: flex;
		width: 100%;
		gap: 32rpx;
	}

	.btn-action-wrapper {
		flex: 1;
		height: 112rpx;
		background-color: var(--control-bg);
		border-radius: 24rpx;
		border: 1px solid var(--border-color);
		
		&:active {
			opacity: 0.8;
		}
	}

	.btn-action-inner {
		display: flex;
		align-items: center;
		justify-content: center;
		height: 100%;
	}

	.btn-icon-img {
		width: 36rpx;
		height: 36rpx;
		margin-right: 16rpx;
	}

	.btn-text {
		font-size: 32rpx;
		font-weight: bold;
		color: var(--text-primary);
	}

	.warning-box {
		background-color: var(--warning-bg);
		padding: 32rpx;
		border-radius: 24rpx;
		display: flex;
		align-items: flex-start;
		gap: 16rpx;
	}

	.warning-icon {
		font-size: 36rpx;
		color: var(--warning-text);
	}

	.warning-text {
		flex: 1;
		font-size: 24rpx;
		color: var(--warning-text);
		line-height: 1.5;
	}
</style>
