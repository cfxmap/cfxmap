/**
 * 与 manifest.json 中 versionName 保持一致。
 * 当运行环境无法读取实际版本时（如部分模拟器），将使用该值参与比对。
 */
export const APP_VERSION_FALLBACK = '1.0.0'

export function formatDisplayVersion(version) {
	const value = version == null ? '' : String(version).trim()
	if (!value) return ''
	return /^[vV]/.test(value) ? value : `V${value}`
}

export function getClientVersionName() {
	// #ifdef APP-PLUS
	try {
		if (typeof plus !== 'undefined' && plus.runtime && plus.runtime.version) {
			const v = String(plus.runtime.version).trim()
			if (v) return v
		}
	} catch (e) {}
	// #endif
	try {
		const info = uni.getSystemInfoSync()
		if (info && info.appVersion) {
			const v = String(info.appVersion).trim()
			if (v) return v
		}
	} catch (e2) {}
	return APP_VERSION_FALLBACK
}
