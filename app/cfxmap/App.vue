<script>
	import store, { getSystemThemeDark } from './store'
	import { checkAppVersionOnLaunch } from './utils/check-app-version.js'
	import { getStaticUrl } from './utils/static-resource.js'

	function runPostVersionLaunch() {
		const token = uni.getStorageSync('token');
		if (token && token !== 'mock_token') {
			uni.switchTab({
				url: '/pages/wallet/wallet'
			});
		} else if (token === 'mock_token') {
			uni.removeStorageSync('token');
		}
	}

	function syncChromeToTheme() {
		const dark = store.getters.isDarkMode
		// #ifdef H5
		if (typeof document !== 'undefined') {
			document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
		}
		// #endif
		try {
			uni.setNavigationBarColor({
				frontColor: dark ? '#ffffff' : '#000000',
				backgroundColor: dark ? '#101d36' : '#E2F5FC'
			})
		} catch (e) {}
		try {
			uni.setBackgroundColor({
				backgroundColor: dark ? '#152645' : '#eaf8fe',
				backgroundColorTop: dark ? '#020617' : '#b8e6f5',
				backgroundColorBottom: dark ? '#1e365c' : '#ffffff'
			})
		} catch (e) {}
		try {
			uni.setTabBarStyle({
				color: dark ? '#8B95B5' : '#64748B',
				selectedColor: dark ? '#7AA2FF' : '#33CFFF',
				backgroundColor: dark ? '#121c32' : '#F5FDFF',
				borderStyle: dark ? 'black' : 'white'
			})
		} catch (e) {}
	}

	export default {
		onLaunch: function() {
			console.log('App Launch')

			store.commit('SET_SYSTEM_PREFERS_DARK', getSystemThemeDark())
			syncChromeToTheme()
			store.subscribe((mutation) => {
				if (mutation.type === 'SET_THEME' || mutation.type === 'SET_SYSTEM_PREFERS_DARK') {
					syncChromeToTheme()
				}
			})

			// #ifdef MP-WEIXIN
			if (typeof uni.onThemeChange === 'function') {
				uni.onThemeChange((res) => {
					store.commit('SET_SYSTEM_PREFERS_DARK', res.theme === 'dark')
					syncChromeToTheme()
				})
			}
			// #endif

			// #ifdef H5
			if (typeof window !== 'undefined' && window.matchMedia) {
				const mq = window.matchMedia('(prefers-color-scheme: dark)')
				const onChange = () => {
					store.commit('SET_SYSTEM_PREFERS_DARK', mq.matches)
					syncChromeToTheme()
				}
				if (mq.addEventListener) {
					mq.addEventListener('change', onChange)
				} else if (mq.addListener) {
					mq.addListener(onChange)
				}
			}
			/* 预加载底部 Tab 图标，减轻切换时解码闪烁 */
			if (typeof Image !== 'undefined') {
				const tabIcons = [
					getStaticUrl('/static/bottom-nav/tab-wallet.png'),
					getStaticUrl('/static/bottom-nav/tab-wallet-active.png'),
					getStaticUrl('/static/bottom-nav/tab-guide.png'),
					getStaticUrl('/static/bottom-nav/tab-guide-active.png'),
					getStaticUrl('/static/bottom-nav/tab-profile.png'),
					getStaticUrl('/static/bottom-nav/tab-profile-active.png')
				]
				tabIcons.forEach((src) => {
					const img = new Image()
					img.src = src
				})
			}
			/* 预加载 Tab 页脚本，减轻首次 switchTab 时整屏黑/空白（H5 按需加载导致） */
			if (typeof uni.preloadPage === 'function') {
				const tabUrls = ['/pages/wallet/wallet', '/pages/community/community', '/pages/guide/conflux', '/pages/profile/profile']
				tabUrls.forEach((url, index) => {
					setTimeout(() => {
						try {
							uni.preloadPage({ url })
						} catch (e) {}
					}, 120 + index * 160)
				})
			}
			// #endif

			checkAppVersionOnLaunch().then((blocked) => {
				if (blocked) return
				runPostVersionLaunch()
			})
		},
		onShow: function() {
			console.log('App Show')
			store.commit('SET_SYSTEM_PREFERS_DARK', getSystemThemeDark())
			syncChromeToTheme()
		},
		onHide: function() {
			console.log('App Hide')
		}
	}
</script>

