<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ asset.name || asset.symbol || i18n.assetDetailTitle }}</text>
			<view class="header-spacer"></view>
		</view>

		<scroll-view
			scroll-y
			class="scroll-area"
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			refresher-background="#00000000"
			:refresher-default-style="refresherIndicatorStyle"
			@refresherrefresh="onPullDownRefresh"
			@scrolltolower="loadMore"
			lower-threshold="80"
		>
			<view class="content">
				<view class="asset-card">
					<view class="asset-head">
						<image class="asset-icon" :src="asset.icon || defaultIcon" mode="aspectFill"></image>
						<view class="asset-head-text">
							<text class="asset-name">{{ asset.name || asset.symbol || '--' }}</text>
							<text class="asset-wallet">{{ asset.walletName || formatAddress(asset.walletAddress) }}</text>
						</view>
					</view>
					<text class="asset-balance">{{ formatAmount(asset.amount) }} {{ asset.symbol || '' }}</text>
					<text class="asset-fiat">{{ asset.fiat || '--' }}</text>
				</view>

				<view class="filter-row">
					<view
						v-for="item in filters"
						:key="item.key"
						class="filter-chip"
						:class="{ active: currentFilter === item.key }"
						@click="currentFilter = item.key"
					>
						<text class="filter-chip-text">{{ item.label }}</text>
					</view>
				</view>

				<view class="section-title">{{ i18n.assetHistoryTitle }}</view>

				<view v-if="loading && filteredTransactions.length === 0" class="loading-state">
					<text>{{ i18n.loadingTransactions }}</text>
				</view>

				<view v-else-if="filteredTransactions.length === 0" class="empty-state">
					<text>{{ i18n.noAssetTransactions }}</text>
				</view>

				<view v-else class="tx-list">
					<view class="tx-item" v-for="(tx, index) in filteredTransactions" :key="tx.txHash || index" @click="openTxProgress(tx)">
						<view class="tx-left">
							<view class="tx-icon-wrapper" :class="isReceived(tx) ? 'tx-dir-in' : 'tx-dir-out'">
								<text class="tx-dir-glyph" :class="isReceived(tx) ? 'tx-dir-glyph--in' : 'tx-dir-glyph--out'">
									{{ isReceived(tx) ? '↙' : '↗' }}
								</text>
							</view>
							<view class="tx-info">
								<text class="tx-title">{{ getTxTitle(tx) }}</text>
								<text class="tx-time">{{ formatTime(tx.createTime) }}</text>
							</view>
						</view>
						<view class="tx-right">
							<text class="tx-amount" :class="isReceived(tx) ? 'amount-positive' : 'amount-negative'">
								{{ isReceived(tx) ? '+' : '-' }}{{ formatAmount(tx.amount) }}{{ txAmountSymbolSuffix(tx) }}
							</text>
							<text class="tx-status" :class="txStatusClass(tx.status)">{{ txStatusLabel(tx.status) }}</text>
						</view>
					</view>
				</view>

				<view v-if="filteredTransactions.length > 0" class="load-more">
					<text v-if="loadingMore">{{ i18n.loading }}</text>
					<text v-else-if="hasMoreRaw">{{ i18n.swipeUpToLoadMore }}</text>
					<text v-else>{{ i18n.noMore }}</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { formatFixed4 } from '@/utils/format-money'
	import { mergePendingTxs } from '@/utils/tx-progress'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				asset: {
					walletAddress: '',
					chainId: '1029',
					walletName: '',
					name: '',
					symbol: 'CFX',
					type: 'native',
					contract: '',
					amount: 0,
					fiat: '',
					icon: ''
				},
				allTransactions: [],
				currentFilter: 'all',
				loading: false,
				loadingMore: false,
				refreshing: false,
				rawSkip: 0,
				hasMoreRaw: true,
				rawPageSize: 20
			}
		},
		computed: {
			...mapGetters(['i18n', 'themeClass']),
			refresherIndicatorStyle() {
				return this.isDarkMode ? 'white' : 'black'
			},
			defaultIcon() {
				return this.$staticUrl('/static/icons/placeholder_coin.png')
			},
			filters() {
				return [
					{ key: 'all', label: this.i18n.assetFilterAll },
					{ key: 'in', label: this.i18n.assetFilterIn },
					{ key: 'out', label: this.i18n.assetFilterOut },
					{ key: 'failed', label: this.i18n.assetFilterFailed },
					{ key: 'pending', label: this.i18n.assetFilterPending }
				]
			},
			filteredTransactions() {
				return this.allTransactions.filter((tx) => {
					if (!this.matchesAsset(tx)) return false
					if (this.currentFilter === 'in') return this.isReceived(tx)
					if (this.currentFilter === 'out') return !this.isReceived(tx)
					if (this.currentFilter === 'failed') return this.txStatusClass(tx.status) === 'failed'
					if (this.currentFilter === 'pending') return this.txStatusClass(tx.status) === 'pending'
					return true
				})
			}
		},
		onLoad(options) {
			if (options && options.payload) {
				try {
					const payload = JSON.parse(decodeURIComponent(options.payload))
					if (payload && typeof payload === 'object') {
						this.asset = { ...this.asset, ...payload }
					}
				} catch (e) {}
			}
			this.fetchTransactions(true)
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			formatAmount(value) {
				return formatFixed4(value != null ? value : 0)
			},
			formatAddress(addr) {
				const value = String(addr || '')
				if (value.length <= 18) return value
				return `${value.substring(0, 10)}...${value.substring(value.length - 6)}`
			},
			formatTime(timestamp) {
				if (!timestamp) return ''
				const date = new Date(timestamp)
				const month = date.toLocaleString('default', { month: 'short' })
				const day = date.getDate()
				const hours = String(date.getHours()).padStart(2, '0')
				const minutes = String(date.getMinutes()).padStart(2, '0')
				return `${month} ${day}, ${hours}:${minutes}`
			},
			normalizeSymbol(value) {
				return String(value || '').trim().toUpperCase()
			},
			isReceived(tx) {
				const walletAddress = String(this.asset.walletAddress || '').toLowerCase()
				return walletAddress && String(tx.toAddress || '').toLowerCase() === walletAddress
			},
			txStatusClass(status) {
				const s = String(status || '').trim().toLowerCase()
				if (s === 'success' || s === 'completed') return 'completed'
				if (s === 'failed' || s === 'failure') return 'failed'
				return 'pending'
			},
			txStatusLabel(status) {
				const s = this.txStatusClass(status)
				if (s === 'completed') return this.i18n.txStatusCompleted
				if (s === 'failed') return this.i18n.txStatusFailed
				return this.i18n.txStatusPending
			},
			txAmountSymbolSuffix(tx) {
				const raw = (tx.tokenSymbol || '').trim()
				const sym = raw.toUpperCase()
				if (!sym || sym === 'CFX') return this.normalizeSymbol(this.asset.symbol) === 'CFX' ? '' : ` ${this.asset.symbol || ''}`
				return ` ${raw}`
			},
			getTxTitle(tx) {
				const action = this.isReceived(tx) ? this.i18n.received : this.i18n.sent
				const symbol = this.asset.symbol || tx.tokenSymbol || ''
				return symbol ? `${action} ${symbol}` : action
			},
			matchesAsset(tx) {
				if (!tx || !tx.txHash) return false
				const txSymbol = this.normalizeSymbol(tx.tokenSymbol || 'CFX')
				const assetSymbol = this.normalizeSymbol(this.asset.symbol || 'CFX')
				if (String(tx.tokenSymbol || '').startsWith('NFT:')) return false
				if (this.asset.type === 'native' || assetSymbol === 'CFX') {
					return !txSymbol || txSymbol === 'CFX'
				}
				return txSymbol === assetSymbol
			},
			openTxProgress(tx) {
				if (!tx || !tx.txHash) return
				const payload = encodeURIComponent(JSON.stringify(tx))
				uni.navigateTo({
					url: `/pages/wallet/transaction_progress?txHash=${encodeURIComponent(tx.txHash)}&payload=${payload}`
				})
			},
			mergeAndSort(list, reset) {
				const mergedBase = reset ? [] : this.allTransactions.slice()
				const exists = new Set(mergedBase.map(item => item && item.txHash).filter(Boolean))
				for (const tx of list) {
					if (!tx || !tx.txHash || exists.has(tx.txHash)) continue
					exists.add(tx.txHash)
					mergedBase.push(tx)
				}
				const merged = mergePendingTxs(mergedBase, this.asset.walletAddress)
				merged.sort((a, b) => (b.createTime || 0) - (a.createTime || 0))
				this.allTransactions = merged
			},
			async fetchTransactions(reset = false) {
				if (this.loading) return
				this.loading = true
				if (reset) {
					this.rawSkip = 0
					this.hasMoreRaw = true
				}
				try {
					let batches = 0
					let gathered = reset ? [] : this.allTransactions.slice()
					let matchedCount = reset ? 0 : this.filteredTransactions.length
					while (this.hasMoreRaw && batches < 3 && matchedCount < 8) {
						const res = await api.getTxHistory(this.asset.walletAddress, this.asset.chainId, {
							limit: this.rawPageSize,
							skip: this.rawSkip
						})
						const list = Array.isArray(res) ? res : []
						this.rawSkip += this.rawPageSize
						this.hasMoreRaw = list.length >= this.rawPageSize
						gathered = reset && batches === 0 ? list.slice() : gathered.concat(list)
						matchedCount = gathered.filter(tx => this.matchesAsset(tx)).length
						batches += 1
						if (list.length < this.rawPageSize) break
					}
					this.mergeAndSort(gathered, reset)
				} catch (e) {
					console.error('fetch asset transactions failed', e)
				} finally {
					this.loading = false
					this.loadingMore = false
				}
			},
			async onPullDownRefresh() {
				this.refreshing = true
				try {
					await this.fetchTransactions(true)
				} finally {
					this.refreshing = false
				}
			},
			async loadMore() {
				if (this.loading || this.loadingMore || !this.hasMoreRaw) return
				this.loadingMore = true
				await this.fetchTransactions(false)
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		display: flex;
		flex-direction: column;
		box-sizing: border-box;
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
		padding: 8rpx 32rpx 40rpx;
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.asset-card,
	.tx-item {
		background: var(--card-bg);
		border: var(--card-border);
		border-radius: 28rpx;
		box-shadow: var(--card-shadow-sm);
	}

	.asset-card {
		padding: 32rpx;
		display: flex;
		flex-direction: column;
		gap: 18rpx;
	}

	.asset-head {
		display: flex;
		align-items: center;
		gap: 20rpx;
	}

	.asset-icon {
		width: 88rpx;
		height: 88rpx;
		border-radius: 50%;
		background: var(--control-bg);
		flex-shrink: 0;
	}

	.asset-head-text {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
		min-width: 0;
	}

	.asset-name {
		font-size: 32rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.asset-wallet,
	.asset-fiat {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.asset-balance {
		font-size: 52rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.filter-row {
		display: flex;
		flex-wrap: wrap;
		gap: 16rpx;
	}

	.filter-chip {
		padding: 14rpx 24rpx;
		border-radius: 999rpx;
		background: var(--control-bg);
		border: 1px solid transparent;
	}

	.filter-chip.active {
		border-color: var(--btn-confirm-bg);
		background: rgba(88, 101, 242, 0.08);
	}

	.filter-chip-text {
		font-size: 24rpx;
		color: var(--text-primary);
	}

	.section-title {
		font-size: 28rpx;
		font-weight: 700;
		color: var(--text-primary);
	}

	.loading-state,
	.empty-state,
	.load-more {
		padding: 32rpx 0;
		text-align: center;
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.tx-list {
		display: flex;
		flex-direction: column;
		gap: 20rpx;
	}

	.tx-item {
		padding: 28rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.tx-left {
		display: flex;
		align-items: center;
		gap: 20rpx;
		min-width: 0;
	}

	.tx-icon-wrapper {
		width: 72rpx;
		height: 72rpx;
		border-radius: 36rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
	}

	.tx-dir-in {
		background: #e8f5e9;
	}

	.tx-dir-out {
		background: #ffebee;
	}

	.tx-dir-glyph {
		font-size: 36rpx;
		line-height: 1;
		font-weight: 600;
	}

	.tx-dir-glyph--in {
		color: #4caf50;
	}

	.tx-dir-glyph--out {
		color: #f44336;
	}

	.tx-info {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
		min-width: 0;
	}

	.tx-title {
		font-size: 30rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.tx-time {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.tx-right {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 8rpx;
	}

	.tx-amount {
		font-size: 30rpx;
		font-weight: 700;
	}

	.amount-positive {
		color: var(--success-color);
	}

	.amount-negative {
		color: var(--text-primary);
	}

	.tx-status {
		font-size: 24rpx;
	}

	.tx-status.completed {
		color: var(--success-color);
	}

	.tx-status.pending {
		color: var(--pending-color);
	}

	.tx-status.failed {
		color: var(--warning-text);
	}
</style>
