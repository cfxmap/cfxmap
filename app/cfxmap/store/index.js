import { createStore } from 'vuex'

/** 系统是否处于深色（小程序 theme / H5 prefers-color-scheme） */
export function getSystemThemeDark() {
	try {
		const info = uni.getSystemInfoSync()
		if (info && info.theme === 'dark') return true
		if (info && info.theme === 'light') return false
	} catch (e) {}
	// #ifdef H5
	if (typeof window !== 'undefined' && window.matchMedia) {
		try {
			return window.matchMedia('(prefers-color-scheme: dark)').matches
		} catch (e2) {}
	}
	// #endif
	return false
}

function normalizeLanguageCode(language) {
	return String(language || '').trim().toLowerCase().replace(/_/g, '-')
}

function isChineseLanguage(language) {
	const normalized = normalizeLanguageCode(language)
	return normalized === 'zh' || normalized.startsWith('zh-')
}

/** 获取系统语言：中文默认中文，其它默认英文 */
export function getDefaultLanguage() {
	let systemLanguage = ''

	try {
		const info = uni.getSystemInfoSync()
		systemLanguage = info && info.language ? info.language : ''
	} catch (e) {}

	// #ifdef APP-PLUS
	if (!systemLanguage && typeof plus !== 'undefined' && plus.os) {
		try {
			systemLanguage = plus.os.language || ''
		} catch (e2) {}
	}
	// #endif

	// #ifdef H5
	if (!systemLanguage && typeof navigator !== 'undefined') {
		systemLanguage = navigator.language || navigator.browserLanguage || ''
	}
	// #endif

	return isChineseLanguage(systemLanguage) ? '中文' : 'English'
}

