/**
 * 密码登录「保存账号 / 保存密码」本地存储：密码使用设备 installation id 作 XOR 混淆（非高强度加密，防明文裸存）。
 * 密钥空间与设备绑定，与钱包/交易等其它用途隔离。
 */

const FLAG = 'login_remember_flag_v1'
const EMAIL_KEY = 'login_remember_email_v1'
const PWD_ENC_KEY = 'login_remember_pwd_xor_v1'
const SAVE_EMAIL_OPT_KEY = 'login_remember_save_email_v1'
const SAVE_PWD_OPT_KEY = 'login_remember_save_pwd_v1'
const DEVICE_KEY = 'app_biometric_device_id'

function getSalt() {
	try {
		let id = uni.getStorageSync(DEVICE_KEY)
		if (!id) {
			id = `dev_${Date.now()}_${Math.random().toString(36).slice(2, 12)}`
			uni.setStorageSync(DEVICE_KEY, id)
		}
		return `LOGIN_REMEMBER|${id}|cfxmap`
	} catch (e) {
		return 'LOGIN_REMEMBER|fallback|cfxmap'
	}
}

function xorEncUtf8(plain, salt) {
	if (!plain) return ''
	const s = String(plain)
	const k = String(salt)
	let out = ''
	for (let i = 0; i < s.length; i++) {
		out += String.fromCharCode(s.charCodeAt(i) ^ k.charCodeAt(i % k.length))
	}
	/* 各端通用 base64 */
	if (typeof btoa !== 'undefined') {
		try {
			return btoa(unescape(encodeURIComponent(out)))
		} catch (e) {
			return btoa(out)
		}
	}
	return out
}

function xorDecUtf8(b64, salt) {
	if (!b64) return ''
	let bin = ''
	try {
		bin = typeof atob !== 'undefined' ? decodeURIComponent(escape(atob(b64))) : b64
	} catch (e) {
		try {
			bin = atob(b64)
		} catch (e2) {
			return ''
		}
	}
	const k = String(salt)
	let out = ''
	for (let i = 0; i < bin.length; i++) {
		out += String.fromCharCode(bin.charCodeAt(i) ^ k.charCodeAt(i % k.length))
	}
	return out
}

/**
 * @param {{ email?: string, password?: string, saveEmail: boolean, savePassword: boolean }} opts
 */
export function saveLoginRemember(opts) {
	const { email = '', password = '', saveEmail, savePassword } = opts || {}
	if (!saveEmail && !savePassword) {
		clearLoginRemember()
		return
	}
	uni.setStorageSync(FLAG, '1')
	uni.setStorageSync(SAVE_EMAIL_OPT_KEY, saveEmail ? '1' : '0')
	uni.setStorageSync(SAVE_PWD_OPT_KEY, savePassword ? '1' : '0')
	if (saveEmail) {
		uni.setStorageSync(EMAIL_KEY, email || '')
	} else {
		try {
			uni.removeStorageSync(EMAIL_KEY)
		} catch (e) {}
	}
	if (savePassword) {
		uni.setStorageSync(PWD_ENC_KEY, xorEncUtf8(password || '', getSalt()))
	} else {
		try {
			uni.removeStorageSync(PWD_ENC_KEY)
		} catch (e) {}
	}
}

export function clearLoginRemember() {
	try {
		uni.removeStorageSync(FLAG)
		uni.removeStorageSync(EMAIL_KEY)
		uni.removeStorageSync(PWD_ENC_KEY)
		uni.removeStorageSync(SAVE_EMAIL_OPT_KEY)
		uni.removeStorageSync(SAVE_PWD_OPT_KEY)
	} catch (e) {}
}

/** @returns {{ email: string, password: string, saveEmail: boolean, savePassword: boolean } | null} */
export function loadLoginRemember() {
	try {
		if (uni.getStorageSync(FLAG) !== '1') return null
		const emailStored = uni.getStorageSync(EMAIL_KEY) || ''
		const enc = uni.getStorageSync(PWD_ENC_KEY) || ''
		let saveEmailFlag = uni.getStorageSync(SAVE_EMAIL_OPT_KEY)
		let savePwdFlag = uni.getStorageSync(SAVE_PWD_OPT_KEY)
		/* 旧版仅有邮箱+密文，无分项开关 */
		if (saveEmailFlag !== '1' && saveEmailFlag !== '0') {
			saveEmailFlag = emailStored ? '1' : '0'
			savePwdFlag = enc ? '1' : '0'
		}
		const saveEmail = saveEmailFlag === '1'
		const savePassword = savePwdFlag === '1'
		const email = saveEmail ? emailStored : ''
		const password = savePassword && enc ? xorDecUtf8(enc, getSalt()) : ''
		return { email, password, saveEmail, savePassword }
	} catch (e) {
		return null
	}
}
