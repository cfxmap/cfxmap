import { getBaseUrl } from '@/utils/request.js'
import { getStaticUrl } from '@/utils/static-resource.js'

const ST_REMOTE = 'app_avatar_cache_remote_url'
const ST_LOCAL = 'app_avatar_cache_local_path'
const DEFAULT_AVATAR_PATH = '/static/avatar.png'
const DEFAULT_AVATAR_URL = getStaticUrl(DEFAULT_AVATAR_PATH)
/** 历史键：仅用于清理残留文件 */
const ST_LEGACY = 'local_user_avatar_path'

export function toAbsoluteAvatarUrl(remote) {
	const r = (remote || '').trim()
	if (!r) return ''
	if (/^https?:\/\//i.test(r)) return r
	if (r.startsWith('//')) {
		// #ifdef H5
		if (typeof window !== 'undefined' && window.location && window.location.protocol) {
			return window.location.protocol + r
		}
		// #endif
		return 'https:' + r
	}
	const base = getBaseUrl().replace(/\/+$/, '')
	const path = r.startsWith('/') ? r : `/${r}`
	return base + path
}

function isDefaultPlaceholder(remote) {
	const r = (remote || '').trim()
	return !r || r === DEFAULT_AVATAR_PATH || r === DEFAULT_AVATAR_URL
}

/** 删除本地已保存的头像文件及元数据（头像变更、恢复默认、退出登录时调用） */
export function clearAvatarCache() {
	const paths = [uni.getStorageSync(ST_LOCAL), uni.getStorageSync(ST_LEGACY)].filter(
		(p) => typeof p === 'string' && p.length > 0
	)
	for (const p of paths) {
		try {
			uni.removeSavedFile({ filePath: p, fail: () => {} })
		} catch (e) {}
		try {
			uni.getFileSystemManager().unlinkSync(p)
		} catch (e) {}
	}
	try {
		uni.removeStorageSync(ST_REMOTE)
	} catch (e) {}
	try {
		uni.removeStorageSync(ST_LOCAL)
	} catch (e) {}
	try {
		uni.removeStorageSync(ST_LEGACY)
	} catch (e) {}
}

function fileExistsSync(path) {
	try {
		uni.getFileSystemManager().accessSync(path)
		return true
	} catch (e) {
		return false
	}
}

/**
 * 返回 <image> 可用的 src：远程地址在 App/小程序下载成功后用本地路径，失败则回退为绝对 URL。
 * H5 使用绝对 URL（依赖浏览器缓存）；服务端 URL 变更时会先清掉旧本地文件再重新下载。
 */
export function ensureAvatarCached(remoteUrl) {
	const remote = (remoteUrl || '').trim()
	if (isDefaultPlaceholder(remote)) {
		clearAvatarCache()
		return Promise.resolve(DEFAULT_AVATAR_URL)
	}
	if (remote.startsWith('data:') || remote.startsWith('blob:')) {
		return Promise.resolve(remote)
	}

	const canonical = toAbsoluteAvatarUrl(remote)

	// #ifdef H5
	return Promise.resolve(canonical)
	// #endif

	// #ifndef H5
	const storedRemote = uni.getStorageSync(ST_REMOTE)
	const storedLocal = uni.getStorageSync(ST_LOCAL)
	if (storedRemote === canonical && storedLocal) {
		if (fileExistsSync(storedLocal)) {
			return Promise.resolve(storedLocal)
		}
		clearAvatarCache()
	} else if (storedRemote && storedRemote !== canonical) {
		clearAvatarCache()
	}
	return new Promise((resolve) => {
		const headers = {}
		const token = uni.getStorageSync('token')
		if (token) {
			headers.Authorization = 'Bearer ' + token
		}
		uni.downloadFile({
			url: canonical,
			header: headers,
			success: (res) => {
				if (res.statusCode !== 200 || !res.tempFilePath) {
					resolve(canonical)
					return
				}
				uni.saveFile({
					tempFilePath: res.tempFilePath,
					success: (s) => {
						try {
							uni.setStorageSync(ST_REMOTE, canonical)
							uni.setStorageSync(ST_LOCAL, s.savedFilePath)
						} catch (e) {}
						resolve(s.savedFilePath)
					},
					fail: () => resolve(canonical)
				})
			},
			fail: () => resolve(canonical)
		})
	})
	// #endif
}
