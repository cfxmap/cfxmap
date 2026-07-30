/**
 * 金额数字格式化（不依赖 Intl.toLocaleString 的 options，兼容 Android WebView）
 */

export function formatFixed2(value) {
	const n = typeof value === 'string' ? parseFloat(value) : Number(value)
	if (!Number.isFinite(n)) return '0.00'
	return n.toFixed(2)
}

/** 代币等非法币数量统一四位小数 */
export function formatFixed4(value) {
	const n = typeof value === 'string' ? parseFloat(value) : Number(value)
	if (!Number.isFinite(n)) return '0.0000'
	return n.toFixed(4)
}

/** 千分位 + 两位小数，例如 1234.5 -> "1,234.50" */
export function formatMoneyEn(value) {
	const fixed = formatFixed2(value)
	const neg = fixed.startsWith('-')
	const core = neg ? fixed.slice(1) : fixed
	const [intPart, dec] = core.split('.')
	const intWithSep = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
	const num = `${intWithSep}.${dec || '00'}`
	return neg ? `-${num}` : num
}
