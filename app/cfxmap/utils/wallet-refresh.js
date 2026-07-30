const ASSET_CHANGE_KEY = 'wallet_assets_changed_at'

export function markWalletAssetsChanged() {
	try {
		uni.setStorageSync(ASSET_CHANGE_KEY, Date.now())
	} catch (e) {}
}

export function getWalletAssetsChangedAt() {
	try {
		const value = uni.getStorageSync(ASSET_CHANGE_KEY)
		return typeof value === 'number' ? value : 0
	} catch (e) {
		return 0
	}
}

export function consumeWalletAssetsChangedAt() {
	const value = getWalletAssetsChangedAt()
	try {
		uni.removeStorageSync(ASSET_CHANGE_KEY)
	} catch (e) {}
	return value
}

