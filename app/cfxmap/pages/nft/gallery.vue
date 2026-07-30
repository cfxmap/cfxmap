<template>
	<view class="page-shell" :class="themeClass">
		<global-loading />
		<view class="top-fixed">
			<!-- Segmented Control and Transaction History -->
			<view class="header-group">
				<view class="segmented-control">
					<view class="segment" @click="goWallet">
						<text class="segment-content">{{ i18n.tokens }}</text>
					</view>
					<view class="segment active">
						<text class="segment-content">{{ i18n.nftTitle }}</text>
					</view>
				</view>
				<view class="tx-entry" :class="{ 'tx-entry-light': !isDarkMode }" @click="goTransactions">
					<image
						class="tx-list-icon"
						:src="$staticUrl('/static/wallet-actions/tx-list-icon.png')"
						mode="aspectFit"
					/>
				</view>
			</view>
		</view>

		<scroll-view
			scroll-y
			class="list-scroll"
			:refresher-enabled="true"
			:refresher-triggered="refreshing"
			refresher-background="#00000000"
			:refresher-default-style="refresherIndicatorStyle"
			@refresherrefresh="onRefresherRefresh"
		>
			<view class="list-scroll-inner">
		<view v-if="walletNfts.length === 0 && !loading" class="empty-state">
			<text>{{ i18n.noNftsFound || 'No NFTs found.' }}</text>
		</view>

		<!-- Loading State -->
		<view v-if="loading && walletNfts.length === 0" class="loading-state">
			<view class="loader"></view>
			<text class="loading-text">{{ i18n.loading || 'Loading...' }}</text>
		</view>

		<view class="wallet-group" v-for="(group, gIndex) in walletNfts" :key="gIndex">
			<!-- Wallet Card Header (Collapsible) -->
			<view class="wallet-card-header" @click="toggleWallet(gIndex)">
				<view class="wallet-info-container">
					<view class="wallet-title-row">
						<text class="wallet-title">{{ group.name || 'Main Wallet' }}</text>
						<text class="toggle-icon">{{ group.collapsed ? '▼' : '▲' }}</text>
					</view>
					<view class="wallet-addr-row">
						<text class="wallet-addr">{{ formatWalletAddress(group.address) }}</text>
						<view class="wallet-icons">
							<image class="action-icon-img" :src="$staticUrl('/static/icons/icon_copy.png')" mode="aspectFit" @click.stop="copyAddress(group.address)"></image>
							<image class="action-icon-img" :src="$staticUrl('/static/icons/icon_qrcode.png')" mode="aspectFit" @click.stop="showQRCode(group.address)"></image>
						</view>
					</view>
				</view>
			</view>

			<!-- Collapsed State Info -->
			<view class="wallet-collapsed-info" v-if="group.collapsed">
				<text>{{ group.nfts.length }} NFTs Hidden</text>
			</view>

			<!-- NFT：CSS Grid 窄屏 2 列，宽屏 3/4 列（与双折、三折展开宽度适配） -->
			<view class="nft-grid" v-else>
				<view
					class="nft-card"
					v-for="(item, index) in visibleNfts(group)"
					:key="nftItemKey(group, item, index)"
					@click="goDetail(item)"
				>
					<view class="image-wrapper">
						<image class="nft-img" :src="convertIpfsUrl(item.image)" mode="aspectFill"></image>
						<view class="badge-3d" v-if="item.is3D">3D</view>
					</view>
					<view class="nft-info">
						<text class="nft-name">{{item.name}}</text>
						<text class="nft-series">{{item.series}}</text>
					</view>
				</view>
			</view>
		</view>
			</view>
		</scroll-view>
		<bottom-nav />
	</view>
</template>

