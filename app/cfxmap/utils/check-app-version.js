import { getBaseUrl } from './request.js'
import { getClientVersionName } from '../config/app-version.js'

function norm(v) {
	return (v == null ? '' : String(v)).trim()
}

/**
 * 启动时调用：若服务端要求版本与当前客户端不一致，则跳转强制更新页（不可返回）。
 * @returns {Promise<boolean>} 已跳转强制更新时为 true
 */
export function checkAppVersionOnLaunch() {
	return new Promise((resolve) => {
		// 浏览器/H5 不做升级拦截，只在原生 APP 端检测版本。
		// #ifndef APP-PLUS
		resolve(false)
		return
		// #endif

		try {
			const pages = typeof getCurrentPages === 'function' ? getCurrentPages() : []
			const cur = pages.length ? pages[pages.length - 1] : null
			if (cur && cur.route === 'pages/system/force-update') {
				resolve(false)
				return
			}
		} catch (e) {}

		const clientV = norm(getClientVersionName())
		uni.request({
			url: getBaseUrl() + '/api/app/version',
			method: 'GET',
			header: { 'Content-Type': 'application/json' },
			success: (res) => {
				if (res.statusCode !== 200 || !res.data || res.data.code !== 200) {
					resolve(false)
					return
				}
				const d = res.data.data || {}
				const serverV = norm(d.currentVersion)
				const officialUrl = norm(d.officialUrl)
				const apkUrl = norm(d.apkUrl)
				if (!serverV) {
					resolve(false)
					return
				}
				if (serverV === clientV) {
					resolve(false)
					return
				}
				const q =
					'url=' +
					encodeURIComponent(officialUrl) +
					'&apk=' +
					encodeURIComponent(apkUrl) +
					'&server=' +
					encodeURIComponent(serverV) +
					'&client=' +
					encodeURIComponent(clientV)
				uni.reLaunch({
					url: '/pages/system/force-update?' + q,
					complete: () => resolve(true)
				})
			},
			fail: () => resolve(false)
		})
	})
}
