<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<!-- Header（LightNew - Transaction History：顶栏标题居中、浅色返回切图） -->
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.transactions }}</text>
			<view class="header-right-spacer"></view>
		</view>

		<scroll-view
			scroll-y
			class="scroll-area"
			@refresherrefresh="onPullDownRefresh"
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			refresher-background="#00000000"
			:refresher-default-style="refresherIndicatorStyle"
			@scrolltolower="onScrollToLower"
			lower-threshold="80"
		>
			<view class="scroll-content">
				<view v-if="loading && walletTxGroups.length === 0" class="loading-state">
					<text>{{ i18n.loadingTransactions }}</text>
				</view>

				<view v-if="!loading && walletTxGroups.length === 0" class="empty-state">
					<text>{{ i18n.noTransactionsFound }}</text>
				</view>

				<!-- Grouped by Wallet -->
				<view class="wallet-group" v-for="(group, gIndex) in walletTxGroups" :key="gIndex">
					<view class="wallet-header" @click="toggleWallet(gIndex)">
						<view class="wallet-title-row">
							<view class="wallet-title-left">
								<text class="wallet-title">{{ group.name || i18n.mainWallet }}</text>
								<text class="wallet-addr">{{ group.address }}</text>
							</view>
							<text class="toggle-icon" :class="{ 'toggle-icon--lightnew': !isDarkMode }">{{ group.collapsed ? '▼' : '▲' }}</text>
						</view>
						<view class="wallet-meta wallet-meta--collapsed" v-if="group.collapsed">
							<text class="wallet-count">{{ (group.totalCount || 0) }} {{ i18n.hidden }}</text>
						</view>
					</view>
					<view class="tx-list" v-if="!group.collapsed">
						<view class="tx-item" v-for="(tx, tIndex) in group.transactions" :key="tIndex" @click="openTxProgress(tx)">
							<view v-if="isNFTTx(tx)" class="tx-item-nft-ribbon">
								<text class="tx-item-nft-ribbon-text">NFT</text>
							</view>
							<view class="tx-left">
								<view
									class="tx-icon-wrapper"
									:class="[
										getTxIconClass(tx, group.address),
										isReceived(tx, group.address) ? 'tx-dir-in' : 'tx-dir-out'
									]"
								>
									<text
										v-if="useLightTxDirGlyph(tx)"
										class="tx-dir-glyph"
										:class="isReceived(tx, group.address) ? 'tx-dir-glyph--in' : 'tx-dir-glyph--out'"
									>{{ isReceived(tx, group.address) ? '↙' : '↗' }}</text>
									<view v-else-if="isNFTTx(tx)" class="tx-icon-nft-wrap">
										<image
											class="tx-icon-img tx-icon-img--nft"
											:src="getTxIconImage(tx, group.address)"
											mode="aspectFill"
										></image>
									</view>
									<image
										v-else
										class="tx-icon-img"
										:src="getTxIconImage(tx, group.address)"
										mode="aspectFit"
									></image>
								</view>
								<view class="tx-info">
									<text class="tx-title">{{ getTxTitle(tx, group.address) }}</text>
									<text class="tx-time">{{ formatTime(tx.createTime) }}</text>
								</view>
							</view>
							<view class="tx-right">
								<text class="tx-amount" :class="getTxAmountClass(tx, group.address)" v-if="!isNFTTx(tx)">
									{{ getTxAmountPrefix(tx, group.address) }}{{ formatTxTokenAmount(tx) }}{{ txAmountSymbolSuffix(tx) }}
								</text>
								<text class="tx-amount" :class="getTxAmountClass(tx, group.address)" v-else>
									{{ getTxAmountPrefix(tx, group.address) }}{{ tx.amount || '1' }} NFT
								</text>
								<text class="tx-status" :class="txStatusClass(tx.status)">{{ txStatusLabel(tx.status) }}</text>
							</view>
						</view>
						
						<view v-if="group.transactions.length === 0" class="empty-group">
							<text>{{ i18n.noTransactionsInWallet }}</text>
						</view>

						<view class="load-more" v-if="group.totalCount > 0 || group.transactions.length > 0">
							<text v-if="group.loadingMore">{{ i18n.loading }}</text>
							<text v-else-if="group.hasMore">{{ i18n.swipeUpToLoadMore }}</text>
							<text v-else>{{ i18n.noMore }}</text>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api, convertIpfsUrl } from '@/utils/request'
	import { cacheGet, cacheSet } from '@/utils/cache'
	import { formatFixed4 } from '@/utils/format-money'
	import { mergePendingTxs } from '@/utils/tx-progress'
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				walletTxGroups: [],
				loading: false,
				refreshing: false,
				pageSize: 5
			}
		},
		computed: {
			...mapGetters(['i18n']),
			refresherIndicatorStyle() {
				return this.isDarkMode ? 'white' : 'black'
			}
		},
		onLoad() {
			this.fetchTransactions();
		},
		onShow() {
			this.fetchTransactions();
		},
		methods: {
			goBack() {
				uni.navigateBack();
			},
			openTxProgress(tx) {
				if (!tx || !tx.txHash) return
				const payload = encodeURIComponent(JSON.stringify(tx))
				uni.navigateTo({
					url: `/pages/wallet/transaction_progress?txHash=${encodeURIComponent(tx.txHash)}&payload=${payload}`
				})
			},
			txAmountSymbolSuffix(tx) {
				const raw = (tx.tokenSymbol || '').trim();
				const sym = raw.toUpperCase();
				if (!sym || sym === 'CFX') {
					return '';
				}
				return ` ${raw}`;
			},
			txStatusClass(status) {
				const s = String(status || '')
					.trim()
					.toLowerCase();
				if (s === 'success' || s === 'completed') return 'completed';
				if (s === 'failed' || s === 'failure') return 'failed';
				if (s === 'pending') return 'pending';
				return 'pending';
			},
			txStatusLabel(status) {
				const s = String(status || '')
					.trim()
					.toLowerCase();
				if (s === 'success') return this.i18n.txStatusSuccess;
				if (s === 'completed') return this.i18n.txStatusCompleted;
				if (s === 'failed' || s === 'failure') return this.i18n.txStatusFailed;
				if (s === 'pending') return this.i18n.txStatusPending;
				return this.i18n.unknown;
			},
			formatTime(timestamp) {
				if (!timestamp) return '';
				const date = new Date(timestamp);
				const month = date.toLocaleString('default', { month: 'short' });
				const day = date.getDate();
				const hours = date.getHours().toString().padStart(2, '0');
				const minutes = date.getMinutes().toString().padStart(2, '0');
				return `${month} ${day}, ${hours}:${minutes}`;
			},
			isReceived(tx, currentAddress) {
				return tx.toAddress && tx.toAddress.toLowerCase() === currentAddress.toLowerCase();
			},
			useLightTxDirGlyph(tx) {
				return !this.isDarkMode && !this.isNFTTx(tx);
			},
			isNFTTx(tx) {
				return tx.tokenSymbol && tx.tokenSymbol.startsWith('NFT:');
			},
			getTokenLogoBySymbol(symbol) {
				const sym = (symbol || '').toUpperCase();
				const map = {
					ETH: this.$staticUrl('/static/icons/icon_eth.png'),
					BTC: this.$staticUrl('/static/icons/icon_btc.png'),
					CFX: this.$staticUrl('/static/icons/logo.png')
				};
				return map[sym] || this.$staticUrl('/static/icons/placeholder_coin.png');
			},
			getTxTitle(tx, currentAddress) {
				const isNFT = this.isNFTTx(tx);
				if (isNFT) {
					const name = (tx.tokenSymbol || '').substring(5);
					return this.isReceived(tx, currentAddress)
						? `${this.i18n.received} ${name}`
						: `${this.i18n.sent} ${name}`;
				}
				const sym = (tx.tokenSymbol || '').trim().toUpperCase();
				if (!sym || sym === 'CFX') {
					return this.isReceived(tx, currentAddress) ? this.i18n.received : this.i18n.sent;
				}
				return this.isReceived(tx, currentAddress)
					? `${this.i18n.received} ${tx.tokenSymbol}`
					: `${this.i18n.sent} ${tx.tokenSymbol}`;
			},
			getTxIconImage(tx, currentAddress) {
				if (this.isNFTTx(tx)) {
					return convertIpfsUrl(tx.nftImage) || this.$staticUrl('/static/icons/placeholder_nft.png');
				}
				return tx.tokenIcon || this.getTokenLogoBySymbol(tx.tokenSymbol || 'CFX');
			},
			getTxIconClass(tx, currentAddress) {
				return this.isNFTTx(tx) ? 'icon-nft' : 'icon-token';
			},
			getTxAmountPrefix(tx, currentAddress) {
				return this.isReceived(tx, currentAddress) ? '+' : '-';
			},
			/** 代币金额四位小数；NFT 仍为原样 */
			formatTxTokenAmount(tx) {
				if (this.isNFTTx(tx)) return tx.amount || '1'
				return formatFixed4(tx.amount != null ? tx.amount : 0)
			},
			getTxAmountClass(tx, currentAddress) {
				return this.isReceived(tx, currentAddress) ? 'amount-positive' : 'amount-negative';
			},
			async onPullDownRefresh() {
				this.refreshing = true;
				await this.fetchTransactions();
				this.refreshing = false;
			},
			onScrollToLower() {
				// 到底时，只对“最后一个展开且有更多”的钱包组翻页，避免多钱包同时狂刷请求
				for (let i = this.walletTxGroups.length - 1; i >= 0; i--) {
					const g = this.walletTxGroups[i];
					if (!g || g.collapsed || !g.hasMore || g.loadingMore) continue;
					this.loadMoreForGroup(i);
					break;
				}
			},
			toggleWallet(index) {
				const g = this.walletTxGroups[index];
				if (!g) return;
				this.$set(g, 'collapsed', !g.collapsed);
			},
			async loadMoreForGroup(index) {
				const g = this.walletTxGroups[index];
				if (!g || g.loadingMore || !g.hasMore) return;
				this.$set(g, 'loadingMore', true);
				try {
					const chainId = g.chainId || '1029';
					const address = g.address;
					const skip = g.skip || 0;
					const res = await api.getTxHistory(address, chainId, { limit: this.pageSize, skip });
					const list = Array.isArray(res) ? res : [];

					// 去重（按 txHash）
					const exist = new Set((g.transactions || []).map(t => t && t.txHash).filter(Boolean));
					const appended = [];
					for (const tx of list) {
						if (!tx || !tx.txHash || exist.has(tx.txHash)) continue;
						exist.add(tx.txHash);
						appended.push(tx);
					}

					const merged = (g.transactions || []).concat(appended);
					const nextTxs = mergePendingTxs(merged, address);
					this.$set(g, 'transactions', nextTxs);
					this.$set(g, 'skip', skip + this.pageSize);
					this.$set(g, 'hasMore', list.length >= this.pageSize);
					this.$set(g, 'totalCount', nextTxs.length);

					// 更新缓存（10分钟过期，按钱包维度）
					const cacheKey = `cache:txs:v2:${chainId}:${address}`;
					cacheSet(cacheKey, {
						transactions: nextTxs,
						skip: skip + this.pageSize,
						hasMore: list.length >= this.pageSize
					});
				} catch (e) {
					// ignore
				} finally {
					this.$set(g, 'loadingMore', false);
				}
			},
			async fetchTransactions() {
				if (this.loading) return;
				this.loading = true;
				try {
					const walletListRes = await api.getWalletList();
					let wallets = [];
					if (walletListRes && walletListRes.length > 0) {
						wallets = walletListRes;
					} else {
						let currentAddress = uni.getStorageSync('current_wallet_address');
						if (!currentAddress) {
							currentAddress = 'cfx:aamgvyzht7h1zxdghb9ee9w26wrz8rd3gj837392dp';
						}
						wallets = [{ address: currentAddress, chainId: '1029', name: 'My Wallet' }];
					}

					const groupedTxs = [];
					for (const wallet of wallets) {
						try {
							const chainId = wallet.chainId || '1029';
							const tokenIconMap = {};
							try {
								const tokens = await api.getTokens(chainId, wallet.address);
								(tokens || []).forEach(t => {
									const sym = (t.symbol || '').toUpperCase();
									if (sym && t.icon) tokenIconMap[sym] = t.icon;
								});
							} catch (e) {
								// ignore token icon failures; fallback to static map
							}

							const cacheKey = `cache:txs:v2:${chainId}:${wallet.address}`;
							const cached = cacheGet(cacheKey);

							let txs = [];
							let skip = 0;
							let hasMore = true;

							if (cached && Array.isArray(cached.transactions)) {
								txs = cached.transactions;
								skip = typeof cached.skip === 'number' ? cached.skip : txs.length;
								hasMore = cached.hasMore !== false;
							} else {
								const res = await api.getTxHistory(wallet.address, chainId, { limit: this.pageSize, skip: 0 });
								txs = Array.isArray(res) ? res : [];
								skip = this.pageSize;
								hasMore = txs.length >= this.pageSize;
								cacheSet(cacheKey, { transactions: txs, skip, hasMore });
							}

							if (txs.length > 0) {
								txs = txs.map(tx => {
									const sym = (tx.tokenSymbol || '').toUpperCase();
									if (sym && !this.isNFTTx(tx)) {
										return { ...tx, tokenIcon: tokenIconMap[sym] };
									}
									return tx;
								});
							}
							txs = mergePendingTxs(txs, wallet.address)
							groupedTxs.push({
								address: wallet.address,
								chainId: chainId,
								name: wallet.name,
								collapsed: false,
								transactions: txs,
								totalCount: txs.length,
								skip,
								hasMore,
								loadingMore: false
							});
						} catch (e) {
							console.error(`Failed to fetch TXs for ${wallet.address}`, e);
							groupedTxs.push({
								address: wallet.address,
								chainId: wallet.chainId || '1029',
								name: wallet.name,
								collapsed: false,
								transactions: [],
								totalCount: 0,
								skip: 0,
								hasMore: false,
								loadingMore: false
							});
						}
					}
					this.walletTxGroups = groupedTxs;
				} catch (err) {
					console.error('Failed to fetch wallets or TXs:', err);
				} finally {
					this.loading = false;
				}
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
	}

	/* LightNew - Transaction History 页面底（V2.pen PuRwT） */
	.container.theme-light {
		background: linear-gradient(180deg, #f0fcff 0%, #ddfaff 100%);
	}

	.header {
		flex-shrink: 0;
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding-top: 100rpx;
		padding-bottom: 32rpx;
		padding-left: 32rpx;
		padding-right: 32rpx;
		background-color: var(--bg-color);
		z-index: 10;
	}

	.theme-light .header {
		background: transparent;
		padding-left: 48rpx;
		padding-right: 48rpx;
		padding-bottom: 24rpx;
	}

	.back-btn {
		position: relative;
		z-index: 2;
		width: 72rpx;
		height: 72rpx;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.theme-light .back-btn {
		width: 64rpx;
		height: 64rpx;
		justify-content: flex-start;
	}

	.title {
		position: absolute;
		left: 50%;
		transform: translateX(-50%);
		width: 62%;
		text-align: center;
		font-size: 36rpx;
		font-weight: bold;
		color: var(--text-primary);
		pointer-events: none;
	}

	.theme-light .title {
		font-size: 40rpx;
		font-weight: 700;
		color: #0f172a;
	}

	.header-right-spacer {
		width: 72rpx;
		height: 72rpx;
		flex-shrink: 0;
	}

	.theme-light .header-right-spacer {
		width: 64rpx;
		height: 64rpx;
	}

	.scroll-area {
		flex: 1;
		height: 0;
		min-height: 0;
	}

	.scroll-content {
		padding: 24rpx 32rpx 80rpx;
		box-sizing: border-box;
	}

	.theme-light .scroll-content {
		padding-left: 48rpx;
		padding-right: 48rpx;
		padding-top: 8rpx;
	}

	.loading-state, .empty-state {
		text-align: center;
		padding: 100rpx 0;
		color: var(--text-secondary);
		font-size: 28rpx;
	}

	.wallet-group {
		margin-bottom: 40rpx;
		border: none;
		box-shadow:none;
	}

	.theme-light .wallet-group {
		margin-bottom: 32rpx;
	}

	.wallet-header {
		display: flex;
		flex-direction: column;
		align-items: stretch;
		gap: 0;
		margin-bottom: 24rpx;
		padding: 0 16rpx;
	}

	.theme-light .wallet-header {
		padding: 0;
		margin-bottom: 24rpx;
		gap: 0;
	}

	.wallet-title-row {
		display: flex;
		align-items: center;
		gap: 16rpx;
		justify-content: space-between;
		width: 100%;
		flex-wrap: nowrap;
	}

	.wallet-title-left {
		display: flex;
		flex-direction: row;
		flex-wrap: nowrap;
		align-items: center;
		gap: 16rpx;
		min-width: 0;
		flex: 1;
	}

	.wallet-title {
		font-size: 32rpx;
		font-weight: 600;
		color: var(--text-primary);
		flex: 0 1 auto;
		max-width: 220rpx;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.theme-light .wallet-title {
		font-size: 28rpx;
		font-weight: 700;
		color: #64748b;
	}

	.wallet-addr {
		font-size: 24rpx;
		color: var(--text-secondary);
		flex: 1;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.theme-light .wallet-addr {
		color: #94a3b8;
		font-weight: 400;
	}

	.wallet-meta--collapsed {
		margin-top: 12rpx;
	}

	.wallet-meta--collapsed .wallet-count {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.theme-light .wallet-meta--collapsed .wallet-count {
		color: #94a3b8;
	}

	.toggle-icon {
		font-size: 24rpx;
		color: var(--text-tertiary);
		flex-shrink: 0;
	}

	.theme-light .toggle-icon--lightnew {
		color: #c7c7cc;
		font-size: 28rpx;
		line-height: 1;
		padding: 8rpx 0 8rpx 12rpx;
	}

	.tx-list {
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.tx-item {
		position: relative;
		overflow: hidden;
		display: flex;
		justify-content: space-between;
		align-items: center;
		background-color: var(--card-bg);
		border-radius: 26rpx;
		padding: 32rpx;
		border: none;
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.theme-light .tx-item {
		background: rgba(255, 255, 255, 0.8);
		border: none;
		border-radius: 40rpx;
		box-shadow: 0 8rpx 20rpx rgba(0, 0, 0, 0.051);
		backdrop-filter: blur(20rpx);
		-webkit-backdrop-filter: blur(20rpx);
	}

	.tx-left {
		display: flex;
		align-items: center;
		gap: 24rpx;
	}

	.tx-icon-wrapper {
		width: 80rpx;
		height: 80rpx;
		border-radius: 40rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		background-color: var(--control-bg);
		overflow: hidden;
		
		&.icon-token {
			border-radius: 40rpx;

			.tx-icon-img {
				width: 48rpx;
				height: 48rpx;
			}
		}
		
		&.icon-nft {
			border-radius: 16rpx;
			overflow: hidden;
		}
	}

	.tx-icon-nft-wrap {
		position: relative;
		width: 100%;
		height: 100%;
		border-radius: 16rpx;
		overflow: hidden;
	}

	.tx-icon-img--nft {
		width: 100%;
		height: 100%;
		display: block;
	}

	/* 整条交易卡片左上角斜角丝带（底色与页面大背景一致） */
	.tx-item-nft-ribbon {
		position: absolute;
		top: -10px;
		left: -8px;
		z-index: 2;
		width: 120rpx;
		height: 120rpx;
		overflow: hidden;
		pointer-events: none;
	}

	.tx-item-nft-ribbon-text {
		position: absolute;
		top: 22rpx;
		left: -36rpx;
		width: 148rpx;
		padding: 10rpx 0;
		font-size: 24rpx;
		font-weight: 700;
		line-height: 1;
		text-align: center;
		transform: rotate(-45deg);
		-webkit-transform: rotate(-45deg);
		transform-origin: center center;
	}

	.theme-light .tx-item-nft-ribbon-text {
		background: linear-gradient(180deg, #f0fcff 0%, #ddfaff 100%);
		color: #64748b;
	}

	.theme-dark .tx-item-nft-ribbon-text {
		background: linear-gradient(168deg, #101d36 0%, #1e365c 100%);
		color: #94a3b8;
	}

	.theme-light .tx-icon-wrapper.tx-dir-in {
		background: #e8f5e9;
	}

	.theme-light .tx-icon-wrapper.tx-dir-out {
		background: #ffebee;
	}

	.tx-dir-glyph {
		font-size: 40rpx;
		line-height: 1;
		font-weight: 600;
	}

	.tx-dir-glyph--in {
		color: #4caf50;
	}

	.tx-dir-glyph--out {
		color: #f44336;
	}

	.tx-icon-img {
		width: 100%;
		height: 100%;
	}

	.tx-info {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
	}

	.tx-title {
		font-size: 32rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.theme-light .tx-title {
		font-size: 32rpx;
		font-weight: 700;
		color: #0f172a;
	}

	.tx-time {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.theme-light .tx-time {
		font-size: 24rpx;
		color: #64748b;
	}

	.tx-right {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		gap: 8rpx;
	}

	.tx-amount {
		font-size: 32rpx;
		font-weight: 600;
		
		&.amount-positive {
			color: var(--success-color);
		}
		
		&.amount-negative {
			color: var(--text-primary);
		}
	}

	.theme-light .tx-amount {
		font-size: 32rpx;
		font-weight: 700;

		&.amount-positive {
			color: #4caf50;
		}

		&.amount-negative {
			color: #0f172a;
		}
	}

	.tx-status {
		font-size: 24rpx;
		color: var(--text-secondary);
		
		&.completed {
			color: var(--success-color);
		}
		
		&.pending {
			color: var(--pending-color);
		}
		
		&.failed {
			color: var(--warning-text);
		}
	}

	.theme-light .tx-status {
		font-size: 24rpx;
		color: #64748b;

		&.completed {
			color: #64748b;
		}

		&.pending {
			color: #f59e0b;
		}

		&.failed {
			color: #e85b77;
		}
	}
	
	.empty-group {
		padding: 32rpx;
		text-align: center;
		color: var(--text-secondary);
		background-color: var(--card-bg);
		border-radius: 24rpx;
		font-size: 28rpx;
	}

	.theme-light .empty-group {
		background: rgba(255, 255, 255, 0.75);
		border-radius: 40rpx;
		color: #64748b;
	}

	.load-more {
		padding: 24rpx 0 8rpx;
		text-align: center;
		color: var(--text-secondary);
		font-size: 24rpx;
	}

	.theme-light .load-more {
		color: #94a3b8;
		font-size: 24rpx;
	}

	.theme-light .loading-state,
	.theme-light .empty-state {
		color: #64748b;
	}
</style>
