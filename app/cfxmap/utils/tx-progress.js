import { cacheRemove } from '@/utils/cache'
import { markWalletAssetsChanged } from '@/utils/wallet-refresh'

const STORAGE_KEY = 'wallet_tx_progress_list'
const PENDING_STATUSES = new Set(['pending', 'confirming', 'broadcasted', 'initiated', 'signed'])

function normalizeStatus(status) {
	const raw = String(status || '').trim().toLowerCase()
	if (raw === 'success' || raw === 'completed') return 'Completed'
	if (raw === 'failed' || raw === 'failure') return 'Failed'
	return 'Pending'
}

function normalizeTime(value) {
	if (typeof value === 'number' && Number.isFinite(value)) return value
	const time = new Date(value).getTime()
	return Number.isFinite(time) ? time : Date.now()
}

function cleanup(list) {
	const now = Date.now()
	return (Array.isArray(list) ? list : []).filter((item) => {
		if (!item || !item.txHash) return false
		const createdAt = normalizeTime(item.createTime || item.createdAt)
		const age = now - createdAt
		const status = normalizeStatus(item.status)
		if (status === 'Pending') {
			return age <= 7 * 24 * 60 * 60 * 1000
		}
		return age <= 24 * 60 * 60 * 1000
	})
}

function writeList(list) {
	try {
		uni.setStorageSync(STORAGE_KEY, cleanup(list))
	} catch (e) {}
}

export function getTxProgressList() {
	try {
		const list = cleanup(uni.getStorageSync(STORAGE_KEY))
		writeList(list)
		return list
	} catch (e) {
		return []
	}
}

export function getTxProgressByHash(txHash) {
	if (!txHash) return null
	return getTxProgressList().find((item) => item.txHash === txHash) || null
}

export function upsertTxProgress(record) {
	if (!record || !record.txHash) return null
	const list = getTxProgressList()
	const next = {
		...record,
		status: normalizeStatus(record.status),
		createTime: normalizeTime(record.createTime || record.createdAt),
		updatedAt: Date.now()
	}
	const index = list.findIndex((item) => item.txHash === next.txHash)
	if (index >= 0) {
		list.splice(index, 1, { ...list[index], ...next })
	} else {
		list.unshift(next)
	}
	writeList(list)
	return next
}

export function mergePendingTxs(remoteList, walletAddress) {
	const remote = Array.isArray(remoteList) ? remoteList.slice() : []
	const address = String(walletAddress || '').toLowerCase()
	if (!address) return remote

	const existingHashes = new Set(remote.map((item) => item && item.txHash).filter(Boolean))
	const localPending = getTxProgressList()
		.filter((item) => {
			const status = String(item.status || '').trim().toLowerCase()
			const from = String(item.fromAddress || '').toLowerCase()
			const to = String(item.toAddress || '').toLowerCase()
			return PENDING_STATUSES.has(status) && (from === address || to === address)
		})
		.filter((item) => !existingHashes.has(item.txHash))

	return localPending.concat(remote)
}

export function syncTxProgress(record) {
	return upsertTxProgress(record)
}

export function invalidateTxRelatedCache(record) {
	if (!record) return
	const chainId = String(record.chainId || '1029')
	const fromAddress = record.fromAddress
	if (fromAddress) {
		cacheRemove(`cache:txs:v2:${chainId}:${fromAddress}`)
		cacheRemove(`cache:tokens:${chainId}:${fromAddress}`)
	}
	const toAddress = record.toAddress
	if (toAddress) {
		cacheRemove(`cache:tokens:${chainId}:${toAddress}`)
	}
	markWalletAssetsChanged()
}

export function buildExplorerUrl(txHash, chainId) {
	if (!txHash) return ''
	const isTestnet = String(chainId || '') === '1'
	const baseUrl = isTestnet ? 'https://testnet.confluxscan.io/tx/' : 'https://confluxscan.io/tx/'
	return `${baseUrl}${txHash}`
}
