import store from '@/store/index.js'
import { getStaticUrl } from './static-resource.js'

/** App 打包（APK 等）默认接口根地址，可被本地存储 backend_ip 或 API 设置页覆盖 */
export const DEFAULT_APP_API_BASE = 'https://wallet.cfxmap.com';

const IPV4_RE = /^(\d{1,3}\.){3}\d{1,3}$/;

/** 判断 host 是否为 IPv4（局域网 IP 开发时常带 :8080） */
export const isIpV4Host = (host) => IPV4_RE.test(host || '');

/** localhost / 127.0.0.1 / 局域网 IP：保留 :8080 等开发端口 */
const preserveDevStylePort = (host) => {
	const h = host || '';
	return h === 'localhost' || h === '127.0.0.1' || isIpV4Host(h);
};

/**
 * 生产域名通常走 443/80 反代，请求根地址不应带 :8080 等端口。
 * localhost / IPv4 开发地址保留显式端口；普通域名去掉 8080/8443 及默认 80/443。
 */
export const normalizeApiBaseUrl = (url) => {
	if (!url || typeof url !== 'string') return url;
	let s = url.trim().replace(/\/+$/, '');
	const m = s.match(/^(https?:)\/\/([^/:?#]+)(?::(\d+))?(\/.*)?$/i);
	if (!m) return s;
	const proto = m[1].toLowerCase();
	const host = m[2];
	const port = m[3];
	const path = m[4] || '';
	let nextPort = port;
	if (port) {
		if ((proto === 'http:' && port === '80') || (proto === 'https:' && port === '443')) {
			nextPort = '';
		} else if (!preserveDevStylePort(host) && (port === '8080' || port === '8443')) {
			nextPort = '';
		}
	}
	if (nextPort) {
		return `${proto}//${host}:${nextPort}${path}`.replace(/\/+$/, '');
	}
	return `${proto}//${host}${path}`.replace(/\/+$/, '');
};

export const getBaseUrl = () => {
	let savedIp = uni.getStorageSync('backend_ip');
	if (savedIp) {
		return normalizeApiBaseUrl(savedIp);
	}

	// #ifdef APP-PLUS
	return normalizeApiBaseUrl(DEFAULT_APP_API_BASE);
	// #endif

	// #ifdef H5
	if (window.location && window.location.hostname && window.location.hostname !== 'localhost') {
		const { protocol, hostname } = window.location;
		// 域名：默认与页面同域，由网关反代到后端，不带端口
		if (!isIpV4Host(hostname)) {
			return normalizeApiBaseUrl(`${protocol}//${hostname}`);
		}
		return normalizeApiBaseUrl(`${protocol}//${hostname}:8080`);
	}
	// #endif

	return 'http://localhost:8080';
};

export const convertIpfsUrl = (url) => {
	if (!url || typeof url !== 'string') return url;
	if (url.startsWith('ipfs://')) {
		return url.replace('ipfs://', 'https://ipfs.io/ipfs/');
	}
	return url;
};

let activeRequestCount = 0;

export const getActiveRequestCount = () => activeRequestCount;

export const withGlobalLoading = async (promiseOrFn) => {
	activeRequestCount++;
	uni.$emit('globalRequestStart');
	const finalizeRequest = () => {
		activeRequestCount--;
		if (activeRequestCount <= 0) {
			activeRequestCount = 0;
			uni.$emit('globalRequestEnd');
		}
	};
	try {
		const result = typeof promiseOrFn === 'function' ? promiseOrFn() : promiseOrFn;
		return await result;
	} finally {
		finalizeRequest();
	}
};

const getContentLanguageHeader = () => {
	const currentLanguage = store && store.state ? store.state.language : '';
	return currentLanguage === '中文' ? 'zh_CN' : 'en_US';
};

export const request = (options) => {
	activeRequestCount++;
	// 告知全局 loading 有接口请求正在进行
	uni.$emit('globalRequestStart');

	return new Promise((resolve, reject) => {
		const token = uni.getStorageSync('token');
		const headers = {
			'Content-Type': 'application/json',
			'content-language': getContentLanguageHeader()
		};
		
		if (token) {
			headers['Authorization'] = 'Bearer ' + token;
		}

		const finalizeRequest = () => {
			activeRequestCount--;
			if (activeRequestCount <= 0) {
				activeRequestCount = 0;
				uni.$emit('globalRequestEnd');
			}
		};

		uni.request({
			url: getBaseUrl() + options.url,
			method: options.method || 'GET',
			data: options.data || {},
			header: Object.assign(headers, options.header || {}),
			success: (res) => {
				finalizeRequest();
				if (res.statusCode === 200) {
					// 若依的返回结构通常是 { code: 200, msg: "...", data: ... }
					if (res.data.code === 200) {
						resolve(res.data.data || res.data);
					} else if (res.data.code === 401) {
						uni.removeStorageSync('token');
						uni.reLaunch({ url: '/pages/login/login' });
						reject(new Error('Unauthorized'));
					} else {
						const t = store.getters.i18n
						uni.showToast({
							title: res.data.msg || t.toastRequestFailed,
							icon: 'none'
						});
						reject(res.data);
					}
				} else {
					const t = store.getters.i18n
					uni.showToast({
						title: t.toastNetworkError,
						icon: 'none'
					});
					reject(new Error('Network error'));
				}
			},
			fail: (err) => {
				finalizeRequest();
				const t = store.getters.i18n
				uni.showToast({
					title: t.toastRequestFailed,
					icon: 'none'
				});
				reject(err);
			}
		});
	});
};

const parseUploadResponse = (res, resolve, reject) => {
	let body = res.data;
	if (typeof body === 'string') {
		try {
			body = JSON.parse(body);
		} catch (e) {
			const t = store.getters.i18n;
			uni.showToast({ title: t.toastRequestFailed, icon: 'none' });
			reject(e);
			return;
		}
	}
	if (res.statusCode === 200 && body.code === 200) {
		const payload = body.data != null ? body.data : body;
		const url = payload.imgUrl || payload.url;
		if (url) {
			resolve(url);
		} else {
			reject(new Error('No image url in response'));
		}
	} else if (body && body.code === 401) {
		uni.removeStorageSync('token');
		uni.reLaunch({ url: '/pages/login/login' });
		reject(new Error('Unauthorized'));
	} else {
		const t = store.getters.i18n;
		uni.showToast({
			title: (body && body.msg) || t.toastRequestFailed,
			icon: 'none'
		});
		reject(body || new Error('Upload failed'));
	}
};

const inferUploadFileName = (file) => {
	if (file && typeof file.name === 'string' && file.name.trim()) {
		return file.name.trim();
	}
	const type = String((file && file.type) || '').toLowerCase();
	if (type.includes('png')) return 'avatar.png';
	if (type.includes('gif')) return 'avatar.gif';
	if (type.includes('webp')) return 'avatar.webp';
	if (type.includes('heic')) return 'avatar.heic';
	if (type.includes('heif')) return 'avatar.heif';
	return 'avatar.jpg';
};

const inferUploadContentType = (fileName, file) => {
	const directType = String((file && file.type) || '').trim();
	if (directType) return directType;
	const lower = String(fileName || '').toLowerCase();
	if (lower.endsWith('.png')) return 'image/png';
	if (lower.endsWith('.gif')) return 'image/gif';
	if (lower.endsWith('.webp')) return 'image/webp';
	if (lower.endsWith('.heic')) return 'image/heic';
	if (lower.endsWith('.heif')) return 'image/heif';
	return 'image/jpeg';
};

const getUploadFilePath = (input) => {
	if (!input) return '';
	if (typeof input === 'string') return input;
	if (typeof input.filePath === 'string' && input.filePath) return input.filePath;
	if (typeof input.tempFilePath === 'string' && input.tempFilePath) return input.tempFilePath;
	if (Array.isArray(input.tempFilePaths) && input.tempFilePaths[0]) return input.tempFilePaths[0];
	return '';
};

const resolveH5AvatarFile = (input) => {
	if (!input) return null;
	if (typeof File !== 'undefined' && input instanceof File) {
		return input;
	}
	if (typeof Blob !== 'undefined' && input instanceof Blob) {
		return input;
	}
	if (input.file && typeof File !== 'undefined' && input.file instanceof File) {
		return input.file;
	}
	if (input.file && typeof Blob !== 'undefined' && input.file instanceof Blob) {
		return input.file;
	}
	if (input.tempFile && typeof File !== 'undefined' && input.tempFile instanceof File) {
		return input.tempFile;
	}
	if (input.tempFile && typeof Blob !== 'undefined' && input.tempFile instanceof Blob) {
		return input.tempFile;
	}
	return null;
};

const resolveH5AvatarUploadFile = async (input) => {
	const directFile = resolveH5AvatarFile(input);
	if (directFile) {
		const fileName = inferUploadFileName(directFile);
		return {
			file: directFile,
			fileName,
			contentType: inferUploadContentType(fileName, directFile)
		};
	}
	const filePath = getUploadFilePath(input);
	if (!filePath || typeof fetch === 'undefined' || typeof Blob === 'undefined') {
		return null;
	}
	if (!/^(blob:|data:|https?:)/i.test(filePath)) {
		return null;
	}
	const response = await fetch(filePath);
	if (!response.ok) {
		throw new Error('Fetch H5 avatar file failed: ' + response.status);
	}
	const blob = await response.blob();
	const fileName = inferUploadFileName({
		name: (input && input.name) || (input && input.tempFile && input.tempFile.name) || '',
		type: blob.type
	});
	return {
		file: blob,
		fileName,
		contentType: inferUploadContentType(fileName, blob)
	};
};

const buildAvatarPolicyQuery = (meta = {}) => {
	const parts = [];
	if (meta.fileName) {
		parts.push('fileName=' + encodeURIComponent(String(meta.fileName)));
	}
	if (meta.contentType) {
		parts.push('contentType=' + encodeURIComponent(String(meta.contentType)));
	}
	return parts.length ? '?' + parts.join('&') : '';
};

const resolveAvatarUploadMeta = async (fileInput) => {
	const filePath = getUploadFilePath(fileInput);
	// #ifdef H5
	const h5Upload = await resolveH5AvatarUploadFile(fileInput);
	if (h5Upload && h5Upload.file) {
		return {
			file: h5Upload.file,
			filePath,
			fileName: h5Upload.fileName,
			contentType: h5Upload.contentType
		};
	}
	// #endif
	const rawFile = fileInput && (fileInput.tempFile || fileInput.file) ? (fileInput.tempFile || fileInput.file) : null;
	const fileName = inferUploadFileName({
		name: (fileInput && fileInput.name) || (rawFile && rawFile.name) || filePath.split('/').pop() || filePath.split('\\').pop() || '',
		type: rawFile && rawFile.type
	});
	return {
		file: rawFile,
		filePath,
		fileName,
		contentType: inferUploadContentType(fileName, rawFile)
	};
};

const uploadAvatarToOss = async (policy, uploadMeta) => {
	// #ifdef H5
	if (uploadMeta.file && typeof FormData !== 'undefined') {
		const formData = new FormData();
		Object.keys(policy.formData || {}).forEach((key) => {
			if (policy.formData[key] != null && policy.formData[key] !== '') {
				formData.append(key, policy.formData[key]);
			}
		});
		formData.append('file', uploadMeta.file, uploadMeta.fileName || 'avatar.jpg');
		await new Promise((resolve, reject) => {
			const xhr = new XMLHttpRequest();
			xhr.open('POST', policy.host, true);
			xhr.onload = () => {
				if (xhr.status === 200 || xhr.status === 204) {
					resolve();
					return;
				}
				reject(new Error('OSS upload failed: ' + xhr.status));
			};
			xhr.onerror = () => reject(new Error('OSS upload failed'));
			xhr.send(formData);
		});
		return;
	}
	// #endif
	if (!uploadMeta.filePath) {
		throw new Error('No upload file path');
	}
	await new Promise((resolve, reject) => {
		uni.uploadFile({
			url: policy.host,
			filePath: uploadMeta.filePath,
			name: 'file',
			formData: policy.formData || {},
			success: (res) => {
				if (res.statusCode === 200 || res.statusCode === 204) {
					resolve();
					return;
				}
				reject(new Error('OSS upload failed: ' + res.statusCode));
			},
			fail: reject
		});
	});
};

const uploadAvatarLegacy = async (fileInput) => {
	const token = uni.getStorageSync('token');
	const header = {
		'content-language': getContentLanguageHeader()
	};
	if (token) {
		header.Authorization = 'Bearer ' + token;
	}
	// #ifdef H5
	const h5Upload = await resolveH5AvatarUploadFile(fileInput);
	if (h5Upload && h5Upload.file) {
		const formData = new FormData();
		formData.append('avatarfile', h5Upload.file, h5Upload.fileName);
		return await new Promise((resolve, reject) => {
			const xhr = new XMLHttpRequest();
			xhr.open('POST', getBaseUrl() + '/api/user/profile/avatar', true);
			Object.keys(header).forEach((key) => xhr.setRequestHeader(key, header[key]));
			xhr.onload = () => {
				parseUploadResponse(
					{
						statusCode: xhr.status,
						data: xhr.responseText
					},
					resolve,
					reject
				);
			};
			xhr.onerror = (event) => {
				const t = store.getters.i18n;
				console.error('[avatar-upload] xhr error', event);
				uni.showToast({ title: t.toastRequestFailed, icon: 'none' });
				reject(new Error('Upload failed'));
			};
			xhr.send(formData);
		});
	}
	// #endif
	const filePath = getUploadFilePath(fileInput);
	if (!filePath) {
		throw new Error('No upload file path');
	}
	return await new Promise((resolve, reject) => {
		uni.uploadFile({
			url: getBaseUrl() + '/api/user/profile/avatar',
			filePath,
			name: 'avatarfile',
			header,
			success: (res) => {
				parseUploadResponse(res, resolve, reject);
			},
			fail: (err) => {
				const t = store.getters.i18n;
				console.error('[avatar-upload] uploadFile fail', err);
				uni.showToast({ title: t.toastRequestFailed, icon: 'none' });
				reject(err);
			}
		});
	});
};

const uploadCommunityImageLegacy = async (fileInput) => {
	const token = uni.getStorageSync('token');
	const header = {
		'content-language': getContentLanguageHeader()
	};
	if (token) {
		header.Authorization = 'Bearer ' + token;
	}
	const filePath = getUploadFilePath(fileInput);
	if (!filePath) {
		throw new Error('No upload file path');
	}
	return await new Promise((resolve, reject) => {
		uni.uploadFile({
			url: getBaseUrl() + '/api/community/images/upload',
			filePath,
			name: 'file',
			header,
			success: (res) => {
				parseUploadResponse(res, resolve, reject);
			},
			fail: (err) => {
				const t = store.getters.i18n;
				uni.showToast({ title: t.toastRequestFailed, icon: 'none' });
				reject(err);
			}
		});
	});
};

export const api = {
	getBaseUrl: getBaseUrl,
	// 认证模块
	sendSms: (phone) => request({ url: '/api/auth/sms/send', method: 'POST', data: { phone } }),
	loginPhone: (phone, code) => request({ url: '/api/auth/login/phone', method: 'POST', data: { phone, code } }),
	loginApple: (identityToken) => request({ url: '/api/auth/login/apple', method: 'POST', data: { identityToken } }),
	
	// 邮箱模块
	sendEmailCode: (email, payload = {}) => request({
		url: '/api/auth/email/code?email=' + encodeURIComponent(email) +
			(payload.code ? '&code=' + encodeURIComponent(payload.code) : '') +
			(payload.uuid ? '&uuid=' + encodeURIComponent(payload.uuid) : ''),
		method: 'GET'
	}),
	loginEmail: (email, code) => request({ url: '/api/auth/login/email', method: 'POST', data: { clientId: 'e5cd7e4891bf95d1d19206ce24a7b32e', grantType: 'email', tenantId: '000000', email, emailCode: code } }),
	registerPassword: (email, password) =>
		request({
			url: '/api/auth/register/password',
			method: 'POST',
			data: { email, password }
		}),
	loginPassword: (email, password, extra = {}) =>
		request({
			url: '/api/auth/login/password',
			method: 'POST',
			data: { email, password, ...extra }
		}),
	/** 未登录：邮箱验证码重置登录密码 */
	resetPasswordByEmail: (email, code, newPassword) =>
		request({
			url: '/api/auth/password/reset',
			method: 'PUT',
			data: { email, code, newPassword }
		}),
	loginBiometric: (biometricToken) => request({ url: '/api/auth/login/biometric', method: 'POST', data: { biometricToken } }),
	getImageCaptcha: () => request({ url: '/api/auth/code', method: 'GET' }),
	verifyEmailCode: (email, code) => request({ url: '/api/user/verifyEmailCode', method: 'POST', data: { email, code } }),
	bindEmail: (email, code) => request({ url: '/api/user/bindEmail', method: 'PUT', data: { email, code } }),
	updatePwdByEmail: (email, code, newPassword) => request({ url: '/api/user/updatePwdByEmail', method: 'PUT', data: { email, code, newPassword } }),
	updatePwd: (oldPassword, newPassword) => request({ url: '/api/user/updatePwd', method: 'PUT', data: { oldPassword, newPassword } }),
	enableBiometricLogin: (data) => request({ url: '/api/user/biometric/enable', method: 'POST', data }),

	getTradePwdStatus: () => request({ url: '/api/user/tradePwd/status', method: 'GET' }),
	setTradePassword: (data) => request({ url: '/api/user/tradePwd/set', method: 'POST', data }),
	updateTradePassword: (data) => request({ url: '/api/user/tradePwd/update', method: 'PUT', data }),
	unlockTradeSession: (data) => request({ url: '/api/user/tradePwd/unlock', method: 'POST', data }),

	// 用户模块
	getGuideConfig: (language) => request({ url: '/api/guide/config/' + language, method: 'GET' }),
	getUserProfile: () => request({ url: '/api/user/profile', method: 'GET' }),
	updateUserProfile: (data) => request({ url: '/api/user/profile', method: 'PUT', data }),
	getAvatarUploadPolicy: (meta = {}) =>
		request({
			url: '/api/user/profile/avatar/policy' + buildAvatarPolicyQuery(meta),
			method: 'GET'
		}),
	saveAvatarUrl: (avatarUrl) =>
		request({
			url: '/api/user/profile/avatar',
			method: 'PUT',
			data: { avatarUrl }
		}),
	/** 优先阿里云 OSS 直传，失败时回退旧的后端 multipart 上传 */
	uploadAvatar: (fileInput) => {
		return withGlobalLoading(async () => {
			try {
				const uploadMeta = await resolveAvatarUploadMeta(fileInput);
				const policy = await api.getAvatarUploadPolicy(uploadMeta);
				if (!policy || !policy.host || !policy.url) {
					throw new Error('Invalid avatar upload policy');
				}
				await uploadAvatarToOss(policy, uploadMeta);
				await api.saveAvatarUrl(policy.url);
				return policy.url;
			} catch (err) {
				console.warn('[avatar-upload] oss direct upload failed, fallback to legacy upload', err);
				return await uploadAvatarLegacy(fileInput);
			}
		});
	},
	getWalletList: async () => {
		const res = await request({ url: '/api/user/wallet/list', method: 'GET' });
		if (Array.isArray(res)) {
			return res.filter(w => w.address && (w.address.startsWith('cfx:') || w.address.startsWith('cfxtest:')));
		}
		return res;
	},
	getExportWalletList: async () => {
		const res = await request({ url: '/api/user/wallet/list?isNewUserCreated=0', method: 'GET' });
		if (Array.isArray(res)) {
			return res.filter(w => w.address && (w.address.startsWith('cfx:') || w.address.startsWith('cfxtest:')));
		}
		return res;
	},
	getExportWalletList2: async () => {
		const res = await request({ url: '/api/user/wallet/list2', method: 'GET' });
		if (Array.isArray(res)) {
			return res.filter(w => w.address && (w.address.startsWith('cfx:') || w.address.startsWith('cfxtest:')));
		}
		return res;
	},
	/** 钱包列表完整数据（不过滤地址格式），供移除钱包等管理页使用 */
	getWalletListRaw: async () => {
		const res = await request({ url: '/api/user/wallet/list', method: 'GET' });
		return Array.isArray(res) ? res : [];
	},
	bindWallet: (address, chainId) => request({ url: '/api/user/wallet/bind', method: 'POST', data: { address, chainId } }),
	createWallet: (account) => request({ url: '/api/user/wallet/create', method: 'POST', data: account }),
	previewImportWallet: (payload) => request({ url: '/api/user/wallet/import/preview', method: 'POST', data: payload }),
	importWallet: (account) => request({ url: '/api/user/wallet/import', method: 'POST', data: account }),
	/** GET 参数必须挂在 URL 上；部分端对 GET 的 data 不会转成 query，导致 accountId 丢失 */
	exportWallet: (params = {}) => {
		const parts = [];
		if (params.accountId != null && params.accountId !== '') {
			parts.push('accountId=' + encodeURIComponent(String(params.accountId)));
		}
		if (params.address) {
			parts.push('address=' + encodeURIComponent(String(params.address)));
		}
		if (params.exportType) {
			parts.push('exportType=' + encodeURIComponent(String(params.exportType)));
		}
		const qs = parts.length ? '?' + parts.join('&') : '';
		return request({ url: '/api/user/wallet/export' + qs, method: 'GET' });
	},
	removeWallet: (accountId) =>
		request({
			url: '/api/user/wallet?accountId=' + encodeURIComponent(String(accountId)),
			method: 'DELETE'
		}),
	updateSettings: (settings) => request({ url: '/api/user/settings', method: 'PUT', data: settings }),

	// 社区模块
	getCommunityTopics: (params = {}) =>
		request({
			url: '/api/community/topics',
			method: 'GET',
			data: params
		}),
	getCommunityPosts: (params = {}) =>
		request({
			url: '/api/community/posts',
			method: 'GET',
			data: params
		}),
	getCommunityPostDetail: (postId) =>
		request({
			url: `/api/community/posts/${postId}`,
			method: 'GET'
		}),
	getCommunityComments: (postId, params = {}) =>
		request({
			url: `/api/community/posts/${postId}/comments`,
			method: 'GET',
			data: params
		}),
	createCommunityPost: (data) =>
		request({
			url: '/api/community/posts',
			method: 'POST',
			data
		}),
	addCommunityComment: (postId, data) =>
		request({
			url: `/api/community/posts/${postId}/comments`,
			method: 'POST',
			data
		}),
	toggleCommunityLike: (postId) =>
		request({
			url: `/api/community/posts/${postId}/like`,
			method: 'PUT'
		}),
	toggleCommunityFavorite: (postId) =>
		request({
			url: `/api/community/posts/${postId}/favorite`,
			method: 'PUT'
		}),
	shareCommunityPost: (postId) =>
		request({
			url: `/api/community/posts/${postId}/share`,
			method: 'PUT'
		}),
	toggleCommunityFollow: (targetUserId) =>
		request({
			url: `/api/community/users/${targetUserId}/follow`,
			method: 'PUT'
		}),
	uploadCommunityImage: (fileInput) => {
		return withGlobalLoading(async () => {
			return await uploadCommunityImageLegacy(fileInput)
		});
	},

	// 资产模块
	getTokens: async (chainId, address) => {
		if (address && (address.startsWith('cfx:') || address.startsWith('cfxtest:'))) {
			const baseUrl = address.startsWith('cfxtest:') ? 'https://api-testnet.confluxscan.org' : 'https://api.confluxscan.org';
			return withGlobalLoading(new Promise((resolve, reject) => {
				uni.request({
					url: `${baseUrl}/account/tokens?account=${address}`,
					success: (res) => {
						if (res.data && res.data.code === 0 && res.data.data) {
							const list = res.data.data.list || [];
							// 只展示 type 为 native 和 CRC20 的代币
							const filteredList = list.filter(t => t.type === 'native' || t.type === 'CRC20');
							const tokens = filteredList.map(t => {
								const decimals = t.decimals !== undefined ? t.decimals : 18;
								const amount = parseFloat(t.amount) / Math.pow(10, decimals);
								const symUp = String(t.symbol || '').toUpperCase();
								const nm = String(t.name || '');
								const isNativeCfx =
									t.type === 'native' ||
									symUp === 'CFX' ||
									/conflux network token/i.test(nm);
								const name = isNativeCfx ? 'CFX' : t.name || t.symbol;
								const symbol = isNativeCfx ? 'CFX' : t.symbol;
								return {
									name,
									symbol,
									type: t.type || (symUp === 'CFX' || isNativeCfx ? 'native' : 'CRC20'),
									contract: t.address || t.contract || '',
									decimals,
									amount: amount.toFixed(4),
									usdValue: t.priceInUSDT ? amount * parseFloat(t.priceInUSDT) : 0,
									icon: t.iconUrl || getStaticUrl('/static/icons/icon_eth.png'),
									chainIcon: t.iconUrl || getStaticUrl('/static/icons/icon_eth.png')
								};
							});
							resolve(tokens);
						} else {
							resolve([]);
						}
					},
					fail: reject
				});
			}));
		}
		return request({ url: '/api/wallet/tokens', method: 'GET', data: { chainId, address } })
	},
	getNfts: async (chainId, address) => {
		if (address && (address.startsWith('cfx:') || address.startsWith('cfxtest:'))) {
			const baseUrl = address.startsWith('cfxtest:') ? 'https://api-testnet.confluxscan.org' : 'https://api.confluxscan.org';
			return withGlobalLoading(new Promise((resolve, reject) => {
				// 先获取用户的 NFT 余额列表 (按合约分组)
				uni.request({
					url: `${baseUrl}/nft/balances?owner=${address}`,
					success: async (res) => {
						if (res.data && res.data.code === 0 && res.data.data) {
							const balanceList = res.data.data.list || [];
							let allNfts = [];
							
							// 遍历每个有余额的合约，获取具体的 NFT Token 列表
							for (const balance of balanceList) {
								if (balance.contract) {
									try {
										const tokenRes = await new Promise((reqResolve, reqReject) => {
											uni.request({
												url: `${baseUrl}/nft/tokens?contract=${balance.contract}&owner=${address}&withBrief=true&withMetadata=true&limit=50`,
												success: (res) => reqResolve(res),
												fail: reqReject
											});
										});
										
										if (tokenRes.data && tokenRes.data.code === 0 && tokenRes.data.data) {
											const tokens = tokenRes.data.data.list || [];
											const nfts = tokens.map(n => {
												let is3D = false;
												let animationUrl = null;
												
												if (n.rawData && n.rawData.metadata) {
													const metadata = n.rawData.metadata;
													if (metadata.animation_url) {
														animationUrl = metadata.animation_url;
														// 简单判断是否为 3D 模型后缀，如 gltf, glb
														if (animationUrl.toLowerCase().endsWith('.gltf') || animationUrl.toLowerCase().endsWith('.glb')) {
															is3D = true;
														}
													}
												}
												
												return {
													id: `${n.contract}_${n.tokenId}`,
													contract: n.contract,
													tokenId: n.tokenId,
													name: n.name || balance.name || `NFT #${n.tokenId}`,
													series: n.contractName || balance.symbol || n.symbol || 'Unknown Collection',
													is3D: is3D,
													animationUrl: animationUrl,
													image: n.image || balance.iconUrl || getStaticUrl('/static/nft_detail.png')
												};
											});
											allNfts = allNfts.concat(nfts);
										}
									} catch (e) {
										console.error(`Failed to fetch tokens for contract ${balance.contract}:`, e);
									}
								}
							}
							resolve(allNfts);
						} else {
							resolve([]);
						}
					},
					fail: reject
				});
			}));
		}
		return request({ url: '/api/wallet/nfts', method: 'GET', data: { chainId, address } })
	},
	getNftDetail: async (id, contract, tokenId) => {
		if (contract && (contract.startsWith('cfx:') || contract.startsWith('cfxtest:'))) {
			const baseUrl = contract.startsWith('cfxtest:') ? 'https://api-testnet.confluxscan.org' : 'https://api.confluxscan.org';
			return withGlobalLoading(new Promise((resolve, reject) => {
				uni.request({
					url: `${baseUrl}/nft/preview?contract=${contract}&tokenId=${tokenId}&withMetadata=true`,
					success: (res) => {
						if (res.data && res.data.code === 0 && res.data.data) {
							const n = res.data.data;
							let is3D = false;
							let animationUrl = null;
							
							if (n.rawData && n.rawData.metadata) {
								const metadata = n.rawData.metadata;
								if (metadata.animation_url) {
									animationUrl = metadata.animation_url;
									if (animationUrl.toLowerCase().endsWith('.gltf') || animationUrl.toLowerCase().endsWith('.glb')) {
										is3D = true;
									}
								}
							}
							
							resolve({
								id: `${n.contract}_${n.tokenId}`,
								contractAddress: n.contract,
								tokenId: n.tokenId,
								name: n.name || `NFT #${n.tokenId}`,
								series: n.contractName || n.symbol || 'Unknown Collection',
								image: n.image || getStaticUrl('/static/nft_detail.png'),
								is3D: is3D,
								animationUrl: animationUrl,
								description: n.description || '',
								standard: n.type || 'ERC-721',
								chain: 'Conflux'
							});
						} else {
							reject(new Error('Not found'));
						}
					},
					fail: reject
				});
			}));
		}
		return request({ url: `/api/wallet/nft/${id}`, method: 'GET' })
	},

	// 交易模块
	getGas: () => request({ url: '/api/tx/gas', method: 'GET' }),
	estimateGas: (data) => request({ url: '/api/tx/estimateGas', method: 'POST', data }),
	broadcastTx: (signedTx) => request({ url: '/api/tx/broadcast', method: 'POST', data: { signedTx } }),
	getTxDetail: (txHash, chainId) => {
		let url = '/api/tx/detail?txHash=' + encodeURIComponent(txHash)
		if (chainId != null && chainId !== '') {
			url += '&chainId=' + encodeURIComponent(String(chainId))
		}
		return request({
			url,
			method: 'GET'
		})
	},
	getTxHistory: async (address, chainId, options = {}) => {
		const limit = typeof options.limit === 'number' ? options.limit : 20;
		const skip = typeof options.skip === 'number' ? options.skip : 0;

		if (address && (address.startsWith('cfx:') || address.startsWith('cfxtest:'))) {
			return withGlobalLoading(async () => {
				const baseUrl = address.startsWith('cfxtest:') ? 'https://api-testnet.confluxscan.net' : 'https://api.confluxscan.net';
				const isTestnet = address.startsWith('cfxtest:');
			
			try {
				const txUrl = `${baseUrl}/account/transactions?account=${address}&limit=${limit}&skip=${skip}`;
				const crc721Url = `${baseUrl}/account/crc721/transfers?account=${address}&limit=${limit}&skip=${skip}`;
				const crc1155Url = `${baseUrl}/account/crc1155/transfers?account=${address}&limit=${limit}&skip=${skip}`;
				
				const [txRes, crc721Res, crc1155Res] = await Promise.all([
					new Promise((resolve, reject) => uni.request({ url: txUrl, success: resolve, fail: reject })),
					new Promise((resolve, reject) => uni.request({ url: crc721Url, success: resolve, fail: reject })),
					new Promise((resolve, reject) => uni.request({ url: crc1155Url, success: resolve, fail: reject }))
				]);
				
				let txList = [];
				
				// Process normal transactions
				if (txRes.data && txRes.data.code === 0 && txRes.data.data) {
					const list = txRes.data.data.list || [];
					for (const txObj of list) {
						let tx = {
							txHash: txObj.hash,
							fromAddress: txObj.from,
							toAddress: txObj.to,
							amount: 0,
							tokenSymbol: 'CFX',
							chainId: isTestnet ? '1' : '1029',
							status: txObj.status === 0 ? 'Completed' : (txObj.status === null ? 'Pending' : 'Failed'),
							createTime: txObj.timestamp ? txObj.timestamp * 1000 : Date.now()
						};
						
						const valueStr = txObj.value;
						if (valueStr) {
							tx.amount = (Number(valueStr) / 1e18).toFixed(4);
						}
						
						if (txObj.tokenTransfer && txObj.tokenTransfer.length > 0) {
							const transfer = txObj.tokenTransfer[0];
							tx.tokenSymbol = transfer.symbol || 'Token';
							if (transfer.value) {
								tx.amount = (Number(transfer.value) / Math.pow(10, transfer.decimals || 18)).toFixed(4);
							}
							if (transfer.from) tx.fromAddress = transfer.from;
							if (transfer.to) tx.toAddress = transfer.to;
						}
						txList.push(tx);
					}
				}
				
				// Process NFT transfers
				const nftList = [];
				if (crc721Res.data && crc721Res.data.code === 0 && crc721Res.data.data && crc721Res.data.data.list) {
					nftList.push(...crc721Res.data.data.list);
				}
				if (crc1155Res.data && crc1155Res.data.code === 0 && crc1155Res.data.data && crc1155Res.data.data.list) {
					nftList.push(...crc1155Res.data.data.list);
				}

				if (nftList.length > 0) {
					const list = nftList;
					
					// Fetch previews sequentially to avoid hitting rate limit (5 calls/second for free tier)
					// 分页时 limit 很小（默认 5），这里的顺序预览请求也会更轻量
					for (const transferObj of list) {
						let tx = {
							txHash: transferObj.transactionHash,
							fromAddress: transferObj.from,
							toAddress: transferObj.to,
							amount: 1,
							chainId: isTestnet ? '1' : '1029',
							status: 'Completed',
							createTime: transferObj.timestamp ? transferObj.timestamp * 1000 : Date.now(),
							isNFT: true,
							contract: transferObj.contract,
							tokenId: transferObj.tokenId
						};
						
						let name = transferObj.name || transferObj.symbol || 'NFT';
						let tokenId = transferObj.tokenId;
						
						// Call NFT preview API
						try {
							const previewUrl = `${baseUrl}/nft/preview?contract=${tx.contract}&tokenId=${tx.tokenId}&withMetadata=true`;
							const previewRes = await new Promise((resolve) => uni.request({ url: previewUrl, success: resolve, fail: () => resolve(null) }));
							if (previewRes && previewRes.data && previewRes.data.code === 0 && previewRes.data.data) {
								const pData = previewRes.data.data;
								if (pData.name) {
									name = pData.name;
								}
								tx.nftImage = pData.image;
							}
							// Add a small delay to avoid rate limit
							await new Promise(resolve => setTimeout(resolve, 200));
						} catch(e) {
							console.error('Failed to fetch NFT preview', e);
						}
						
						tx.tokenSymbol = `NFT: ${name}` + (tokenId ? ` #${tokenId}` : '');
						
						// Remove duplicate from txList (the CFX wrapper)
						txList = txList.filter(t => t.txHash !== tx.txHash);
						txList.push(tx);
					}
				}
				
				txList.sort((a, b) => b.createTime - a.createTime);
				return txList;
			} catch (e) {
				console.error('Failed to fetch tx history', e);
				return request({
					url: `/api/tx/history?address=${address}&chainId=${chainId}&limit=${limit}&skip=${skip}`,
					method: 'GET'
				});
			}
			});
		}
		return request({ url: `/api/tx/history?address=${address}&chainId=${chainId}&limit=${limit}&skip=${skip}`, method: 'GET' });
	},
	sendNft: (fromAddress, toAddress, contract, tokenId, standard, extra = {}) =>
		request({
			url: '/api/tx/sendNft',
			method: 'POST',
			data: {
				fromAddress,
				toAddress,
				contract,
				tokenId,
				standard,
				tokenName: extra.tokenName,
				tokenSymbol: extra.tokenSymbol,
				tradeSessionToken: extra.tradeSessionToken
			}
		}),
	sendCfx: (fromAddress, toAddress, amount, extra = {}) =>
		request({
			url: '/api/tx/sendCfx',
			method: 'POST',
			data: { fromAddress, toAddress, amount, tradeSessionToken: extra.tradeSessionToken }
		}),
	sendToken: (fromAddress, toAddress, amount, contract, tokenSymbol, decimals, extra = {}) =>
		request({
			url: '/api/tx/sendToken',
			method: 'POST',
			data: {
				fromAddress,
				toAddress,
				amount: String(amount),
				contract,
				tokenSymbol,
				decimals: String(decimals),
				tradeSessionToken: extra.tradeSessionToken
			}
		})
};
