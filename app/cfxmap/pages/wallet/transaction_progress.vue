<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.transactionProgress }}</text>
			<view class="header-spacer"></view>
		</view>

		<scroll-view scroll-y class="scroll-area">
			<view class="content">
				<view v-if="isNFTTx" class="nft-card">
					<image class="nft-image" :src="nftImageSrc" mode="aspectFill"></image>
					<view class="nft-info">
						<text class="nft-label">NFT</text>
						<text class="nft-name">{{ nftName }}</text>
						<text v-if="txRecord.tokenId" class="nft-meta">Token ID: {{ txRecord.tokenId }}</text>
					</view>
				</view>

				<view class="status-card">
					<text class="status-caption">{{ i18n.txCurrentStatus }}</text>
					<text class="status-value" :class="statusClass">{{ statusLabel }}</text>
					<text v-if="isPending" class="status-hint">{{ i18n.txProgressPendingHint }}</text>
				</view>

				<view class="timeline-card">
					<view v-for="(step, index) in timelineSteps" :key="step.key" class="timeline-item">
						<view class="timeline-line" v-if="index !== timelineSteps.length - 1"></view>
						<view class="timeline-dot" :class="step.state"></view>
						<view class="timeline-text">
							<text class="timeline-title" :class="step.state">{{ step.label }}</text>
						</view>
					</view>
				</view>

				<view class="detail-card">
					<view class="detail-row">
						<text class="detail-label">{{ i18n.amount }}</text>
						<text class="detail-value">{{ displayAmount }}</text>
					</view>
					<view v-if="isNFTTx && txRecord.tokenId" class="detail-row">
						<text class="detail-label">Token ID</text>
						<text class="detail-value">{{ txRecord.tokenId }}</text>
					</view>
					<view v-if="isNFTTx && txRecord.contract" class="detail-row">
						<text class="detail-label">Contract</text>
						<view class="detail-action">
							<text class="detail-value detail-value--mono">{{ txRecord.contract }}</text>
							<text class="detail-link" @click="copyText(txRecord.contract)">{{ i18n.copyAddressBtn }}</text>
						</view>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txNetwork }}</text>
						<text class="detail-value">{{ networkLabel }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txFromAddress }}</text>
						<view class="detail-action">
							<text class="detail-value detail-value--mono">{{ txRecord.fromAddress || '--' }}</text>
							<text class="detail-link" @click="copyText(txRecord.fromAddress)">{{ i18n.copyAddressBtn }}</text>
						</view>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txToAddress }}</text>
						<view class="detail-action">
							<text class="detail-value detail-value--mono">{{ txRecord.toAddress || '--' }}</text>
							<text class="detail-link" @click="copyText(txRecord.toAddress)">{{ i18n.copyAddressBtn }}</text>
						</view>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txHashLabel }}</text>
						<view class="detail-action">
							<text class="detail-value detail-value--mono">{{ txRecord.txHash || '--' }}</text>
							<text class="detail-link" @click="copyText(txRecord.txHash)">{{ i18n.copyTxHash }}</text>
						</view>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txGasFee }}</text>
						<text class="detail-value">{{ displayGasFee }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txBlockTime }}</text>
						<text class="detail-value">{{ displayBlockTime }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txBlockHeight }}</text>
						<text class="detail-value">{{ displayBlockHeight }}</text>
					</view>
					<view class="detail-row">
						<text class="detail-label">{{ i18n.txBlockHash }}</text>
						<view class="detail-action">
							<text class="detail-value detail-value--mono">{{ txRecord.blockHash || '--' }}</text>
							<text class="detail-link" @click="copyText(txRecord.blockHash)">{{ i18n.copyTxHash }}</text>
						</view>
					</view>
					<view v-if="txRecord.errorMessage" class="detail-row">
						<text class="detail-label">{{ i18n.txFailureReason }}</text>
						<text class="detail-value">{{ txRecord.errorMessage }}</text>
					</view>
					<button class="btn-secondary" @click="openExplorer">
						<text class="btn-secondary-text">{{ i18n.viewOnExplorer }}</text>
					</button>
				</view>
			</view>
		</scroll-view>

		<view class="action-bar">
			<button class="btn-primary" @click="goTransactions">
				<text class="btn-primary-text">{{ i18n.viewAllTransactions }}</text>
			</button>
			<button class="btn-ghost" @click="goWallet">
				<text class="btn-ghost-text">{{ i18n.backToWallet }}</text>
			</button>
		</view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { buildExplorerUrl, getTxProgressByHash, invalidateTxRelatedCache, syncTxProgress } from '@/utils/tx-progress'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				txHash: '',
				shouldPoll: false,
				txRecord: {
					txHash: '',
					fromAddress: '',
					toAddress: '',
					amount: '',
					tokenSymbol: 'CFX',
					tokenName: '',
					status: 'Pending',
					chainId: '1029',
					gasFee: '',
					createTime: Date.now(),
					blockTime: '',
					blockHeight: '',
					blockHash: '',
					errorMessage: '',
					isNFT: false,
					nftImage: '',
					tokenId: '',
					contract: ''
				},
				pollTimer: null,
				loading: false
			}
		},
		computed: {
			...mapGetters(['i18n', 'themeClass']),
			isNFTTx() {
				return !!this.txRecord.isNFT || /^NFT(?::|\s|$)/i.test(String(this.txRecord.tokenSymbol || '').trim())
			},
			normalizedStatus() {
				const raw = String(this.txRecord.status || '').trim().toLowerCase()
				if (raw === 'success' || raw === 'completed') return 'completed'
				if (raw === 'failed' || raw === 'failure') return 'failed'
				return 'pending'
			},
			isPending() {
				return this.normalizedStatus === 'pending'
			},
			statusLabel() {
				if (this.normalizedStatus === 'completed') return this.i18n.txStatusCompleted
				if (this.normalizedStatus === 'failed') return this.i18n.txStatusFailed
				return this.i18n.txStatusPending
			},
			statusClass() {
				return `status-${this.normalizedStatus}`
			},
			networkLabel() {
				return String(this.txRecord.chainId || '') === '1' ? this.i18n.cfxTestnet : 'CFX Mainnet'
			},
			nftName() {
				if (this.txRecord.tokenName) return this.txRecord.tokenName
				const raw = String(this.txRecord.tokenSymbol || '').trim()
				if (!raw) return 'NFT'
				return raw.replace(/^NFT:\s*/i, '') || 'NFT'
			},
			nftImageSrc() {
				return this.txRecord.nftImage || this.txRecord.image || this.$staticUrl('/static/icons/placeholder_nft.png')
			},
			displayAmount() {
				const amount = this.txRecord.amount != null && this.txRecord.amount !== '' ? this.txRecord.amount : '--'
				const symbol = this.txRecord.tokenSymbol || 'CFX'
				if (this.isNFTTx) {
					return `${amount} NFT`
				}
				return `${amount} ${symbol}`
			},
			displayGasFee() {
				if (this.txRecord.gasFee == null || this.txRecord.gasFee === '') return '--'
				return `${this.txRecord.gasFee} CFX`
			},
			displayBlockTime() {
				const time = this.txRecord.blockTime || this.txRecord.createTime
				if (!time) return '--'
				const date = new Date(time)
				if (Number.isNaN(date.getTime())) return '--'
				const y = date.getFullYear()
				const m = String(date.getMonth() + 1).padStart(2, '0')
				const d = String(date.getDate()).padStart(2, '0')
				const hh = String(date.getHours()).padStart(2, '0')
				const mm = String(date.getMinutes()).padStart(2, '0')
				const ss = String(date.getSeconds()).padStart(2, '0')
				return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
			},
			displayBlockHeight() {
				return this.txRecord.blockHeight != null && this.txRecord.blockHeight !== '' ? this.txRecord.blockHeight : '--'
			},
			timelineSteps() {
				const currentStep = this.normalizedStatus === 'completed'
					? 4
					: this.normalizedStatus === 'failed'
						? 4
						: 3
				const finalKey = this.normalizedStatus === 'failed' ? 'failure' : 'success'
				const steps = [
					{ key: 'initiated', label: this.i18n.txStageInitiated },
					{ key: 'signed', label: this.i18n.txStageSigned },
					{ key: 'broadcasted', label: this.i18n.txStageBroadcasted },
					{ key: 'confirming', label: this.i18n.txStageConfirming },
					{ key: finalKey, label: finalKey === 'failure' ? this.i18n.txStageFailure : this.i18n.txStageSuccess }
				]
				return steps.map((step, index) => ({
					...step,
					state: index < currentStep ? 'done' : index === currentStep ? 'current' : 'todo'
				}))
			}
		},
		onLoad(options) {
			this.txHash = options && options.txHash ? decodeURIComponent(options.txHash) : ''
			if (!this.txHash) {
				this.goTransactions()
				return
			}
			if (options && options.payload) {
				try {
					const payload = JSON.parse(decodeURIComponent(options.payload))
					if (payload && typeof payload === 'object') {
						this.txRecord = { ...this.txRecord, ...payload }
					}
				} catch (e) {}
			}
			const localRecord = getTxProgressByHash(this.txHash)
			if (localRecord) {
				this.txRecord = { ...this.txRecord, ...localRecord }
			}
			this.shouldPoll = !!localRecord || this.normalizedStatus === 'pending'
			this.refreshTxDetail(true)
			if (this.shouldPoll) {
				this.startPolling()
			}
		},
		onUnload() {
			this.stopPolling()
		},
		methods: {
			async refreshTxDetail(force = false) {
				if (!this.txHash || this.loading) return
				if (!force && !this.shouldPoll) return
				this.loading = true
				try {
					const detail = await api.getTxDetail(this.txHash, this.txRecord.chainId)
					if (detail) {
						this.txRecord = {
							...this.txRecord,
							...detail
						}
						syncTxProgress(this.txRecord)
						if (this.normalizedStatus !== 'pending') {
							this.shouldPoll = false
							invalidateTxRelatedCache(this.txRecord)
						}
					}
				} catch (e) {
				} finally {
					this.loading = false
				}
			},
			startPolling() {
				this.stopPolling()
				this.pollTimer = setInterval(async () => {
					await this.refreshTxDetail()
					if (!this.isPending) {
						this.stopPolling()
					}
				}, 5000)
			},
			stopPolling() {
				if (this.pollTimer) {
					clearInterval(this.pollTimer)
					this.pollTimer = null
				}
			},
			goBack() {
				uni.navigateBack({
					fail: () => {
						this.goTransactions()
					}
				})
			},
			goTransactions() {
				uni.navigateTo({
					url: '/pages/wallet/transactions'
				})
			},
			goWallet() {
				uni.switchTab({
					url: '/pages/wallet/wallet'
				})
			},
			copyText(value) {
				if (!value) return
				uni.setClipboardData({
					data: value,
					success: () => {
						uni.showToast({ title: this.i18n.copied, icon: 'none' })
					}
				})
			},
			openExplorer() {
				const url = buildExplorerUrl(this.txRecord.txHash, this.txRecord.chainId)
				if (!url) return
				// #ifdef APP-PLUS
				try {
					plus.runtime.openURL(url)
					return
				} catch (e) {}
				// #endif
				// #ifdef H5
				if (typeof window !== 'undefined' && window.open) {
					window.open(url, '_blank')
					return
				}
				// #endif
				uni.showToast({ title: this.i18n.openExplorerFailed, icon: 'none' })
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		height: 100vh;
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		box-sizing: border-box;
		background: var(--bg-color);
	}

	.container.theme-light {
		background: linear-gradient(180deg, #f0fcff 0%, #ddfaff 100%);
	}

	.header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 100rpx 32rpx 24rpx;
	}

	.back-btn,
	.header-spacer {
		width: 64rpx;
		height: 64rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.title {
		font-size: 36rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.scroll-area {
		flex: 1;
		height: 0;
	}

	.content {
		padding: 8rpx 32rpx 32rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.nft-card,
	.status-card,
	.timeline-card,
	.detail-card {
		background: var(--card-bg);
		border: var(--card-border);
		border-radius: 28rpx;
		padding: 32rpx;
		box-shadow: var(--card-shadow-sm);
	}

	.nft-card {
		display: flex;
		align-items: center;
		gap: 24rpx;
	}

	.nft-image {
		width: 120rpx;
		height: 120rpx;
		border-radius: 24rpx;
		background: var(--control-bg);
		flex-shrink: 0;
	}

	.nft-info {
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 10rpx;
	}

	.nft-label,
	.nft-meta {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.nft-name {
		font-size: 32rpx;
		font-weight: 700;
		color: var(--text-primary);
		word-break: break-word;
	}

	.status-caption {
		font-size: 24rpx;
		color: var(--text-secondary);
		display: block;
		margin-bottom: 12rpx;
	}

	.status-value {
		font-size: 44rpx;
		font-weight: 700;
	}

	.status-value.status-completed {
		color: var(--success-color);
	}

	.status-value.status-failed {
		color: var(--warning-text);
	}

	.status-value.status-pending {
		color: var(--pending-color);
	}

	.status-hint {
		font-size: 24rpx;
		line-height: 1.6;
		color: var(--text-secondary);
		display: block;
		margin-top: 12rpx;
	}

	.timeline-item {
		position: relative;
		padding-left: 40rpx;
		min-height: 72rpx;
	}

	.timeline-line {
		position: absolute;
		left: 11rpx;
		top: 24rpx;
		bottom: -24rpx;
		width: 2rpx;
		background: var(--border-color);
	}

	.timeline-dot {
		position: absolute;
		left: 0;
		top: 8rpx;
		width: 24rpx;
		height: 24rpx;
		border-radius: 50%;
		background: var(--border-color);
	}

	.timeline-dot.done {
		background: var(--success-color);
	}

	.timeline-dot.current {
		background: var(--pending-color);
		box-shadow: 0 0 0 8rpx rgba(245, 158, 11, 0.14);
	}

	.timeline-title {
		font-size: 28rpx;
		color: var(--text-secondary);
	}

	.timeline-title.done,
	.timeline-title.current {
		color: var(--text-primary);
		font-weight: 600;
	}

	.detail-row {
		display: flex;
		flex-direction: column;
		gap: 10rpx;
		padding-bottom: 24rpx;
		margin-bottom: 24rpx;
		border-bottom: 1px solid var(--border-color);
	}

	.detail-row:last-of-type {
		margin-bottom: 0;
	}

	.detail-label {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.detail-value {
		font-size: 28rpx;
		line-height: 1.6;
		color: var(--text-primary);
		word-break: break-all;
	}

	.detail-value--mono {
		font-family: Consolas, 'Courier New', monospace;
	}

	.detail-action {
		display: flex;
		flex-direction: column;
		gap: 10rpx;
	}

	.detail-link {
		font-size: 24rpx;
		color: var(--accent-color);
	}

	.btn-secondary,
	.btn-primary,
	.btn-ghost {
		width: 100%;
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0;

		&::after {
			border: none;
		}
	}

	.btn-secondary {
		height: 92rpx;
		background: transparent;
		border-radius: 22rpx;
		border: 1px solid var(--border-color);
	}

	.btn-secondary-text {
		font-size: 28rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.action-bar {
		padding: 16rpx 32rpx calc(40rpx + env(safe-area-inset-bottom));
		display: flex;
		flex-direction: column;
		gap: 16rpx;
		background: transparent;
	}

	.btn-primary {
		height: 104rpx;
		border-radius: 24rpx;
		background: var(--btn-confirm-bg);
	}

	.btn-primary-text {
		font-size: 30rpx;
		font-weight: 700;
		color: var(--btn-text-white);
	}

	.btn-ghost {
		height: 92rpx;
		border-radius: 22rpx;
		background: transparent;
		border: 1px solid var(--border-color);
	}

	.btn-ghost-text {
		font-size: 28rpx;
		color: var(--text-secondary);
	}
</style>
