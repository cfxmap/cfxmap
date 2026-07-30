<template>
	<view class="container" :class="themeClass">
		<global-loading />
		<view class="header">
			<view class="back-btn" @click="goBack">
				<nav-back-btn />
			</view>
			<text class="title">{{ i18n.removeWallet }}</text>
		</view>

		<view class="card-wrapper">
			<view class="card-inner">
				<text class="ex-title">{{ i18n.removeWallet }}</text>
				<text class="ex-desc">{{ i18n.removeWalletDesc }}</text>

				<text class="label">{{ i18n.selectWallet }}</text>
				<picker
					v-if="removableWallets.length > 0"
					:key="walletPickerKey"
					@change="onRemovableChange"
					:value="removableIndex"
					:range="removableWallets"
					range-key="displayTitle"
				>
					<view class="picker-box">
						<text class="picker-text">{{ currentRemovable ? currentRemovable.displayTitle : '' }}</text>
						<text class="picker-arrow">></text>
					</view>
				</picker>
				<text v-else class="ex-desc">{{ i18n.removeWalletNoEligible }}</text>

				<text v-if="currentRemovable" class="hint">{{ i18n.removeWalletConfirmContent }}</text>
			</view>
		</view>

		<button
			v-if="removableWallets.length > 0"
			class="btn-danger"
			:disabled="removing || !currentRemovable"
			@click="confirmRemove"
		>
			<view class="btn-content">{{ removing ? i18n.loading : i18n.removeWalletBtn }}</view>
		</button>
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
				wallets: [],
				removableIndex: 0,
				removing: false,
				/** 列表变化后刷新 picker，避免部分端仍绑定旧 range 点确定报错 */
				walletPickerKey: 0
			}
		},
		computed: {
			...mapGetters(['i18n']),
			removableWallets() {
				return (this.wallets || []).filter(w => w.canRemove === true)
			},
			currentRemovable() {
				const list = this.removableWallets
				if (!list.length) return null
				const i = this.removableIndex
				if (i < 0 || i >= list.length) return list[0]
				return list[i]
			}
		},
		onShow() {
			this.loadWallets()
		},
		methods: {
			goBack() {
				uni.navigateBack()
			},
			onRemovableChange(e) {
				try {
					const d = e && e.detail != null ? e.detail : {}
					let v = d.value
					if (v === undefined || v === null) {
						v = 0
					}
					let idx = parseInt(String(v), 10)
					if (!Number.isFinite(idx) || idx < 0) {
						idx = 0
					}
					const list = this.removableWallets || []
					const max = Math.max(0, list.length - 1)
					if (idx > max) {
						idx = max
					}
					this.removableIndex = idx
				} catch (err) {
					console.error('picker change', err)
					this.removableIndex = 0
				}
			},
			normalizeWalletRow(w) {
				const addr = w.address || ''
				const accountIdRaw = w.accountId != null ? w.accountId : w.account_id
				const accountId =
					accountIdRaw == null || accountIdRaw === ''
						? null
						: String(accountIdRaw).trim()
				const shortAddr =
					addr.length > 16 ? addr.substring(0, 8) + '...' + addr.substring(addr.length - 6) : addr
				const nameProvisioned = typeof w.name === 'string' && w.name.indexOf('主钱包_') === 0
				const systemNew =
					w.isNewUserCreated === true ||
					w.isNewUserCreated === 1 ||
					nameProvisioned
				const hasServerCanRemove =
					Object.prototype.hasOwnProperty.call(w, 'canRemove') ||
					Object.prototype.hasOwnProperty.call(w, 'can_remove')
				let canRemove = false
				if (hasServerCanRemove) {
					const cr = w.canRemove != null ? w.canRemove : w.can_remove
					canRemove = cr === true || cr === 1 || cr === 'true' || cr === '1'
				} else {
					const dbOne = w.newUserCreatedDb === true || w.newUserCreatedDb === 1
					canRemove = !systemNew && !dbOne
				}
				const titleBase = w.name
					? `${w.name} (${shortAddr || addr || accountId})`
					: shortAddr || addr || (accountId != null ? `#${accountId}` : '')
				// 仅传纯字段给 picker，避免 ...w 带入异常结构导致部分端点「确定」崩溃
				return {
					accountId,
					address: addr,
					chainId: w.chainId || w.chain_id || '',
					name: w.name,
					isNewUserCreated: systemNew,
					canRemove,
					displayTitle: titleBase || '—'
				}
			},
			async loadWallets() {
				try {
					const res = await api.getExportWalletList2()
					const list = Array.isArray(res) ? res : []
					this.wallets = list.map(w => this.normalizeWalletRow(w))
					const removable = this.removableWallets
					let idx = 0
					const currentAddr = uni.getStorageSync('current_wallet_address')
					if (currentAddr && removable.length) {
						const i = removable.findIndex(x => x.address === currentAddr)
						if (i >= 0) idx = i
					}
					this.removableIndex = removable.length ? Math.min(idx, removable.length - 1) : 0
					this.walletPickerKey += 1
				} catch (e) {
					console.error('loadWallets failed', e)
					this.wallets = []
					this.removableIndex = 0
					this.walletPickerKey += 1
				}
			},
			confirmRemove() {
				const w = this.currentRemovable
				const rawId = w && (w.accountId != null ? w.accountId : w.account_id)
				if (!w || rawId == null || rawId === '' || this.removing) return
				uni.showModal({
					title: this.i18n.removeWalletConfirmTitle,
					content: this.i18n.removeWalletConfirmContent,
					confirmText: this.i18n.confirmBtn,
					cancelText: this.i18n.cancelBtn,
					success: (res) => {
						if (res.confirm) {
							this.doRemove(w)
						}
					}
				})
			},
			async doRemove(w) {
				if (this.removing) return
				const rawId = w && (w.accountId != null ? w.accountId : w.account_id)
				const idStr = rawId != null && rawId !== '' ? String(rawId).trim() : ''
				// 禁止 parseInt/Number：大于 2^53-1 的雪花 ID 会丢精度
				if (w == null || !/^\d+$/.test(idStr)) {
					uni.showToast({ title: this.i18n.selectWalletFirst, icon: 'none' })
					return
				}
				this.removing = true
				const removedAddr = w.address
				uni.showLoading({ title: this.i18n.loading, mask: true })
				try {
					await api.removeWallet(idStr)
					if (uni.getStorageSync('current_wallet_address') === removedAddr) {
						const res = await api.getWalletList()
						const list = Array.isArray(res) ? res : []
						if (list.length > 0 && list[0].address) {
							uni.setStorageSync('current_wallet_address', list[0].address)
						} else {
							uni.removeStorageSync('current_wallet_address')
						}
					}
					clearWalletCache()
					markWalletAssetsChanged()
					uni.showToast({ title: this.i18n.removeWalletSuccess, icon: 'success' })
					await this.loadWallets()
				} catch (e) {
					console.error('remove wallet failed', e)
				} finally {
					uni.hideLoading()
					this.removing = false
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	.container {
		min-height: 100vh;
		padding: 0 32rpx 80rpx;
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

	.hint {
		font-size: 24rpx;
		color: var(--warning-text);
		background-color: var(--warning-bg);
		padding: 16rpx;
		border-radius: 16rpx;
		line-height: 1.5;
	}

	.btn-danger {
		width: 100%;
		height: 112rpx;
		background-color: rgba(239, 68, 68, 0.14);
		border-radius: 16rpx;
		border: 1px solid rgba(239, 68, 68, 0.35);
		display: flex;
		align-items: center;
		justify-content: center;
		margin-top: 8rpx;
		&::after {
			border: none;
		}
	}

	.btn-danger[disabled] {
		opacity: 0.55;
	}

	.btn-content {
		color: var(--warning-text);
		font-size: 32rpx;
		font-weight: 600;
	}
</style>
