/**
 * 浏览器环境无 Node 的 process；部分 Vue/uni H5 运行时会访问 process.env。
 * 须在入口其它 import 之前执行（见 main.js 条件 import）。
 */
if (typeof globalThis !== 'undefined' && typeof globalThis.process === 'undefined') {
	globalThis.process = { env: { NODE_ENV: 'production' } }
}
if (typeof window !== 'undefined' && typeof window.process === 'undefined' && typeof globalThis !== 'undefined') {
	window.process = globalThis.process
}