const store = createStore({
	state: {
		language: uni.getStorageSync('language') || getDefaultLanguage(), // 'English' | '中文'
		currency: uni.getStorageSync('currency') || 'USD',     // 'USD' | 'CNY'
		theme: uni.getStorageSync('theme') || 'Auto',          // 'Auto' | 'Light' | 'Dark'
		systemPrefersDark: getSystemThemeDark(),
		/**
		 * 个人中心修改头像成功后写入，供「我的」等 tab 即时展示；
		 * 资料拉取成功后会 clear，避免与接口长期脱节。
		 */
		profileAvatarForcedSrc: null
	},
	mutations: {
		SET_LANGUAGE(state, lang) {
			state.language = lang
			uni.setStorageSync('language', lang)
		},
		SET_CURRENCY(state, curr) {
			state.currency = curr
			uni.setStorageSync('currency', curr)
		},
		SET_THEME(state, theme) {
			state.theme = theme
			uni.setStorageSync('theme', theme)
		},
		SET_SYSTEM_PREFERS_DARK(state, dark) {
			state.systemPrefersDark = !!dark
		},
		SET_PROFILE_AVATAR_FORCED(state, src) {
			state.profileAvatarForcedSrc = src || null
		},
		CLEAR_PROFILE_AVATAR_FORCED(state) {
			state.profileAvatarForcedSrc = null
		}
	},
	getters: {
		/** 当前界面是否按深色渲染（含 Auto + 系统深色） */
		isDarkMode(state) {
			if (state.theme === 'Dark') return true
			if (state.theme === 'Light') return false
			return state.systemPrefersDark
		},
		themeClass(state, getters) {
			return getters.isDarkMode ? 'theme-dark' : 'theme-light'
		},
		i18n: state => {
			const isZh = state.language === '中文'
			return {
				// Login
				loginTitle: isZh ? '欢迎来到 Web3' : 'Welcome to Web3',
				loginSloganTitle: isZh ? 'CFXMAP' : 'CFXMAP',
				loginSloganSub: isZh ? '通往去中心化世界的入口' : 'Your Gateway to Decentralization',
				oneClickLogin: isZh ? '本机号码一键登录' : 'One-Click Login',
				smsLogin: isZh ? '短信验证码登录' : 'SMS Login',
				appleLogin: isZh ? '使用 Apple 登录' : 'Sign in with Apple',
				codeLoginTab: isZh ? '验证码登录' : 'Code Login',
				pwdLoginTab: isZh ? '密码登录' : 'Password Login',
				registerTab: isZh ? '注册' : 'Register',
				emailAddress: isZh ? '邮箱地址' : 'Email Address',
				emailCode: isZh ? '邮箱验证码' : 'Email Code',
				password: isZh ? '密码' : 'Password',
				confirmPasswordLabel: isZh ? '确认密码' : 'Confirm Password',
				notReceivedCode: isZh ? '未收到验证码？重新发送' : 'Not received? Resend',
				forgotPassword: isZh ? '忘记密码？' : 'Forgot Password?',
				noAccountYet: isZh ? '还没有账号？' : "Don't have an account yet?",
				haveAccountAlready: isZh ? '已有账号？' : 'Already have an account?',
				registerBtn: isZh ? '注 册' : 'Register',
				registerTitle: isZh ? '创建账号' : 'Create account',
				registerDesc: isZh
					? '使用邮箱和密码即可完成注册，注册成功后会收到一封通知邮件。'
					: 'Create an account with email and password. A registration confirmation email will be sent after success.',
				registerSuccess: isZh ? '注册成功' : 'Registered successfully',
				registering: isZh ? '注册中...' : 'Creating account...',
				forgotPasswordTitle: isZh ? '忘记密码' : 'Forgot password',
				forgotPasswordDesc: isZh
					? '请输入注册邮箱，通过邮件验证码设置新密码。'
					: 'Enter your registered email. We will send a code to set a new password.',
				passwordAtLeast6Chars: isZh ? '密码至少6位' : 'Password must be at least 6 characters',
				resetPwdBtn: isZh ? '重置密码' : 'Reset password',
				loginBtn: isZh ? '登 录' : 'Log In',
				sendCode: isZh ? '发送验证码' : 'Send Code',
				codeLoginHintBeforeSend: isZh ? '请输入邮箱后发送验证码' : 'Enter your email, then send the verification code',
				codeLoginHintAfterSend: isZh ? '验证码已发送，可继续前往下一步' : 'Code sent. Continue to the next step.',
				sendCodeResendIn: isZh ? '{n}秒后可重发' : 'Resend in {n}s',
				imageCaptchaPlaceholder: isZh ? '图形验证码' : 'Image captcha',
				captchaRefresh: isZh ? '刷新' : 'Refresh',
				captchaModalHint: isZh ? '点击图片可刷新，请输入图中字符' : 'Tap the image to refresh, then type the characters shown',

				// Verify code page (email login)
				verifyCodeTitle: isZh ? '输入验证码' : 'Enter verification code',
				verifyCodeSentTo: isZh ? '验证码已发送至 {email}' : 'Code sent to {email}',
				verifyCodePlaceholder: isZh ? '请输入 4 位数字验证码' : 'Enter the 4-digit code',
				verifyCodeAutoLoginTip: isZh ? '输入 4 位数字后将自动登录' : 'You will be signed in automatically after 4 digits',
				verifyCodeNotReceived: isZh ? '没有收到验证码？' : "Didn't receive a code?",
				resendCode: isZh ? '重新发送' : 'Resend',
				pleaseEnterEmail: isZh ? '请输入邮箱' : 'Please enter your email',
				emailCannotBeEmpty: isZh ? '邮箱不能为空' : 'Email is required',
				pleaseEnterImageCaptcha: isZh ? '请输入图形验证码' : 'Please enter the image captcha',
				emailCodeSent: isZh ? '验证码已发送' : 'Verification code sent',
				loggingIn: isZh ? '登录中...' : 'Signing in...',
				loginSuccessNoCredential: isZh ? '登录成功但未获取到有效凭证' : 'Signed in but no valid credential was returned',
				biometricNotSupportedPlatform: isZh ? '当前平台不支持指纹登录' : 'Fingerprint login is not supported on this platform',
				biometricCapabilityUnavailable: isZh ? '设备未开启指纹能力' : 'Fingerprint is not available on this device',
				enrollFingerprintInSystem: isZh ? '请先在系统中录入指纹' : 'Please enroll a fingerprint in system settings first',
				fingerprintAuthFailed: isZh ? '指纹验证未通过' : 'Fingerprint verification failed',
				enableFingerprintLoginTitle: isZh ? '开启指纹登录' : 'Enable fingerprint login',
				enableFingerprintLoginMessage: isZh ? '下次登录时可直接使用指纹验证，是否现在开启？' : 'Sign in faster next time with fingerprint. Enable now?',
				verifyFingerprintToEnableQuickLogin: isZh ? '请验证指纹以开启快捷登录' : 'Verify your fingerprint to enable quick login',
				fingerprintLoginEnabled: isZh ? '指纹登录已开启' : 'Fingerprint login enabled',

				// TabBar & Common
				walletTitle: isZh ? '钱包' : 'Wallet',
				communityTitle: isZh ? '社区' : 'Community',
				guideTitle: isZh ? '指南' : 'Guide',
				nftTitle: isZh ? 'NFT展示' : 'NFTs',
				profileTitle: isZh ? '我的' : 'Profile',
				tokens: isZh ? '代币' : 'Tokens',
				transactions: isZh ? '交易记录' : 'Transactions',
				transactionProgress: isZh ? '交易进度' : 'Transaction Progress',

				// Wallet
				totalBalance: isZh ? '总资产' : 'Total Balance',
				sendTitle: isZh ? '转出' : 'Send',
				receiveTitle: isZh ? '接收' : 'Receive',
				addressCopied: isZh ? '地址已复制' : 'Address Copied',

				// Profile
				accountSettings: isZh ? '账号设置' : 'Account Settings',
				securityTitle: isZh ? '个人中心' : 'Personal Center',
				custodyWalletSectionTitle: isZh ? '托管说明' : 'Custody Notice',
				custodyWalletTitle: isZh ? '托管钱包说明' : 'Custody Wallet Notice',
				custodyWalletEntryDesc: isZh
					? '查看当前钱包模式、平台保管边界、资产迁移方式和常见问题。'
					: 'View the wallet model, platform custody boundaries, migration path, and common questions.',
				custodyWalletBadge: isZh ? '当前模式：托管钱包' : 'Current Mode: Custodial Wallet',
				custodyWalletHeroTitle: isZh ? '请先了解当前钱包的控制边界' : 'Understand the control boundaries of this wallet',
				custodyWalletHeroDesc: isZh
					? '当前钱包由平台服务协助创建和托管，私钥/助记词默认不在你的设备本地生成。继续使用前，请确认你理解备份、恢复和退出机制。'
					: 'This wallet is created and custody-assisted by the platform. Private keys and mnemonics are not generated locally on your device by default. Please review the backup, recovery, and exit options before continuing.',
				custodyWalletModalTitle: isZh ? '托管钱包安全提示' : 'Custody Wallet Notice',
				custodyWalletModalContent: isZh
					? '当前账户默认使用托管钱包。私钥/助记词并非默认仅保存在本地设备。请尽快阅读托管说明，并按需导出私钥或助记词完成备份。'
					: 'This account currently uses a custodial wallet. Private keys and mnemonics are not stored only on your local device by default. Please review the custody notice and export your private key or mnemonic if you need an independent backup.',
				custodyWalletViewNoticeBtn: isZh ? '查看说明' : 'View Notice',
				custodyWalletLaterBtn: isZh ? '稍后查看' : 'Later',
				phoneBinding: isZh ? '手机绑定' : 'Phone Binding',
				appSettings: isZh ? '应用设置' : 'App Settings',
				language: isZh ? '语言' : 'Language',
				currency: isZh ? '币种' : 'Currency',
				theme: isZh ? '主题' : 'Theme',
				
				// Setting Values
				currencyUSD: isZh ? '美元 (USD)' : 'USD',
				currencyCNY: isZh ? '人民币 (CNY)' : 'CNY',
				themeAuto: isZh ? '跟随系统' : 'Auto',
				themeLight: isZh ? '浅色' : 'Light',
				themeDark: isZh ? '深色' : 'Dark',

				logout: isZh ? '退出登录' : 'Log Out',
				logoutTitle: isZh ? '退出登录' : 'Log Out',
				logoutConfirm: isZh ? '确定要退出登录吗？' : 'Are you sure you want to log out?',

				// Personal Center
				personalInfo: isZh ? '个人信息' : 'PERSONAL INFO',
				avatar: isZh ? '头像' : 'Avatar',
				changeUsername: isZh ? '修改用户名' : 'Change Username',
				security: isZh ? '安全' : 'SECURITY',
				changePassword: isZh ? '修改密码' : 'Change Password',

				// NFT Detail
				properties: isZh ? '属性' : 'Properties',
				sendNftBtn: isZh ? '发送 NFT' : 'Send NFT',

				// Receive
				yourAddress: isZh ? '您的地址' : 'Your Address',
				copy: isZh ? '复制' : 'Copy',
				share: isZh ? '分享' : 'Share',
				receiveWarning: isZh ? '仅可向此地址发送 CFX/NFT。发送其他资产将导致永久丢失。' : 'Send only CFX/NFT to this address. Sending other assets will result in permanent loss.',
				copied: isZh ? '已复制' : 'Copied',

				// Send
				sendNft: isZh ? '发送 NFT' : 'Send NFT',
				sendCrypto: isZh ? '发送加密货币' : 'Send Crypto',
				amount: isZh ? '数量' : 'Amount',
				balance: isZh ? '余额: ' : 'Balance: ',
				max: isZh ? '全部' : 'MAX',
				toAddress: isZh ? '接收地址' : 'To Address',
				gasFee: isZh ? '预估矿工费' : 'Estimated Gas Fee',
				confirmSend: isZh ? '确认发送' : 'Confirm Send',
				fromWallet: isZh ? '转出钱包' : 'From wallet',
				selectToken: isZh ? '选择代币' : 'Select token',
				selectWalletPlaceholder: isZh ? '请选择钱包' : 'Select wallet',
				selectTokenPlaceholder: isZh ? '请选择代币' : 'Select token',
				sending: isZh ? '发送中...' : 'Sending...',
				scanQr: isZh ? '扫码' : 'Scan',
				scanFailed: isZh ? '扫描失败' : 'Scan failed',
				pleaseEnterToAddress: isZh ? '请输入接收地址' : 'Please enter recipient address',
				pleaseEnterAmount: isZh ? '请输入金额' : 'Please enter amount',
				invalidAmount: isZh ? '请输入有效的金额' : 'Please enter a valid amount',
				amountExceedsBalance: isZh ? '输入金额不能超过当前余额' : 'Amount exceeds balance',
				sendSuccess: isZh ? '发送成功' : 'Sent successfully',
				txSent: isZh ? '交易已发送' : 'Transaction Sent',
				noAvailableTokens: isZh ? '没有可用代币' : 'No available tokens',
				sendTransferOutHint: isZh
					? '转出提示：链上确认需要时间，到账可能有延迟；请核对接收地址，误发无法撤回。'
					: 'Outgoing transfers need on-chain confirmation and may arrive with a delay. Verify the recipient address—mistaken sends cannot be reversed.',

				// Common list states
				loading: isZh ? '加载中...' : 'Loading...',
				noMore: isZh ? '没有更多了' : 'No more',
				mainWallet: isZh ? '主钱包' : 'Main Wallet',
				hidden: isZh ? '已隐藏' : 'Hidden',
				received: isZh ? '接收' : 'Received',
				sent: isZh ? '发送' : 'Sent',
				unknown: isZh ? '未知' : 'Unknown',
				cfxTestnet: isZh ? 'CFX 测试网' : 'CFX Testnet',

				// Transactions page
				loadingTransactions: isZh ? '加载交易中...' : 'Loading transactions...',
				noTransactionsFound: isZh ? '暂无交易记录' : 'No transactions found.',
				noTransactionsInWallet: isZh ? '该钱包暂无交易' : 'No transactions in this wallet.',
				swipeUpToLoadMore: isZh ? '上滑加载更多' : 'Swipe up to load more',
				txStatusCompleted: isZh ? '已完成' : 'Completed',
				txStatusSuccess: isZh ? '成功' : 'Success',
				txStatusPending: isZh ? '处理中' : 'Pending',
				txStatusFailed: isZh ? '失败' : 'Failed',
				txStageInitiated: isZh ? '已发起' : 'Initiated',
				txStageSigned: isZh ? '已签名/已校验' : 'Signed / Verified',
				txStageBroadcasted: isZh ? '已广播' : 'Broadcasted',
				txStageConfirming: isZh ? '链上确认中' : 'Confirming On-chain',
				txStageSuccess: isZh ? '已成功' : 'Succeeded',
				txStageFailure: isZh ? '已失败' : 'Failed',
				txCurrentStatus: isZh ? '当前状态' : 'Current Status',
				txHashLabel: isZh ? '交易哈希' : 'Transaction Hash',
				txFromAddress: isZh ? '转出地址' : 'From Address',
				txToAddress: isZh ? '转入地址' : 'To Address',
				txNetwork: isZh ? '所在网络' : 'Network',
				txGasFee: isZh ? '手续费' : 'Gas Fee',
				txBlockTime: isZh ? '区块时间' : 'Block Time',
				txBlockHeight: isZh ? '区块高度' : 'Block Height',
				txBlockHash: isZh ? '区块哈希' : 'Block Hash',
				txFailureReason: isZh ? '失败原因' : 'Failure Reason',
				viewOnExplorer: isZh ? '查看区块浏览器' : 'View On Explorer',
				copyTxHash: isZh ? '复制哈希' : 'Copy Hash',
				copyAddressBtn: isZh ? '复制地址' : 'Copy Address',
				openExplorerFailed: isZh ? '无法打开区块浏览器' : 'Failed to open explorer',
				txProgressPendingHint: isZh ? '交易已提交，正在等待链上确认。' : 'The transaction has been submitted and is waiting for confirmation.',
				backToWallet: isZh ? '返回钱包首页' : 'Back to Wallet',
				viewAllTransactions: isZh ? '查看全部交易' : 'View All Transactions',
				assetDetailTitle: isZh ? '资产详情' : 'Asset Details',
				assetHistoryTitle: isZh ? '变动明细' : 'History',
				assetFilterAll: isZh ? '全部' : 'All',
				assetFilterIn: isZh ? '转入' : 'Received',
				assetFilterOut: isZh ? '转出' : 'Sent',
				assetFilterFailed: isZh ? '失败' : 'Failed',
				assetFilterPending: isZh ? '处理中' : 'Pending',
				noAssetTransactions: isZh ? '该资产暂无明细' : 'No activity for this asset.',

				// NFT
				noNftsFound: isZh ? '暂无 NFT' : 'No NFTs found.',

				// Wallet manage / Security center (临时补齐，避免切换语言不一致)
				manageWallet: isZh ? '钱包管理' : 'Manage Wallet',
				createWallet: isZh ? '创建钱包' : 'Create Wallet',
				walletName: isZh ? '钱包名称' : 'Wallet Name',
				createWalletBtn: isZh ? '创建钱包' : 'Create Wallet',
				pleaseEnterWalletName: isZh ? '请输入钱包名称' : 'Please enter wallet name',
				createSuccessful: isZh ? '创建成功' : 'Created successfully',
				createFailed: isZh ? '创建失败' : 'Create failed',
				createdNewWallet: isZh ? '创建了新钱包' : 'Created New Wallet',
				importExportWallet: isZh ? '导入/导出钱包' : 'Import / Export Wallet',
				importWalletMenu: isZh ? '导入钱包' : 'Recover Wallet',
				exportWalletMenu: isZh ? '导出钱包' : 'Export Wallet',
				importWallet: isZh ? '导入钱包' : 'Recover Wallet',
				exportWallet: isZh ? '导出钱包' : 'Export Wallet',
				confluxCore: isZh ? 'Conflux Core (CFX)' : 'Conflux Core (CFX)',
				confluxCoreTestnet: isZh ? 'Conflux Core 测试网' : 'Conflux Core Testnet',
				derivedInBackend: isZh ? 'N/A (后端派生)' : 'N/A (Derived in backend)',
				privateKey: isZh ? '私钥' : 'Private Key',
				mnemonic: isZh ? '助记词' : 'Mnemonic',
				exportPrivateKeyBtn: isZh ? '导出私钥' : 'Export Private Key',
				exportMnemonicBtn: isZh ? '导出助记词' : 'Export Mnemonic',
				selectWallet: isZh ? '选择钱包' : 'Select Wallet',
				selectWalletFirst: isZh ? '请先选择钱包' : 'Please select a wallet first',
				securityWarningSecret: isZh ? '请妥善保管私钥/助记词，勿泄露给他人。' : 'Keep your private key/mnemonic safe. Never share it.',
				selectNetwork: isZh ? '选择网络' : 'Select Network',
				walletNameOptional: isZh ? '钱包名称（可选）' : 'Wallet Name (Optional)',
				walletNamePlaceholder: isZh ? '例如：我的金库' : 'e.g. My Vault',
				walletNameMaxLen: isZh ? '钱包名称最长20个字符' : 'Wallet name: max 20 characters',
				enterPrivateKey: isZh ? '请输入私钥' : 'Enter your Private Key',
				enterMnemonic: isZh ? '请输入助记词' : 'Enter your Mnemonic Phrase',
				privateKeyPlaceholder: isZh ? '例如：0x1234...' : 'e.g. 0x1234...',
				mnemonicPlaceholder: isZh ? '例如：abandon amount bottom catch...' : 'abandon amount bottom catch...',
				importWalletBtn: isZh ? '导入钱包' : 'Recover Wallet',
				restoreWalletDesc: isZh
					? '从助记词或私钥恢复已有地址，不会新生成链上地址，只会把该地址关联到当前 APP 账户。'
					: 'Recover an existing address from a mnemonic or private key. This does not create a new on-chain address; it only links the address to the current app account.',
				restoreMnemonicDesc: isZh
					? '输入助记词后先预览派生地址，再选择要恢复的地址。'
					: 'Preview derived addresses from the mnemonic first, then choose which address to recover.',
				restorePrivateKeyDesc: isZh
					? '私钥恢复会直接关联该私钥对应的地址，不会新生成地址。'
					: 'Private-key recovery links the address behind that key directly and does not create a new address.',
				previewAddressesBtn: isZh ? '预览地址' : 'Preview Addresses',
				previewingAddresses: isZh ? '预览中...' : 'Previewing...',
				previewAddressesFirst: isZh ? '请先预览助记词地址' : 'Preview mnemonic addresses first',
				selectDerivedAddress: isZh ? '选择要恢复的地址' : 'Choose Address to Recover',
				selectDerivedAddressFirst: isZh ? '请选择要恢复的地址' : 'Please choose an address to recover',
				derivedPathLabel: isZh ? '派生路径' : 'Derivation Path',
				currentSelectedAddress: isZh ? '当前恢复地址' : 'Selected Address',
				addressAlreadyImported: isZh ? '该地址已在当前账户中' : 'Already in this account',
				addressAlreadyImportedHint: isZh
					? '该地址已经关联到当前 APP 账户，无需重复恢复。'
					: 'This address is already linked to the current app account.',
				addressPreviewEmpty: isZh ? '暂无可恢复地址' : 'No derived addresses available',
				exportCurrentWallet: isZh ? '导出当前钱包' : 'Export Current Wallet',
				selectWalletToExport: isZh ? '选择要导出的钱包' : 'Choose a wallet to export',
				systemProvisionedWallet: isZh ? '系统新建' : 'System-created',
				systemProvisionedWalletHint: isZh
					? '注册时由系统自动创建的钱包，可导出私钥与助记词备份。'
					: 'Created automatically at sign-up; you may export private key and mnemonic to back it up.',
				walletManageCustodyHint: isZh
					? '当前钱包为平台托管模式。建议先查看托管说明，再根据需要导出私钥或助记词完成自主管理备份。'
					: 'This wallet currently uses platform custody. Review the custody notice first, then export the private key or mnemonic if you need a self-managed backup.',
				exportNotAvailableForWallet: isZh ? '该钱包未开放密钥导出' : 'Export is not enabled for this wallet',
				walletAddressLabel: isZh ? '钱包地址' : 'Wallet address',
				exportDesc: isZh ? '通过导出私钥或助记词来备份你的钱包。' : 'Backup your wallet by exporting the private key or mnemonic phrase.',
				exportWalletBtn: isZh ? '导出钱包' : 'Export Wallet',
				exportResultTitle: isZh ? '导出结果' : 'Export Result',
				close: isZh ? '关闭' : 'Close',
				ethAddress: isZh ? 'ETH 地址：' : 'ETH Address:',
				cfxMainnet: isZh ? 'CFX（主网）：' : 'CFX (Mainnet):',
				cfxTestnet: isZh ? 'CFX（测试网）：' : 'CFX (Testnet):',
				btcAddress: isZh ? 'BTC 地址：' : 'BTC Address:',
				privateKeyLabel: isZh ? '私钥：' : 'Private Key:',
				mnemonicLabel: isZh ? '助记词：' : 'Mnemonic:',
				pleaseEnterData: isZh ? '请输入内容' : 'Please enter data',
				importing: isZh ? '导入中...' : 'Importing...',
				importSuccessful: isZh ? '导入成功' : 'Import successful',
				importFailed: isZh ? '格式无效或导入失败' : 'Invalid format or import failed',
				tapToCopy: isZh ? '点击复制' : 'Tap to copy',
				exportSuccessful: isZh ? '导出成功' : 'Export successful',
				noWalletToExport: isZh ? '没有可导出的钱包' : 'No wallet to export',
				removeWallet: isZh ? '移除钱包' : 'Remove Wallet',
				removeWalletDesc: isZh
					? '仅可移除自行创建或导入的钱包。系统注册时自动创建的钱包不可移除。'
					: 'You can only remove wallets you created or imported. System-provisioned wallets cannot be removed.',
				removeWalletConfirmTitle: isZh ? '确认移除钱包？' : 'Remove this wallet?',
				removeWalletConfirmContent: isZh
					? '移除后链上资产不受影响，但本账号将无法再使用该钱包入口。请确认已备份私钥或助记词。'
					: 'On-chain assets are unaffected, but this account will no longer list this wallet. Ensure you have backed up the key or mnemonic.',
				removeWalletSuccess: isZh ? '已移除' : 'Wallet removed',
				removeWalletNoEligible: isZh ? '没有可移除的钱包' : 'No removable wallets',
				removeWalletBtn: isZh ? '从账号中移除' : 'Remove from account',
				importedViaMnemonic: isZh ? '通过助记词导入' : 'Imported via Mnemonic',
				importedViaPrivateKey: isZh ? '通过私钥导入' : 'Imported via Private Key',
				custodyWalletFaqTitle: isZh ? '帮助与 FAQ' : 'Help & FAQ',
				custodyWalletTypeTitle: isZh ? '1. 当前钱包类型' : '1. Wallet Type',
				custodyWalletTypeBody: isZh
					? '当前钱包属于托管钱包。钱包创建、保管和恢复流程依赖平台服务，不等同于完全由用户本地掌控的非托管钱包。'
					: 'This is a custodial wallet. Creation, custody, and recovery rely on platform services, which is different from a fully self-custodied wallet controlled only from the user device.',
				custodyWalletKeyTitle: isZh ? '2. 私钥与助记词由谁生成和保存' : '2. Who Generates and Stores Keys',
				custodyWalletKeyBody: isZh
					? '私钥/助记词默认由平台服务协助生成与保管，并非默认仅保存在你的本地设备。若你需要完全自主管理，请尽快在钱包管理页导出并离线备份。'
					: 'Private keys and mnemonics are generated and stored with platform assistance by default, rather than only on your local device. If you need full self-control, export them from wallet management and back them up offline as soon as possible.',
				custodyWalletRecoveryTitle: isZh ? '3. 平台是否能协助恢复' : '3. Platform Recovery Support',
				custodyWalletRecoveryBody: isZh
					? '在托管模式下，平台可在服务正常运行且校验通过的前提下协助恢复钱包访问。但恢复依赖账号验证、服务状态和平台运营能力，不代表链上资产风险由平台全额承担。'
					: 'Under custodial mode, the platform may help recover wallet access while services are available and verification succeeds. Recovery depends on account verification, service availability, and operational support, and does not mean the platform fully assumes all on-chain asset risks.',
				custodyWalletShutdownTitle: isZh ? '4. 如果平台停止服务怎么办' : '4. If the Platform Stops Service',
				custodyWalletShutdownBody: isZh
					? '若平台计划停服，将优先提供公告期和迁移窗口。你应在此期间将资产转出到外部自托管地址，或提前导出私钥/助记词，避免在服务不可用后失去迁移入口。'
					: 'If the platform plans to shut down, an announcement period and migration window should be provided first. During that time, transfer assets to an external self-custodied address or export your private key/mnemonic in advance so you do not lose the migration path after services end.',
				custodyWalletExportTitle: isZh ? '5. 是否可以导出密钥' : '5. Can You Export Keys',
				custodyWalletExportBody: isZh
					? '支持导出的钱包可在“钱包管理 > 导出钱包”中查看并备份私钥或助记词。导出后请立即离线保存，不要截图、云盘明文存储或转发给他人。'
					: 'For wallets with export enabled, go to "Wallet Management > Export Wallet" to back up the private key or mnemonic. Store exports offline immediately and avoid screenshots, plaintext cloud storage, or sharing them with anyone else.',
				custodyWalletRiskTitle: isZh ? '6. 风险边界与责任边界' : '6. Risk and Responsibility Boundary',
				custodyWalletRiskBody: isZh
					? '平台会尽力保障服务安全与可用性，但无法对链上转账误操作、钓鱼、恶意授权、用户自行泄露密钥等风险承担全部责任。请在转账、授权和备份时自行完成二次确认。'
					: 'The platform works to keep services secure and available, but cannot take full responsibility for mistaken transfers, phishing, malicious approvals, or user-caused secret leakage. Double-check transfers, approvals, and backups on your side.',
				custodyWalletExitTitle: isZh ? '7. 建议的退出与备份方式' : '7. Recommended Exit and Backup Path',
				custodyWalletExitBody: isZh
					? '建议至少完成以下一项：导出私钥、导出助记词，或将主要资产提币到你自有的非托管钱包地址。只依赖单一平台托管会增加不可用时的迁移风险。'
					: 'Complete at least one of these actions: export the private key, export the mnemonic, or withdraw major assets to your own non-custodial wallet address. Relying on a single custodial platform increases migration risk if it becomes unavailable.',
				custodyWalletFaq1Question: isZh ? '我还能把资产转到自己的钱包吗？' : 'Can I move assets to my own wallet?',
				custodyWalletFaq1Answer: isZh
					? '可以。最稳妥的方式是将资产转到你自行掌握助记词的外部钱包，或先导出私钥/助记词后再导入到其他钱包应用。'
					: 'Yes. The safest option is to transfer assets to an external wallet whose mnemonic you control, or export the private key/mnemonic first and import it into another wallet app.',
				custodyWalletFaq2Question: isZh ? '导出助记词后还需要看这页说明吗？' : 'Do I still need this notice after exporting the mnemonic?',
				custodyWalletFaq2Answer: isZh
					? '需要。导出只是给你增加了一份可自主控制的备份，并不改变历史上曾依赖平台托管的事实。你仍应及时验证备份是否可用，并规划资产迁移。'
					: 'Yes. Exporting only gives you a self-controlled backup and does not change the fact that the wallet previously relied on platform custody. You should still verify that the backup works and plan your migration.',
				custodyWalletFaq3Question: isZh ? '如果我不导出密钥会怎样？' : 'What if I never export the keys?',
				custodyWalletFaq3Answer: isZh
					? '你将继续依赖平台提供的钱包入口和恢复能力。一旦平台服务异常、停服或你需要跨平台迁移资产，处理空间会明显更小。'
					: 'You will continue relying on the platform wallet entry and recovery support. If the service fails, shuts down, or you need cross-platform migration later, your available options will be much smaller.',
				custodyWalletFooterNote: isZh
					? '如需长期自主管理资产，建议尽快完成密钥导出或转移到外部非托管钱包。'
					: 'If you plan to self-manage assets long term, export the keys or move funds to an external non-custodial wallet as soon as possible.',

				changeAvatar: isZh ? '修改头像' : 'Change Avatar',
				changeUsernameTitle: isZh ? '修改用户名' : 'Change Username',
				changePasswordTitle: isZh ? '修改密码' : 'Change Password',
				
				// Bind Email & Change Password
				bindEmail: isZh ? '绑定邮箱' : 'Bind Email',
				bindEmailTitle: isZh ? '绑定邮箱' : 'Bind Email',
				bindEmailStepsTitle: isZh ? '绑定邮箱需两次验证' : 'Two-Step Verification Required',
				bindEmailStepsDesc: isZh ? '先验证当前邮箱，再验证新邮箱，全部通过后才会完成绑定。' : 'Verify your current email first, then verify the new email.',
				step1VerifyOld: isZh ? '步骤1：验证当前邮箱' : 'Step 1: Verify Current Email',
				currentEmail: isZh ? '当前邮箱' : 'Current Email',
				enterOldCode: isZh ? '输入当前邮箱验证码' : 'Enter current email code',
				verifyCurrentEmail: isZh ? '验证当前邮箱' : 'Verify Current Email',
				step2VerifyNew: isZh ? '步骤2：验证新邮箱' : 'Step 2: Verify New Email',
				enterNewEmail: isZh ? '输入新邮箱地址' : 'Enter new email address',
				enterNewCode: isZh ? '输入新邮箱验证码' : 'Enter new email code',
				completeBinding: isZh ? '完成绑定' : 'Complete Binding',
				
				changePwdStepsTitle: isZh ? '先邮箱验证，再修改密码' : 'Email Verification Required',
				changePwdStepsDesc: isZh ? '必须先通过邮箱验证码验证，才能提交新密码。' : 'You must verify your email code before setting a new password.',
				step1VerifyEmail: isZh ? '步骤1：邮箱验证码验证' : 'Step 1: Verify Email Code',
				enterEmailCode: isZh ? '输入邮箱验证码' : 'Enter email verification code',
				verifyPassed: isZh ? '验证通过' : 'Verify',
				step2SetPwd: isZh ? '步骤2：设置新密码' : 'Step 2: Set New Password',
				enterNewPwd: isZh ? '输入新密码' : 'Enter new password',
				confirmNewPwd: isZh ? '确认新密码' : 'Confirm new password',
				confirmChange: isZh ? '确认修改' : 'Confirm Change',

				// Modal / Toast（系统弹框、全局请求提示等）
				confirmBtn: isZh ? '确定' : 'OK',
				cancelBtn: isZh ? '取消' : 'Cancel',
				verifyFingerprintToLogin: isZh ? '请验证指纹登录' : 'Verify fingerprint to sign in',
				fingerprintLoginFailed: isZh ? '指纹登录失败' : 'Fingerprint sign-in failed',
				pleaseEnableBiometricFirst: isZh ? '请先使用邮箱密码登录并开启指纹登录' : 'Sign in with email and password first, then enable fingerprint login',
				loginPasswordNotSet: isZh
					? '尚未设置登录密码，请联系支持或通过找回密码重新设置'
					: 'No password is set yet. Reset it from Forgot Password or contact support.',
				pleaseEnterVerificationCode: isZh ? '请输入验证码' : 'Please enter verification code',
				pleaseEnterPassword: isZh ? '请输入密码' : 'Please enter password',
				pleaseConfirmPassword: isZh ? '请确认密码' : 'Please confirm your password',
				emailFormatInvalid: isZh ? '邮箱格式不正确' : 'Please enter a valid email address',
				signInWithFingerprint: isZh ? '使用指纹登录' : 'Sign in with fingerprint',
				verifying: isZh ? '验证中...' : 'Verifying...',
				toastRequestFailed: isZh ? '请求失败' : 'Request failed',
				toastNetworkError: isZh ? '网络错误' : 'Network error',
				uploading: isZh ? '上传中...' : 'Uploading...',
				modifySuccess: isZh ? '修改成功' : 'Updated successfully',
				uploadFailed: isZh ? '上传失败' : 'Upload failed',
				emailNotBound: isZh ? '未绑定邮箱' : 'No email bound',
				fillAllFields: isZh ? '请填写完整信息' : 'Please fill in all fields',
				pleaseEnterOldPassword: isZh ? '请输入旧密码' : 'Please enter your current password',
				passwordMismatch: isZh ? '两次密码不一致' : 'Passwords do not match',
				modifying: isZh ? '修改中...' : 'Saving...',
				pleaseEnterNewEmailToast: isZh ? '请输入新邮箱' : 'Please enter new email',
				binding: isZh ? '绑定中...' : 'Binding...',
				bindSuccess: isZh ? '绑定成功' : 'Email bound successfully',
				pleaseEnterUsername: isZh ? '请输入用户名' : 'Please enter username',
				enterNewUsername: isZh ? '新用户名' : 'New username',
				usernamePlaceholder: isZh ? '请输入新用户名' : 'Enter new username',
				leavingPageTitle: isZh ? '即将离开本页面' : 'Leaving this page',
				leavingPageMessage: isZh
					? '您即将跳转至第三方网站。Conflux 不对第三方网站的内容、安全性和隐私政策负责。请注意保护您的资产安全。'
					: 'You are about to visit a third-party website. Conflux is not responsible for its content, security, or privacy practices. Stay vigilant with your assets.',
				continueToExternal: isZh ? '继续前往' : 'Continue',
				linkCopied: isZh ? '链接已复制' : 'Link copied',
				shareUnavailable: isZh ? '分享功能暂不可用' : 'Share is not available',
				unbound: isZh ? '未绑定' : 'Unbound',
				apiSettings: isZh ? 'API 设置' : 'API Settings',
				backendIp: isZh ? '后端地址' : 'Backend URL',
				backendIpPlaceholder: isZh ? '例如 http://192.168.1.100:8080' : 'e.g. http://192.168.1.100:8080',
				save: isZh ? '保存' : 'Save',
				pleaseEnterIp: isZh ? '请输入后端地址' : 'Please enter backend URL',
				saveSuccessful: isZh ? '保存成功' : 'Saved successfully',
				languageOptionEnglish: 'English',
				languageOptionChinese: isZh ? '中文' : 'Chinese',

				saveLoginAccount: isZh ? '保存账号' : 'Save account',
				saveLoginPassword: isZh ? '保存密码' : 'Save password',
				tradeAuthTitle: isZh ? '交易验证' : 'Confirm transaction',
				tradeAuthBio: isZh ? '指纹' : 'Fingerprint',
				tradeAuthPwd: isZh ? '交易密码' : 'Password',
				tradePwdPlaceholder: isZh ? '请输入交易密码' : 'Enter transaction password',
				tradeAuthBioHint: isZh ? '将使用本机已录入的指纹验证' : 'Use enrolled fingerprint on this device',
				tradeUnlockFailed: isZh ? '验证失败' : 'Verification failed',
				tradePwdRequiredTitle: isZh ? '未设置交易密码' : 'No transaction password',
				tradePwdRequiredBody: isZh
					? '发送资产前需在个人中心设置交易密码。'
					: 'Set a transaction password in Security before sending.',
				goSetTradePwd: isZh ? '去设置' : 'Set now',
				tradePwdPageTitle: isZh ? '交易密码' : 'Transaction password',
				tradePwdSetTitle: isZh ? '设置交易密码' : 'Set transaction password',
				tradePwdUpdateTitle: isZh ? '修改交易密码' : 'Change transaction password',
				tradePwdNew: isZh ? '新交易密码（至少6位）' : 'New password (min 6)',
				tradePwdConfirm: isZh ? '确认新密码' : 'Confirm new password',
				tradePwdOld: isZh ? '原交易密码' : 'Current transaction password',
				tradePwdEmailCode: isZh ? '邮箱验证码' : 'Email code',
				tradePwdSendCode: isZh ? '发送验证码' : 'Send code',
				tradePwdSubmitSet: isZh ? '完成设置' : 'Save',
				tradePwdSubmitUpdate: isZh ? '保存修改' : 'Save changes',
				tradePwdSetOk: isZh ? '交易密码已设置' : 'Transaction password saved',
				tradePwdUpdateOk: isZh ? '交易密码已更新' : 'Transaction password updated',

				// H5：添加到桌面 / 安装提示（PC、iOS、Android）
				addToHomeTitle: isZh ? '添加到主屏幕' : 'Add to home screen',
				addToHomeIos: isZh
					? '在 Safari 中点底部分享按钮，再选「添加到主屏幕」，下次可像 App 一样打开。'
					: 'In Safari, tap Share, then Add to Home Screen for quick access like an app.',
				addToHomeAndroid: isZh
					? '点浏览器菜单 ⋮，选择「添加到主屏幕」或「安装应用」（视浏览器而定）。'
					: 'Open the browser menu (⋮) and tap Add to Home screen or Install app.',
				addToHomeDesktop: isZh
					? '可将本站安装为应用，或固定到任务栏 / 加入书签，方便下次访问（视浏览器而定）。'
					: 'Install this site as an app, pin it to the taskbar, or bookmark it for faster access.',
				addToHomeDismiss: isZh ? '知道了' : 'Got it'
			}
		}
	}
})

export default store
