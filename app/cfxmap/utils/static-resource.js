const OSS_STATIC_BASE = 'https://cfxmap.oss-cn-hongkong.aliyuncs.com/static'
const ABSOLUTE_URL_RE = /^(?:[a-z]+:)?\/\//i

export function getStaticUrl(path = '') {
	const raw = String(path || '').trim()
	if (!raw) return ''
	if (raw.startsWith('data:') || raw.startsWith('blob:') || ABSOLUTE_URL_RE.test(raw)) {
		return raw
	}

	let relativePath = raw.replace(/^\/+/, '')
	if (relativePath.startsWith('static/')) {
		relativePath = relativePath.slice('static/'.length)
	}

	// #ifdef H5
	return `${OSS_STATIC_BASE}/${relativePath}`
	// #endif

	return `/static/${relativePath}`
}
