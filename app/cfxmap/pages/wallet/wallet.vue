<template>
	<view class="page-shell" :class="themeClass">
		<global-loading />
		<view class="top-fixed">
			<!-- Segmented Control and Transaction History -->
			<view class="header-group">
				<view class="segmented-control">
					<view class="segment active">
						<text class="segment-content">{{ i18n.tokens }}</text>
					</view>
					<view class="segment" @click="goNFTs">
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

			<!-- Asset Card（LightNew：纵向居中排布，与设计稿 Slb0f 一致） -->
			<view class="asset-card" :class="{'asset-card--collapsed': isCardCollapsed}">
				<view v-if="isDarkMode" class="asset-card-glow asset-card-glow-left"></view>
				<view v-if="isDarkMode" class="asset-card-glow asset-card-glow-right"></view>
				<view class="asset-title-group">
					<text class="asset-title">{{ i18n.totalBalance }}</text>
					<view class="eye-btn" @click.stop="toggleBalancePrivacy">
						<text class="eye-icon" :class="{ 'eye-icon--off': balanceHidden }">👁</text>
					</view>
				</view>
				<text class="asset-value">{{ formattedTotalBalance }}</text>
				<view class="action-group" :class="{'action-group--collapsed': isCardCollapsed}">
					<view
						class="action-btn-wrapper"
						:class="actionSendBtnClass"
						@click="goSend"
					>
						<image
							v-if="useLightActionPng"
							class="action-btn-img"
							:src="$staticUrl('/static/wallet-actions/btn-send-lightnew.png')"
							mode="widthFix"
						/>
						<view v-else class="action-btn-inner">
							<image
								class="action-arrow-img"
								:src="sendActionArrowSrc"
								mode="aspectFit"
							/>
							<text class="action-text">{{ i18n.sendTitle }}</text>
						</view>
					</view>
					<view
						class="action-btn-wrapper"
						:class="actionReceiveBtnClass"
						@click="goReceive"
					>
						<image
							v-if="useLightActionPng"
							class="action-btn-img"
							:src="$staticUrl('/static/wallet-actions/btn-receive-lightnew.png')"
							mode="widthFix"
						/>
						<view v-else class="action-btn-inner">
							<image
								class="action-arrow-img"
								:src="receiveActionArrowSrc"
								mode="aspectFit"
							/>
							<text class="action-text">{{ i18n.receiveTitle }}</text>
						</view>
					</view>
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
			@scroll="onScroll"
		>
			<view class="list-scroll-inner">

				<!-- 仅钱包列表未就绪时全屏加载；代币按钱包逐个加载 -->
				<view v-if="listLoading && walletTokens.length === 0" class="loading-state">
					<view class="loader"></view>
					<text class="loading-text">{{ i18n.loading }}</text>
				</view>

				<!-- Asset List -->
				<view class="wallet-group" v-for="(group, gIndex) in walletTokens" :key="gIndex">
			<view class="group-header">
				<view class="group-title-row">
					<text class="group-title">{{ group.name || i18n.mainWallet }}</text>
				</view>
				<view class="group-addr-row">
					<text class="group-addr">{{ formatWalletAddress(group.address) }}</text>
					<view class="group-icons">
						<image class="action-icon-img" :src="$staticUrl('/static/icons/icon_copy.png')" mode="aspectFit" @click.stop="copyAddress(group.address)"></image>
						<image class="action-icon-img" :src="$staticUrl('/static/icons/icon_qrcode.png')" mode="aspectFit" @click.stop="showQRCode(group.address)"></image>
					</view>
				</view>
			</view>
			<view class="asset-list">
				<view v-if="group.tokensLoading" class="wallet-tokens-loading">
					<view class="loader loader--sm"></view>
					<text class="loading-text loading-text--inline">{{ i18n.loading }}</text>
				</view>
				<block v-else>
				<view class="asset-item" v-for="(item, index) in group.assets" :key="index" @click="openAssetDetail(group, item)">
					<view class="asset-left">
						<view class="icon-wrapper">
							<image class="asset-icon" :src="item.icon" mode="aspectFill"></image>
						</view>
						<view class="asset-name-group">
							<text class="asset-name">{{item.name}}</text>
							<text v-if="item.name !== item.symbol" class="asset-symbol">{{item.symbol}}</text>
						</view>
					</view>
					<view class="asset-right">
						<text class="asset-amount">{{ balanceHidden ? '****' : formatTokenAmount(item.amount) }}</text>
						<text class="asset-fiat">{{ balanceHidden ? '****' : item.fiat }}</text>
					</view>
				</view>
				</block>
			</view>
				</view>
			</view>
		</scroll-view>
		<bottom-nav current="wallet" />
		<!-- #ifdef H5 -->
		<h5-add-to-home-tip bottomNav />
		<!-- #endif -->
	</view>
