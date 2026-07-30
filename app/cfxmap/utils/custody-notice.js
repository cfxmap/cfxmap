const CUSTODY_NOTICE_PENDING_KEY = 'custody_wallet_notice_pending'
const CUSTODY_NOTICE_ACK_KEY = 'custody_wallet_notice_ack_at'

export function markCustodyNoticePending() {
	try {
		uni.setStorageSync(CUSTODY_NOTICE_PENDING_KEY, Date.now())
	} catch (e) {}
}

export function consumeCustodyNoticePending() {
	let value = 0
	try {
		const raw = uni.getStorageSync(CUSTODY_NOTICE_PENDING_KEY)
		value = typeof raw === 'number' ? raw : 0
		uni.removeStorageSync(CUSTODY_NOTICE_PENDING_KEY)
	} catch (e) {
		value = 0
	}
	return value
}

export function markCustodyNoticeAcknowledged() {
	try {
		uni.setStorageSync(CUSTODY_NOTICE_ACK_KEY, Date.now())
	} catch (e) {}
}

export function getCustodyNoticeAcknowledgedAt() {
	try {
		const raw = uni.getStorageSync(CUSTODY_NOTICE_ACK_KEY)
		return typeof raw === 'number' ? raw : 0
	} catch (e) {
		return 0
	}
}