<style>
	/*每个页面公共css */
	/* 全局主题变量（勿把 page 与 .theme-light 绑在一起，否则 page 永远用浅色变量） */
	.theme-light {
		/* 浅色：纵向渐变，越往下越浅（底部接近白） */
		--page-bg-gradient: linear-gradient(180deg, #b8e6f5 0%, #d4f2fb 28%, #eaf8fe 62%, #ffffff 100%);
		--bg-color: #ffffff;
		--bg-elevated: #FFFFFF;
		--card-bg: rgba(255, 255, 255, 0.82);
		--card-bg-strong: #FFFFFF;
		--text-primary: #0F172A;
		--text-secondary: #64748B;
		--text-tertiary: #94A3B8;
		--border-color: rgba(216, 228, 255, 0.92);
		--control-bg: #EAF1FF;
		--segment-bg: rgba(234, 241, 255, 0.92);
		--segment-active-bg: #FFFFFF;
		--accent-color: #33cfff;
		--accent-color-strong: #5bddff;
		--warning-bg: rgba(255, 242, 245, 0.88);
		--warning-text: #E85B77;
		--success-color: #22C55E;
		--pending-color: #F59E0B;
		--glass-bg: rgba(255, 255, 255, 0.7);
		--glass-handle: rgba(147, 163, 194, 0.42);
		--card-shadow: 0 18rpx 52rpx rgba(51, 207, 255, 0.14);
		--card-shadow-sm: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
		--card-shadow-md: 0 12rpx 32rpx rgba(51, 207, 255, 0.16);
		--card-shadow-lg: 0 24rpx 60rpx rgba(51, 207, 255, 0.18);
		--btn-shadow: 0 18rpx 40rpx rgba(51, 207, 255, 0.32);
		--qr-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.08);
		--btn-apple-bg: #0F172A;
		--btn-apple-text: #FFFFFF;
		--btn-confirm-bg: #33cfff;
		--btn-text-white: #FFFFFF;
		--icon-bg: rgba(255, 255, 255, 0.7);
		--max-btn-bg: rgba(51, 207, 255, 0.14);
		--nav-icon-bg: rgba(255, 255, 255, 0.72);
		--card-border: 1px solid rgba(220, 231, 248, 0.96);
		--overlay-border: 4rpx solid #FFFFFF;
		--badge-bg: rgba(15, 23, 42, 0.62);
	}

	.theme-dark {
		/* 深色：对角主渐变 + 角部弱光晕，与主题强调色呼应 */
		--page-bg-gradient: radial-gradient(ellipse 130% 88% at 100% -12%, rgba(91, 140, 255, 0.18) 0%, transparent 52%), radial-gradient(ellipse 95% 72% at -8% 108%, rgba(51, 207, 255, 0.08) 0%, transparent 46%), linear-gradient(168deg, #020617 0%, #080f1c 14%, #0c1528 32%, #101d36 50%, #152645 68%, #1a2f52 86%, #1e365c 100%);
		--bg-color: #1a2f52;
		--bg-elevated: #101A30;
		/* 列表卡片仍用纯色以兼容 background-color；略透 + 偏青蓝，配合 blur 更接近资产卡 */
		--card-bg: rgba(28, 48, 82, 0.52);
		--card-bg-strong: #101A30;
		--text-primary: #F5F7FF;
		--text-secondary: #8B95B5;
		--text-tertiary: #6F7A9A;
		--border-color: rgba(255, 255, 255, 0.08);
		--control-bg: #162340;
		--segment-bg: rgba(22, 38, 68, 0.9);
		--segment-active-bg: #162340;
		--accent-color: #5B8CFF;
		--accent-color-strong: #7AA2FF;
		--warning-bg: rgba(38, 26, 36, 0.88);
		--warning-text: #FF738D;
		--success-color: #4ADE80;
		--pending-color: #FBBF24;
		/* 毛玻璃与总资产卡一致：冷色高光渐变 + 低不透明底，避免白雾叠层发灰 */
		--glass-tint: linear-gradient(135deg, rgba(80, 140, 200, 0.25), rgba(80, 140, 200, 0.02));
		--glass-base: rgba(16, 26, 48, 0.4);
		--glass-bg: var(--glass-tint), var(--glass-base);
		--glass-handle: rgba(160, 205, 255, 0.3);
		--card-shadow: 0 20rpx 60rpx rgba(0, 0, 0, 0.32);
		--card-shadow-sm: 0 10rpx 28rpx rgba(0, 0, 0, 0.24);
		--card-shadow-md: 0 12rpx 36rpx rgba(0, 0, 0, 0.28);
		--card-shadow-lg: 0 28rpx 70rpx rgba(0, 0, 0, 0.34);
		--btn-shadow: 0 18rpx 40rpx rgba(91, 140, 255, 0.2);
		--qr-shadow: 0 14rpx 34rpx rgba(0, 0, 0, 0.32);
		--btn-apple-bg: #F5F7FF;
		--btn-apple-text: #08101F;
		--btn-confirm-bg: #5B8CFF;
		--btn-text-white: #FFFFFF;
		--icon-bg: rgba(22, 35, 64, 0.9);
		--max-btn-bg: rgba(91, 140, 255, 0.16);
		--nav-icon-bg: rgba(22, 35, 64, 0.92);
		--card-border: 1px solid rgba(255, 255, 255, 0.08);
		--overlay-border: 4rpx solid #101A30;
		--badge-bg: rgba(8, 16, 31, 0.74);
		--ios-glass-bg: linear-gradient(180deg, rgba(34, 49, 78, 0.78), rgba(16, 26, 48, 0.62));
		--ios-glass-bg-soft: linear-gradient(180deg, rgba(32, 46, 74, 0.62), rgba(12, 22, 40, 0.48));
		--ios-glass-border: rgba(255, 255, 255, 0.12);
		--ios-glass-highlight: inset 0 1px 0 rgba(255, 255, 255, 0.14);
		--ios-glass-shadow: 0 18rpx 40rpx rgba(2, 8, 20, 0.34);
	}

	.theme-light,
	.theme-dark {
		color: var(--text-primary) !important;
		font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
		transition: background 0.4s ease, background-color 0.4s ease, color 0.35s ease;
	}

	/*
	 * 原生 page 无法挂 theme-dark，这里写死浅色默认 + html[data-theme=dark] 覆盖（H5）。
	 * 小程序依赖 setBackgroundColor + 根节点铺满渐变。
	 */
	page {
		font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
		transition: background 0.4s ease, background-color 0.4s ease;
		background: linear-gradient(180deg, #b8e6f5 0%, #d4f2fb 28%, #eaf8fe 62%, #ffffff 100%) !important;
	}

	/* #ifdef H5 */
	/* 根节点与 uni 页面栈背景与主题一致，避免 Tab 切换动画间隙透出黑底 */
	html {
		min-height: 100%;
		background: linear-gradient(180deg, #b8e6f5 0%, #d4f2fb 28%, #eaf8fe 62%, #ffffff 100%);
	}
	html[data-theme="dark"] {
		background: radial-gradient(ellipse 130% 88% at 100% -12%, rgba(91, 140, 255, 0.18) 0%, transparent 52%),
			radial-gradient(ellipse 95% 72% at -8% 108%, rgba(51, 207, 255, 0.08) 0%, transparent 46%),
			linear-gradient(168deg, #020617 0%, #080f1c 14%, #0c1528 32%, #101d36 50%, #152645 68%, #1a2f52 86%, #1e365c 100%);
	}
	body,
	#app {
		min-height: 100%;
		margin: 0;
		background: transparent;
	}
	/* 与 body 首屏底色一致，避免路由层默认黑底在转场时露出 */
	#app > uni-app,
	uni-app {
		display: block;
		width: 100%;
		min-height: 100%;
		background-color: #b8e6f5;
		background: linear-gradient(180deg, #b8e6f5 0%, #d4f2fb 28%, #eaf8fe 62%, #ffffff 100%);
	}
	html[data-theme="dark"] #app > uni-app,
	html[data-theme="dark"] uni-app {
		background-color: #101d36;
		background: radial-gradient(ellipse 130% 88% at 100% -12%, rgba(91, 140, 255, 0.18) 0%, transparent 52%),
			radial-gradient(ellipse 95% 72% at -8% 108%, rgba(51, 207, 255, 0.08) 0%, transparent 46%),
			linear-gradient(168deg, #020617 0%, #080f1c 14%, #0c1528 32%, #101d36 50%, #152645 68%, #1a2f52 86%, #1e365c 100%);
	}
	uni-page-wrapper,
	uni-page-body,
	uni-page {
		background-color: transparent !important;
		background: transparent !important;
	}
	html[data-theme="dark"] page,
	html[data-theme="dark"] uni-page-body {
		background: radial-gradient(ellipse 130% 88% at 100% -12%, rgba(91, 140, 255, 0.18) 0%, transparent 52%), radial-gradient(ellipse 95% 72% at -8% 108%, rgba(51, 207, 255, 0.08) 0%, transparent 46%), linear-gradient(168deg, #020617 0%, #080f1c 14%, #0c1528 32%, #101d36 50%, #152645 68%, #1a2f52 86%, #1e365c 100%) !important;
	}
	/* #endif */

	/* 浅色：透底露出 page 上的同一套渐变；保证至少一屏高，避免漏边 */
	view.container.theme-light,
	view.page-shell.theme-light {
		background: transparent;
		min-height: 100vh;
	}

	view.container.theme-dark,
	view.page-shell.theme-dark {
		background: var(--page-bg-gradient);
		min-height: 100vh;
	}

	.container,
	view.page-shell {
		position: relative;
		color: var(--text-primary);
	}

	/* #ifndef H5 */
	.container,
	view.page-shell {
		animation: page-enter 0.32s cubic-bezier(0.22, 1, 0.36, 1);
	}
	/* #endif */

	.asset-card,
	.asset-item,
	.card-qr,
	.card-form,
	.detail-card,
	.wallet-group,
	.user-card-wrapper,
	.list-group-wrapper,
	.warning-box,
	.nft-card,
	.tx-item,
	.empty-group,
	.card-wrapper,
	.export-result-wrapper,
	.footer-action .send-btn,
	.btn-login,
	.btn-confirm,
	.btn-logout {
		border: var(--card-border);
		box-shadow: var(--card-shadow);
		transition: transform 0.24s ease, box-shadow 0.24s ease, background-color 0.24s ease, border-color 0.24s ease, opacity 0.24s ease;
	}

	.asset-card,
	.card-qr,
	.card-form,
	.detail-card,
	.user-card-wrapper,
	.list-group-wrapper,
	.wallet-group,
	.card-wrapper,
	.export-result-wrapper,
	.warning-box {
		backdrop-filter: blur(28rpx);
		-webkit-backdrop-filter: blur(28rpx);
	}

	.asset-item:active,
	.nft-card:active,
	.tx-item:active,
	.list-item:active,
	.wallet-card-header:active,
	.btn-action-wrapper:active,
	.action-btn-wrapper:active,
	.tx-entry:active,
	.settings-btn:active {
		transform: scale(0.985);
	}

	.btn-login,
	.btn-confirm,
	.footer-action .send-btn,
	.btn-import {
		background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong)) !important;
		box-shadow: var(--btn-shadow) !important;
	}

	.send-code-btn,
	.max-btn,
	.btn-export,
	.action-btn-wrapper,
	.btn-action-wrapper {
		transition: transform 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
	}

	.segmented-control,
	.login-tabs,
	.tab-group-wrapper,
	.sub-tab-group {
		background: var(--segment-bg) !important;
		box-shadow: inset 0 0 0 1px var(--border-color);
		transition: background-color 0.24s ease, box-shadow 0.24s ease;
	}

	.segment.active,
	.tab-item.active,
	.sub-tab-item.active {
		background: var(--segment-active-bg) !important;
		box-shadow: var(--card-shadow-sm);
	}

	.tx-entry,
	.settings-btn {
		background: var(--nav-icon-bg) !important;
		border-radius: 999rpx;
		box-shadow: var(--card-shadow-sm);
		transition: transform 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
	}

	/* 钱包 / NFT 顶栏右上角交易入口：深色实心圆角按钮（与毛玻璃区分） */
	.theme-dark .header-group .tx-entry {
		background: linear-gradient(180deg, rgba(44, 62, 104, 0.98), rgba(22, 36, 64, 0.99)) !important;
		border: 1px solid rgba(255, 255, 255, 0.16) !important;
		border-radius: 16rpx !important;
		box-shadow:
			0 10rpx 28rpx rgba(0, 0, 0, 0.38),
			inset 0 1px 0 rgba(255, 255, 255, 0.12) !important;
		backdrop-filter: none !important;
		-webkit-backdrop-filter: none !important;
	}

	.logo,
	.avatar,
	.avatar-img {
		box-shadow: var(--card-shadow-md);
		transition: transform 0.28s ease, box-shadow 0.28s ease;
	}

	.logo:active,
	.avatar:active,
	.avatar-img:active {
		transform: scale(0.98);
	}

	.loader {
		box-shadow: 0 0 0 8rpx rgba(51, 207, 255, 0.12);
	}

	/* 不用 opacity:0，避免 H5 Tab 切换时透出底层黑底 */
	@keyframes page-enter {
		from {
			opacity: 1;
			transform: translateY(10rpx);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	/*
	 * 折叠屏展开 / 双屏拼合 / 平板：按可视宽度分段（约 1→2→3 倍常规手机逻辑宽），
	 * 主内容区 max-width + 居中，避免单列在超宽下被无限拉散。
	 * 依赖视口宽度变化（含 H5 resize、端内折叠态切换）；单位用 px 与 rpx 解耦。
	 */
	.page-shell,
	.container {
		width: 100%;
		box-sizing: border-box;
	}

	@media (min-width: 600px) {
		.page-shell,
		.container {
			max-width: 560px;
			margin-left: auto;
			margin-right: auto;
		}
	}

	@media (min-width: 900px) {
		.page-shell,
		.container {
			max-width: 760px;
		}
	}

	@media (min-width: 1200px) {
		.page-shell,
		.container {
			max-width: 1020px;
		}
	}
</style>