</template>

<script>
	import { mapState, mapGetters } from 'vuex'
	import { api } from '@/utils/request'
	import { cacheGet, cacheSet } from '@/utils/cache'
	import { formatFixed4, formatMoneyEn } from '@/utils/format-money'
	import { getTxProgressList } from '@/utils/tx-progress'
	import { consumeCustodyNoticePending, markCustodyNoticeAcknowledged } from '@/utils/custody-notice'
	import { consumeWalletAssetsChangedAt, markWalletAssetsChanged } from '@/utils/wallet-refresh'
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
				totalBalanceUsd: 0,
				usdToCnyRate: 7.2,
				walletTokens: [],
				loading: false,
				listLoading: false,
				_tokensPipelineBusy: false,
				_assetsFetchGen: 0,
				_profilePopupChecked: false,
				balanceHidden: false,
				refreshing: false,
				isCardCollapsed: false,
				assetPollTimer: null
			}
		},
		computed: {
			...mapState(['currency', 'language']),
			...mapGetters(['i18n']),
			/** 仅浅色+英文用原版 LightNew 切图；浅色+中文与深色均为 Lucide 箭头 + i18n 文案 */
			useLightActionPng() {
				return !this.isDarkMode && this.language !== '中文'
			},
			useLightZhActionStyle() {
				return !this.isDarkMode && this.language === '中文'
			},
			actionSendBtnClass() {
				return {
					'action-btn-slice': this.useLightActionPng,
					'action-btn-pill--send': this.useLightZhActionStyle
				}
			},
			actionReceiveBtnClass() {
				return {
					'action-btn-slice': this.useLightActionPng,
					'action-btn-pill--receive': this.useLightZhActionStyle
				}
			},
			sendActionArrowSrc() {
				const stroke = this.useLightZhActionStyle ? '#E68620' : '#F5F7FF'
				return this.lucideArrowDataUri('up', stroke)
			},
			receiveActionArrowSrc() {
				const stroke = this.useLightZhActionStyle ? '#22A65A' : '#F5F7FF'
				return this.lucideArrowDataUri('down', stroke)
			},
			formattedTotalBalance() {
				const isCny = this.currency === 'CNY'
				const symbol = isCny ? '¥' : '$'
				if (this.balanceHidden) {
					return `${symbol}****`
				}
				const val = isCny ? this.totalBalanceUsd * this.usdToCnyRate : this.totalBalanceUsd
				return `${symbol}${formatMoneyEn(val)}`
			},
			refresherIndicatorStyle() {
				return this.isDarkMode ? 'white' : 'black'
			}
		},
		onShow() {
			uni.hideTabBar()
			this.syncBalanceHiddenFromStorage()
			this.runEntryPopups()
			const hasAssetChangeSignal = consumeWalletAssetsChangedAt() > 0
			const hasPendingTx = this.hasPendingTxForCurrentWallets()
			this.fetchAssets(hasAssetChangeSignal || hasPendingTx)
			this.updateAssetPolling(hasPendingTx)
		},
		onHide() {
			this.stopAssetPolling()
		},
		onUnload() {
			this.stopAssetPolling()
		},
		methods: {
			async runEntryPopups() {
				await this.checkCustodyWalletNotice()
				await this.checkNewUserRewardPopup()
			},
			onScroll(e) {
				const scrollTop = e.detail.scrollTop
				if (scrollTop > 20) {
					if (!this.isCardCollapsed) this.isCardCollapsed = true
				} else if (scrollTop <= 5) {
					if (this.isCardCollapsed) this.isCardCollapsed = false
				}
			},
			async onRefresherRefresh() {
				this.refreshing = true
				try {
					await this.fetchAssets(true, true)
					markWalletAssetsChanged()
				} finally {
					this.refreshing = false
				}
			},
			hasPendingTxForCurrentWallets() {
				const groups = this.walletTokens || []
				const tracked = groups.map(item => String(item.address || '').toLowerCase()).filter(Boolean)
				const pendingList = getTxProgressList().filter(item => {
					const status = String(item.status || '').trim().toLowerCase()
					return status === 'pending'
				})
				if (!tracked.length) {
					return pendingList.length > 0
				}
				return pendingList.some(item => {
					const from = String(item.fromAddress || '').toLowerCase()
					const to = String(item.toAddress || '').toLowerCase()
					return tracked.includes(from) || tracked.includes(to)
				})
			},
			updateAssetPolling(forcePending) {
				const shouldPoll = forcePending || this.hasPendingTxForCurrentWallets()
				if (shouldPoll) {
					this.startAssetPolling()
				} else {
					this.stopAssetPolling()
				}
			},
			startAssetPolling() {
				if (this.assetPollTimer) return
				this.assetPollTimer = setInterval(() => {
					if (this._tokensPipelineBusy) return
					if (!this.hasPendingTxForCurrentWallets()) {
						this.stopAssetPolling()
						return
					}
					this.fetchAssets(true)
				}, 30000)
			},
			stopAssetPolling() {
				if (this.assetPollTimer) {
					clearInterval(this.assetPollTimer)
					this.assetPollTimer = null
				}
			},
			lucideArrowDataUri(direction, stroke) {
				const paths =
					direction === 'up'
						? '<path d="m5 12 7-7 7 7"/><path d="M12 19V5"/>'
						: '<path d="M12 5v14"/><path d="m19 12-7 7-7-7"/>'
				const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="${stroke}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">${paths}</svg>`
				return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`
			},
			syncBalanceHiddenFromStorage() {
				const v = uni.getStorageSync('wallet_balance_hidden')
				this.balanceHidden = v === true || v === 'true'
			},
			toggleBalancePrivacy() {
				this.balanceHidden = !this.balanceHidden
				uni.setStorageSync('wallet_balance_hidden', this.balanceHidden)
			},
			formatWalletTitle(group) {
				const addr = group.address || '';
				let shortAddr = addr;
				if (addr.length > 12) {
					shortAddr = addr.substring(0, 6) + '...' + addr.substring(addr.length - 4);
				}
				if (group.name && group.name.trim() !== '') {
					return `${group.name} (${shortAddr})`;
				}
				return shortAddr;
			},
			formatWalletAddress(addr) {
				if (!addr) return '';
				/* 尽量多展示：仅超长地址中间省略 */
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
						uni.showToast({ title: this.i18n.addressCopied, icon: 'none' });
					}
				});
			},
			showQRCode(address) {
				// We can navigate to a receive page or show a modal with QR code
				uni.navigateTo({
					url: `/pages/transfer/receive?address=${encodeURIComponent(address)}`
				});
			},
			formatAssetFiat(asset) {
				const isCny = this.currency === 'CNY'
				const symbol = isCny ? '¥' : '$'
				const val = isCny ? asset.usdValue * this.usdToCnyRate : asset.usdValue
				return `${symbol}${formatMoneyEn(val)}`
			},
			formatTokenAmount(value) {
				return formatFixed4(value)
			},
			recomputeTotalUsdFromGroups() {
				let t = 0
				;(this.walletTokens || []).forEach((g) => {
					;(g.assets || []).forEach((a) => {
						t += a.usdValue || 0
					})
				})
				this.totalBalanceUsd = t
			},
			async checkCustodyWalletNotice() {
				const pendingAt = consumeCustodyNoticePending()
				if (!pendingAt) {
					return
				}
				markCustodyNoticeAcknowledged()
				await new Promise(resolve => {
					uni.showModal({
						title: this.i18n.custodyWalletModalTitle,
						content: this.i18n.custodyWalletModalContent,
						confirmText: this.i18n.custodyWalletViewNoticeBtn,
						cancelText: this.i18n.custodyWalletLaterBtn,
						success: (res) => {
							if (res.confirm) {
								uni.navigateTo({
									url: '/pages/profile/custody_wallet_notice'
								})
							}
							resolve()
						},
						fail: () => resolve()
					})
				})
			},
			async checkNewUserRewardPopup() {
				if (this._profilePopupChecked) {
					return
				}
				this._profilePopupChecked = true
				try {
					const profile = await api.getUserProfile()
					const rewardPopup = profile && profile.newUserRewardPopup
					if (!rewardPopup || !rewardPopup.content) {
						return
					}
					await new Promise(resolve => {
						uni.showModal({
							title: rewardPopup.title || '新用户奖励',
							content: rewardPopup.content,
							showCancel: false,
							confirmText: this.i18n.confirmBtn || '确定',
							success: () => resolve(),
							fail: () => resolve()
						})
					})
				} catch (e) {}
			},
			async fetchAssets(forceRefresh = false, isPullDown = false) {
				if (this._tokensPipelineBusy) return
				this._tokensPipelineBusy = true
				const gen = ++this._assetsFetchGen
				this.listLoading = true
				this.loading = true
				try {
					const walletListRes = await api.getWalletList()
					let wallets = []
					if (walletListRes && walletListRes.length > 0) {
						wallets = walletListRes
					} else {
						let currentAddress = uni.getStorageSync('current_wallet_address')
						if (!currentAddress) {
							currentAddress = 'cfx:aamgvyzht7h1zxdghb9ee9w26wrz8rd3gj837392dp' // fallback
						}
						wallets = [{ address: currentAddress, chainId: '1029', name: '' }]
					}

					if (gen !== this._assetsFetchGen) return

					const skeleton = wallets.map((w) => ({
						address: w.address,
						chainId: w.chainId,
						name: w.name,
						assets: [],
						tokensLoading: true
					}))
					this.walletTokens = skeleton
					this.totalBalanceUsd = 0
					this.listLoading = false
					this.loading = false

					for (let i = 0; i < wallets.length; i++) {
						if (gen !== this._assetsFetchGen) return
						const wallet = wallets[i]
						try {
							const chainId = wallet.chainId || '1'
							const cacheKey = `cache:tokens:${chainId}:${wallet.address}`
							let cached = null
							if (!forceRefresh) {
								cached = cacheGet(cacheKey)
							}
							const res = cached || await api.getTokens(chainId, wallet.address)
							if (!cached || forceRefresh) {
								cacheSet(cacheKey, res || [])
							}

							let assets = []
							if (res && res.length > 0) {
								assets = res.map((asset) => ({
									...asset,
									fiat: this.formatAssetFiat(asset)
								}))
							}
							if (gen !== this._assetsFetchGen) return
							this.$set(this.walletTokens[i], 'assets', assets)
							this.$set(this.walletTokens[i], 'tokensLoading', false)
							this.recomputeTotalUsdFromGroups()
							this.updateAssetPolling(false)
						} catch (e) {
							console.error(`Failed to fetch assets for ${wallet.address}`, e)
							if (gen !== this._assetsFetchGen) return
							this.$set(this.walletTokens[i], 'assets', [])
							this.$set(this.walletTokens[i], 'tokensLoading', false)
							this.recomputeTotalUsdFromGroups()
							this.updateAssetPolling(false)
						}
					}
				} catch (err) {
					console.error('Failed to fetch assets:', err)
					if (gen === this._assetsFetchGen) {
						this.walletTokens = [{
							address: 'cfx:aamgvyzht7h1zxdghb9ee9w26wrz8rd3gj837392dp',
							chainId: '1029',
							name: '',
							assets: [],
							tokensLoading: false
						}]
						this.totalBalanceUsd = 0
						this.listLoading = false
					}
				} finally {
					this.updateAssetPolling(false)
					if (gen === this._assetsFetchGen) {
						this.listLoading = false
						this.loading = false
					}
					this._tokensPipelineBusy = false
				}
			},
			goNFTs() {
				uni.navigateTo({
					url: '/pages/nft/gallery'
				})
			},
			goTransactions() {
				uni.navigateTo({
					url: '/pages/wallet/transactions'
				})
			},
			openAssetDetail(group, item) {
				if (!group || !item) return
				const payload = encodeURIComponent(JSON.stringify({
					walletAddress: group.address,
					chainId: group.chainId || '1029',
					walletName: group.name || '',
					name: item.name,
					symbol: item.symbol,
					type: item.type,
					contract: item.contract || '',
					amount: item.amount,
					fiat: item.fiat,
					usdValue: item.usdValue,
					icon: item.icon
				}))
				uni.navigateTo({
					url: `/pages/wallet/asset_detail?payload=${payload}`
				})
			},
			goSend() {
				uni.navigateTo({
					url: '/pages/transfer/send'
				})
			},
			goReceive() {
				uni.navigateTo({
					url: '/pages/transfer/receive'
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

	.theme-light.page-shell {
		/* LightNew wwtPS：390 宽画布 → rpx 按 750/390 */
	}

	.top-fixed {
		flex-shrink: 0;
		padding-top: 40rpx;
		padding-left: 32rpx;
		padding-right: 32rpx;
		z-index: 5;
		display: flex;
		flex-direction: column;
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
		scrollbar-width: none;
		-ms-overflow-style: none;
	}

	.list-scroll::-webkit-scrollbar {
		width: 0;
		height: 0;
		display: none;
		background: transparent;
	}

	.list-scroll-inner {
		padding-top: 16rpx;
		padding-bottom: 220rpx;
		padding-left: 32rpx;
		padding-right: 32rpx;
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
		/* 试效果：把图标中的深色压成黑再反相为白（整图会呈单色白轮廓） */
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

	.asset-card {
		position: relative;
		overflow: hidden;
		background: transparent;
		padding: 48rpx;
		border-radius: 32rpx;
		margin-bottom: 24rpx;
		border: 1px solid rgba(255, 255, 255, 0.22);
		box-shadow:
			0 24rpx 60rpx rgba(51, 207, 255, 0.12),
			inset 0 1px 0 rgba(255, 255, 255, 0.24);
		backdrop-filter: blur(36rpx);
		-webkit-backdrop-filter: blur(36rpx);
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.asset-card.asset-card--collapsed {
		padding-top: 24rpx;
		padding-bottom: 24rpx;
		margin-bottom: 24rpx;
	}

	.theme-light .asset-card {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 31rpx;
		padding: 46rpx;
		border-radius: 38rpx;
		margin-bottom: 24rpx;
		min-height: 0;
		background: rgba(255, 255, 255, 0.8);
		border: 1px solid rgba(255, 255, 255, 0.26);
		box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
		backdrop-filter: blur(48rpx);
		-webkit-backdrop-filter: blur(48rpx);
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.theme-light .asset-card.asset-card--collapsed {
		padding-top: 24rpx;
		padding-bottom: 24rpx;
		margin-bottom: 24rpx;
		gap: 16rpx;
	}

	.theme-dark .asset-card {
		background: var(--glass-bg);
	}

	.asset-card-glow {
		position: absolute;
		border-radius: 50%;
		filter: blur(20rpx);
		opacity: 0.8;
		pointer-events: none;
	}

	.asset-card-glow-left {
		top: -72rpx;
		left: -48rpx;
		width: 220rpx;
		height: 220rpx;
		background: rgba(160, 210, 255, 0.2);
	}

	.asset-card-glow-right {
		right: -64rpx;
		bottom: -88rpx;
		width: 240rpx;
		height: 240rpx;
		background: rgba(120, 180, 240, 0.15);
	}

	.asset-title-group {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: center;
		margin-bottom: 16rpx;
	}

	.theme-light .asset-title-group {
		justify-content: center;
		margin-bottom: 0;
		gap: 16rpx;
		width: 100%;
	}

	.asset-title {
		font-size: 28rpx;
		color: var(--text-secondary);
		margin-right: 16rpx;
	}

	.theme-light .asset-title {
		font-size: 27rpx;
		font-weight: 400;
		margin-right: 0;
		color: #64748b;
	}

	.eye-btn {
		padding: 12rpx;
		margin: -12rpx -12rpx -12rpx 4rpx;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.eye-icon {
		font-size: 32rpx;
		color: var(--text-secondary);
	}

	.eye-icon.eye-icon--off {
		opacity: 0.45;
	}

	.theme-light .eye-icon {
		font-size: 24rpx;
		opacity: 0.75;
	}

	.theme-light .eye-icon.eye-icon--off {
		opacity: 0.38;
	}

	.asset-value {
		position: relative;
		z-index: 1;
		font-size: 72rpx;
		font-weight: 700;
		color: var(--text-primary);
		display: block;
		margin-bottom: 48rpx;
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.asset-card--collapsed .asset-value {
		margin-bottom: 0;
		font-size: 56rpx;
	}

	.theme-light .asset-value {
		font-size: 70rpx;
		text-align: center;
		width: 100%;
		margin-bottom: 0;
		line-height: 1.12;
		letter-spacing: -0.02em;
	}

	.theme-light .asset-card--collapsed .asset-value {
		font-size: 54rpx;
	}

	.action-group {
		position: relative;
		z-index: 1;
		display: flex;
		justify-content: space-between;
		gap: 32rpx;
		width: 100%;
		max-height: 200rpx;
		opacity: 1;
		transform: scaleY(1);
		transform-origin: top;
		transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
		overflow: hidden;
	}

	.action-group.action-group--collapsed {
		max-height: 0;
		opacity: 0;
		transform: scaleY(0);
		margin-top: 0;
	}

	.theme-light .action-group {
		gap: 31rpx;
	}

	.theme-light .action-group.action-group--collapsed {
		margin-top: -31rpx;
	}

	.action-btn-wrapper {
		flex: 1;
		background: linear-gradient(180deg, rgba(90, 150, 215, 0.22), rgba(91, 140, 255, 0.1));
		border-radius: 24rpx;
		border: 1px solid rgba(255, 255, 255, 0.22);

		&:active {
			opacity: 0.8;
		}
	}

	.action-btn-wrapper.action-btn-slice {
		background: transparent;
		border: none;
		padding: 0;
		overflow: hidden;
		border-radius: 20rpx;
	}

	.action-btn-img {
		width: 100%;
		display: block;
	}

	.action-btn-inner {
		padding: 24rpx;
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: center;
		gap: 16rpx;
	}

	.action-icon {
		font-size: 32rpx;
	}

	.action-text {
		font-size: 28rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	/* 浅色中文：与英文切图同系配色（暖底+橙箭 / 绿底+绿箭） */
	.theme-light .action-btn-wrapper.action-btn-pill--send {
		background: linear-gradient(180deg, #fffdfb 0%, #fff3e6 100%);
		border: 1px solid rgba(230, 150, 70, 0.38);
		border-radius: 24rpx;
		box-shadow: 0 8rpx 26rpx rgba(230, 134, 32, 0.14);
	}

	.theme-light .action-btn-wrapper.action-btn-pill--receive {
		background: linear-gradient(180deg, #f9fefc 0%, #e6f7ee 100%);
		border: 1px solid rgba(46, 180, 100, 0.38);
		border-radius: 24rpx;
		box-shadow: 0 8rpx 26rpx rgba(34, 170, 90, 0.12);
	}

	/* Pencil：lucide 24 视图盒 */
	.action-arrow-img {
		width: 48rpx;
		height: 48rpx;
		flex-shrink: 0;
	}

	.theme-light .action-btn-wrapper:not(.action-btn-slice) .action-btn-inner {
		padding: 28rpx 20rpx;
		min-height: 96rpx;
		box-sizing: border-box;
		justify-content: center;
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

	.wallet-tokens-loading {
		display: flex;
		flex-direction: row;
		align-items: center;
		justify-content: center;
		gap: 20rpx;
		padding: 40rpx 24rpx;
		color: var(--text-secondary);
	}

	.theme-light .wallet-tokens-loading {
		padding: 32rpx 20rpx;
	}

	.loader--sm {
		width: 40rpx;
		height: 40rpx;
		border-width: 4rpx;
	}

	.loading-text--inline {
		font-size: 26rpx;
	}

	.asset-list {
		display: flex;
		flex-direction: column;
		gap: 24rpx;
	}

	.theme-light .asset-list {
		gap: 15rpx;
	}

	.wallet-group {
		margin-bottom: 40rpx;
		padding: 28rpx;
		border-radius: 32rpx;
		background: var(--glass-bg);
		border: 1px solid rgba(255, 255, 255, 0.22);
		backdrop-filter: blur(36rpx);
		-webkit-backdrop-filter: blur(36rpx);
		box-shadow:
			0 22rpx 56rpx rgba(91, 140, 255, 0.14),
			inset 0 1px 0 rgba(255, 255, 255, 0.2);
	}

	.theme-light .wallet-group {
		padding: 31rpx;
		border-radius: 38rpx;
		margin-bottom: 38rpx;
		background: rgba(255, 255, 255, 0.8);
		border: 1px solid rgba(216, 228, 255, 0.55);
		box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.05);
		backdrop-filter: blur(48rpx);
		-webkit-backdrop-filter: blur(48rpx);
	}

	.group-header {
		display: flex;
		flex-direction: column;
		gap: 16rpx;
		margin-bottom: 24rpx;
		padding: 0 8rpx;
	}

	.theme-light .group-header {
		gap: 12rpx;
		margin-bottom: 23rpx;
		padding: 0;
	}

	.group-title-row {
		display: flex;
		justify-content: flex-start;
		align-items: center;
	}

	.group-title {
		font-size: 32rpx;
		font-weight: 600;
		color: var(--text-primary);
	}

	.theme-light .group-title {
		font-size: 31rpx;
		font-weight: 700;
	}

	
	.group-addr-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		margin-top: 8rpx;
	}
	
	.group-addr {
		font-size: 24rpx;
		color: var(--text-secondary);
		flex: 1;
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.theme-light .group-addr {
		font-size: 24rpx;
		color: #64748b;
	}
	
	.group-icons {
		display: flex;
		gap: 26rpx;
		align-items: center;
		flex-shrink: 0;
	}

	.theme-light .group-icons {
		gap: 24rpx;
	}
	
	.action-icon-img {
		width: 28rpx;
		height: 28rpx;
		opacity: 0.6;
	}

	.theme-light .action-icon-img {
		width: 28rpx;
		height: 28rpx;
		opacity: 0.72;
	}
	
	.asset-item {
		background: var(--glass-bg);
		border-radius: 24rpx;
		padding: 32rpx;
		display: flex;
		justify-content: space-between;
		align-items: center;
		border: 1px solid rgba(255, 255, 255, 0.2);
		backdrop-filter: blur(36rpx);
		-webkit-backdrop-filter: blur(36rpx);
		box-shadow:
			0 16rpx 40rpx rgba(91, 140, 255, 0.12),
			inset 0 1px 0 rgba(255, 255, 255, 0.18);
	}

	.theme-light .asset-item {
		border-radius: 20rpx;
		padding: 18rpx 24rpx;
		min-height: 112rpx;
		background: #f6f9ff;
		border: 1px solid rgba(220, 231, 248, 0.65);
		box-shadow: 0 4rpx 12rpx rgba(15, 23, 42, 0.04);
		backdrop-filter: none;
		-webkit-backdrop-filter: none;
	}

	.asset-left {
		display: flex;
		align-items: center;
		flex: 1;
	}

	.icon-wrapper {
		position: relative;
		width: 80rpx;
		height: 80rpx;
		margin-right: 24rpx;
		flex-shrink: 0;
	}

	.theme-light .icon-wrapper {
		width: 77rpx;
		height: 77rpx;
		margin-right: 23rpx;
	}

	.asset-icon {
		width: 100%;
		height: 100%;
		border-radius: 20rpx;
		background-color: var(--control-bg);
		display: block;
	}

	.theme-light .asset-icon {
		border-radius: 50%;
	}

	.asset-name-group {
		display: flex;
		flex-direction: column;
		justify-content: center;
	}

	.asset-name {
		font-size: 32rpx;
		font-weight: 600;
		color: var(--text-primary);
		margin-bottom: 8rpx;
	}

	.theme-light .asset-name {
		font-size: 31rpx;
		font-weight: 700;
		margin-bottom: 4rpx;
	}

	.asset-symbol {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.theme-light .asset-symbol {
		font-size: 24rpx;
		color: #64748b;
	}

	.asset-right {
		display: flex;
		flex-direction: column;
		align-items: flex-end;
		flex-shrink: 0;
	}

	.asset-amount {
		font-size: 32rpx;
		font-weight: 600;
		color: var(--text-primary);
		margin-bottom: 8rpx;
	}

	.theme-light .asset-amount {
		font-size: 31rpx;
		font-weight: 700;
		margin-bottom: 4rpx;
	}

	.asset-fiat {
		font-size: 24rpx;
		color: var(--text-secondary);
	}

	.theme-light .asset-fiat {
		font-size: 24rpx;
		color: #64748b;
	}
</style>