<script>
	import { mapGetters } from 'vuex'
	import { api, convertIpfsUrl } from '@/utils/request'
	import { cacheGet, cacheSet } from '@/utils/cache'
	import BottomNav from '@/components/bottom-nav.vue'

	export default {
		components: {
			BottomNav
		},
		data() {
			return {
				walletNfts: [],
				loading: false,
				refreshing: false,
				_nftRevealTimerIds: []
			}
		},
		computed: {
			...mapGetters(['i18n']),
			refresherIndicatorStyle() {
				return this.isDarkMode ? 'white' : 'black'
			}
		},
		onShow() {
			uni.hideTabBar()
			this.fetchNFTs();
		},
		beforeDestroy() {
			this.clearNftRevealTimers()
		},
		methods: {
			async onRefresherRefresh() {
				this.refreshing = true
				try {
					await this.fetchNFTs(true, true)
				} finally {
					this.refreshing = false
				}
			},
			convertIpfsUrl,
			clearNftRevealTimers() {
				(this._nftRevealTimerIds || []).forEach((id) => clearTimeout(id))
				this._nftRevealTimerIds = []
			},
			visibleNfts(group) {
				const list = group && group.nfts ? group.nfts : []
				const cap = typeof group.visibleNftCount === 'number' ? group.visibleNftCount : list.length
				return list.slice(0, Math.min(cap, list.length))
			},
			nftItemKey(group, item, index) {
				const id = item && (item.id != null ? item.id : '')
				const c = item && item.contract != null ? item.contract : ''
				const t = item && item.tokenId != null ? item.tokenId : ''
				const addr = group && group.address ? group.address : ''
				return `${addr}:${id}:${c}:${t}:${index}`
			},
			scheduleRevealMoreNfts() {
				this.clearNftRevealTimers()
				const step = 8
				const delayMs = 32
				const tick = () => {
					let pending = false
					this.walletNfts.forEach((g, gIndex) => {
						const total = (g.nfts && g.nfts.length) || 0
						if (total <= 4) return
						const cur = typeof g.visibleNftCount === 'number' ? g.visibleNftCount : total
						if (cur >= total) return
						pending = true
						const next = Math.min(total, cur + step)
						this.$set(this.walletNfts[gIndex], 'visibleNftCount', next)
					})
					if (pending) {
						const tid = setTimeout(tick, delayMs)
						this._nftRevealTimerIds.push(tid)
					}
				}
				const tid0 = setTimeout(tick, delayMs)
				this._nftRevealTimerIds.push(tid0)
			},
			formatWalletAddress(addr) {
				if (!addr) return '';
				/* 与 tokens（wallet）页一致 */
				const maxFull = 44;
				if (addr.length <= maxFull) return addr;
				const head = 26;
				const tail = 12;
				return addr.substring(0, head) + '...' + addr.substring(addr.length - tail);
			},
			copyAddress(address) {
				uni.setClipboardData({
					data: address,
					success: () => {
						uni.showToast({ title: this.i18n.addressCopied || 'Address Copied', icon: 'none' });
					}
				});
			},
			showQRCode(address) {
				uni.navigateTo({
					url: `/pages/transfer/receive?address=${encodeURIComponent(address)}`
				});
			},
			toggleWallet(index) {
				const g = this.walletNfts[index]
				const wasCollapsed = g.collapsed
				this.$set(this.walletNfts[index], 'collapsed', !wasCollapsed)
				if (wasCollapsed) {
					const ng = this.walletNfts[index]
					const total = (ng.nfts && ng.nfts.length) || 0
					const cur = typeof ng.visibleNftCount === 'number' ? ng.visibleNftCount : total
					if (total > 4 && cur < total) {
						this.$nextTick(() => this.scheduleRevealMoreNfts())
					}
				}
			},
			async fetchNFTs(forceRefresh = false, isPullDown = false) {
				if (this.loading && !isPullDown) return;
				this.clearNftRevealTimers()
				this.loading = true;
				try {
					// 1. Get the user's wallet list
					const walletListRes = await api.getWalletList();
					let wallets = [];
					if (walletListRes && walletListRes.length > 0) {
						wallets = walletListRes;
					} else {
						// Fallback if no wallets binded
						let currentAddress = uni.getStorageSync('current_wallet_address');
						if (!currentAddress) {
							currentAddress = 'cfx:aamgvyzht7h1zxdghb9ee9w26wrz8rd3gj837392dp'; // fallback to conflux mainnet address
						}
						wallets = [{ address: currentAddress, chainId: '1029', name: '' }];
					}

					// 2. Fetch NFTs for each wallet
					const groupedNfts = [];
					for (const wallet of wallets) {
						try {
							const chainId = wallet.chainId || '1029';
							const cacheKey = `cache:nfts:${chainId}:${wallet.address}`;
							let cached = null;
							if (!forceRefresh) {
								cached = cacheGet(cacheKey);
							}
							const res = cached || await api.getNfts(chainId, wallet.address);
							if (!cached || forceRefresh) {
								cacheSet(cacheKey, res || []);
							}
							let nfts = [];
							if (res && res.length > 0) {
								nfts = res;
							}
							const hasData = nfts.length > 0
							const initialVisible = hasData ? Math.min(4, nfts.length) : 0
							groupedNfts.push({
								address: wallet.address,
								chainId: wallet.chainId,
								name: wallet.name,
								nfts: nfts,
								collapsed: !hasData,
								visibleNftCount: initialVisible
							});
						} catch (e) {
							console.error(`Failed to fetch NFTs for ${wallet.address}`, e);
							groupedNfts.push({
								address: wallet.address,
								chainId: wallet.chainId,
								name: wallet.name,
								nfts: [],
								collapsed: true,
								visibleNftCount: 0
							});
						}
					}

					this.walletNfts = groupedNfts;
					this.$nextTick(() => this.scheduleRevealMoreNfts());
				} catch (err) {
					console.error('Failed to fetch wallets or NFTs:', err);
					this.walletNfts = [{
						address: 'cfx:aamgvyzht7h1zxdghb9ee9w26wrz8rd3gj837392dp',
						chainId: '1029',
						name: '',
						nfts: [],
						collapsed: true,
						visibleNftCount: 0
					}];
				} finally {
					this.loading = false;
				}
			},
			goWallet() {
				uni.switchTab({
					url: '/pages/wallet/wallet'
				})
			},
			goTransactions() {
				uni.navigateTo({
					url: '/pages/wallet/transactions'
				})
			},
			goDetail(item) {
				uni.navigateTo({
					url: `/pages/nft/detail?id=${item.id}&contract=${item.contract || ''}&tokenId=${item.tokenId || ''}`
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.page-shell {
		height: 100vh;
		display: flex;
		flex-direction: column;
		box-sizing: border-box;
		overflow: hidden;
	}

	.top-fixed {
		flex-shrink: 0;
		padding-top: 40rpx;
		padding-left: 32rpx;
		padding-right: 32rpx;
		z-index: 5;
	}

	.theme-light .top-fixed {
		padding-top: 116rpx;
		padding-left: 48rpx;
		padding-right: 48rpx;
	}

	.list-scroll {
		flex: 1;
		height: 0;
		min-height: 0;
	}

	.list-scroll-inner {
		padding-left: 32rpx;
		padding-right: 32rpx;
		padding-bottom: 220rpx;
		box-sizing: border-box;
	}

	.theme-light .list-scroll-inner {
		padding-left: 48rpx;
		padding-right: 48rpx;
	}

	.header-group {
		display: flex;
		align-items: center;
		gap: 32rpx;
		margin-bottom: 40rpx;
	}

	.theme-dark .header-group {
		margin-top: 60rpx;
	}

	.theme-light .header-group {
		margin-top: 0;
		margin-bottom: 38rpx;
		gap: 31rpx;
	}

	.segmented-control {
		display: flex;
		flex: 1;
		background:
			linear-gradient(135deg, rgba(80, 140, 200, 0.22), rgba(80, 140, 200, 0.05)),
			var(--segment-bg);
		padding: 8rpx;
		border-radius: 16rpx;
		border: 1px solid rgba(255, 255, 255, 0.22);
		backdrop-filter: blur(26rpx);
		-webkit-backdrop-filter: blur(26rpx);
	}

	.theme-light .segmented-control {
		background: #eaf1ff;
		border: none;
		padding: 8rpx;
		border-radius: 38rpx;
		box-shadow: none;
		backdrop-filter: none;
		-webkit-backdrop-filter: none;
	}

	.tx-entry {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 72rpx;
		height: 72rpx;
		flex-shrink: 0;
		border-radius: 16rpx;
		border: 1px solid rgba(255, 255, 255, 0.22);
		background: linear-gradient(135deg, rgba(85, 145, 210, 0.26), rgba(85, 145, 210, 0.07));
		backdrop-filter: blur(26rpx);
		-webkit-backdrop-filter: blur(26rpx);
	}

	.theme-light .tx-entry.tx-entry-light {
		width: 70rpx;
		height: 70rpx;
		border-radius: 38rpx;
		border: none;
		background: #eaf1ff;
		backdrop-filter: none;
		-webkit-backdrop-filter: none;
	}

	.theme-light .tx-list-icon {
		width: 38rpx;
		height: 38rpx;
		flex-shrink: 0;
	}

	.theme-dark .header-group .tx-list-icon {
		width: 38rpx;
		height: 38rpx;
		flex-shrink: 0;
		filter: brightness(0) invert(1);
		opacity: 0.92;
	}

	.segment {
		flex: 1;
		text-align: center;
		padding: 16rpx 0;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 16rpx;

		&.active {
			background-color: var(--segment-active-bg);
		}
	}

	.theme-light .segment {
		padding: 15rpx 0;
		min-height: 62rpx;
		border-radius: 38rpx;

		&.active {
			background-color: transparent;
			box-shadow: none;
		}
	}

	.segment-content {
		font-size: 28rpx;
		font-weight: 600;
		color: var(--text-secondary);
		display: block;
	}

	.theme-light .segment-content {
		font-size: 27rpx;
		font-weight: 700;
	}

	.segment.active .segment-content {
		color: var(--text-primary);
	}

	.empty-state {
		text-align: center;
		padding: 60rpx 0;
		color: var(--text-secondary);
		font-size: 28rpx;
	}

	.loading-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 100rpx 0;
		gap: 24rpx;
	}

	.loader {
		width: 60rpx;
		height: 60rpx;
		border: 6rpx solid var(--control-bg);
		border-top-color: var(--text-primary);
		border-radius: 50%;
		animation: spin 1s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	.loading-text {
		color: var(--text-secondary);
		font-size: 28rpx;
	}

	.wallet-group {
		margin-bottom: 32rpx;
		background: var(--glass-bg);
		border-radius: 28rpx;
		padding: 32rpx;
		border: 1px solid rgba(255, 255, 255, 0.22);
		backdrop-filter: blur(36rpx);
		-webkit-backdrop-filter: blur(36rpx);
		box-shadow:
			0 20rpx 54rpx rgba(91, 140, 255, 0.14),
			inset 0 1px 0 rgba(255, 255, 255, 0.2);
	}

	.wallet-card-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 24rpx;
		padding: 24rpx;
		border-radius: 24rpx;
		background: var(--glass-bg);
		border: 1px solid rgba(255, 255, 255, 0.2);
		backdrop-filter: blur(32rpx);
		-webkit-backdrop-filter: blur(32rpx);
	}

	.wallet-info-container {
		display: flex;
		flex-direction: column;
		gap: 8rpx;
		width: 100%;
	}

	.wallet-title-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		width: 100%;
	}

	.wallet-title {
		font-size: 32rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.wallet-addr-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		width: 100%;
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
		font-size: 24rpx;
		color: #64748b;
	}

	.wallet-icons {
		display: flex;
		gap: 26rpx;
		align-items: center;
		flex-shrink: 0;
	}

	.theme-light .wallet-icons {
		gap: 24rpx;
	}

	.action-icon-img {
		width: 32rpx;
		height: 32rpx;
		opacity: 0.6;
	}

	.wallet-toggle {
		padding: 10rpx;
	}

	.toggle-icon {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.wallet-collapsed-info {
		padding: 32rpx 24rpx;
		text-align: center;
		color: var(--text-secondary);
		font-size: 28rpx;
		border-radius: 24rpx;
		background: var(--glass-bg);
		border: 1px solid rgba(255, 255, 255, 0.18);
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.nft-grid {
		display: grid;
		grid-template-columns: repeat(2, minmax(0, 1fr));
		gap: 24rpx;
		align-items: start;
	}

	@media (min-width: 900px) {
		.nft-grid {
			grid-template-columns: repeat(3, minmax(0, 1fr));
			gap: 28rpx;
		}
	}

	@media (min-width: 1200px) {
		.nft-grid {
			grid-template-columns: repeat(4, minmax(0, 1fr));
			gap: 32rpx;
		}
	}

	.nft-card {
		background: var(--glass-bg);
		overflow: hidden;
		border-radius: 24rpx;
		border: 1px solid rgba(255, 255, 255, 0.2);
		backdrop-filter: blur(36rpx);
		-webkit-backdrop-filter: blur(36rpx);
		box-shadow:
			0 16rpx 42rpx rgba(91, 140, 255, 0.12),
			inset 0 1px 0 rgba(255, 255, 255, 0.18);
		
		&:active {
			transform: scale(0.98);
			transition: transform 0.2s;
		}
	}

	.image-wrapper {
		position: relative;
		width: 100%;
		height: 320rpx;
	}

	.nft-img {
		width: 100%;
		height: 100%;
		background-color: var(--control-bg);
	}

	.badge-3d {
		position: absolute;
		top: 20rpx;
		right: 20rpx;
		background-color: var(--badge-bg);
		color: var(--btn-text-white);
		font-size: 20rpx;
		font-weight: bold;
		padding: 8rpx 16rpx;
		backdrop-filter: blur(8px);
		border-radius: 999rpx;
	}

	.nft-info {
		padding: 24rpx;
		display: flex;
		flex-direction: column;
	}

	.nft-name {
		font-size: 28rpx;
		font-weight: bold;
		color: var(--text-primary);
		margin-bottom: 8rpx;
	}

	.nft-series {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

</style>
