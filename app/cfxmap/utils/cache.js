const DEFAULT_TTL_MS = 10 * 60 * 1000;

function now() {
	return Date.now();
}

function wrapValue(value, ttlMs) {
	const ttl = typeof ttlMs === 'number' ? ttlMs : DEFAULT_TTL_MS;
	return {
		value,
		expireAt: now() + Math.max(0, ttl)
	};
}

function isValidWrapped(obj) {
	return obj && typeof obj === 'object' && 'expireAt' in obj;
}

export function cacheSet(key, value, ttlMs) {
	if (!key) return;
	try {
		uni.setStorageSync(key, wrapValue(value, ttlMs));
	} catch (e) {
		// ignore storage failures (quota, private mode, etc.)
	}
}

export function cacheGet(key) {
	if (!key) return null;
	try {
		const wrapped = uni.getStorageSync(key);
		if (!isValidWrapped(wrapped)) return null;
		if (typeof wrapped.expireAt !== 'number' || wrapped.expireAt <= now()) {
			try {
				uni.removeStorageSync(key);
			} catch (e) {}
			return null;
		}
		return wrapped.value;
	} catch (e) {
		return null;
	}
}

export function cacheRemove(key) {
	if (!key) return;
	try {
		uni.removeStorageSync(key);
	} catch (e) {}
}

export function cacheRemember(key, getter, ttlMs) {
	const cached = cacheGet(key);
	if (cached !== null && cached !== undefined) return Promise.resolve(cached);
	return Promise.resolve()
		.then(() => getter())
		.then((val) => {
			cacheSet(key, val, ttlMs);
			return val;
		});
}

export function clearWalletCache() {
	try {
		const res = uni.getStorageInfoSync();
		res.keys.forEach(key => {
			if (key.startsWith('cache:tokens:') || key.startsWith('cache:nfts:') || key.startsWith('cache:txs:v2:')) {
				uni.removeStorageSync(key);
			}
		});
	} catch (e) {
		console.error('Failed to clear wallet cache', e);
	}
}
