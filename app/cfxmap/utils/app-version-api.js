import { getBaseUrl } from './request.js'

function normalizeVersion(value) {
	return value == null ? '' : String(value).trim()
}

export function fetchAppVersionInfo() {
	return new Promise((resolve) => {
		uni.request({
			url: getBaseUrl() + '/api/app/version',
			method: 'GET',
			header: {
				'Content-Type': 'application/json'
			},
			success: (res) => {
				if (res.statusCode === 200 && res.data && res.data.code === 200) {
					resolve(res.data.data || {})
					return
				}
				resolve(null)
			},
			fail: () => resolve(null)
		})
	})
}

export async function fetchServerVersionName() {
	const info = await fetchAppVersionInfo()
	return normalizeVersion(info && info.currentVersion)
}
