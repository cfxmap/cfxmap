import { api } from '@/utils/request.js'

const COMMUNITY_DRAFT_KEY = 'community_compose_draft_v1'

function clone(value) {
	return JSON.parse(JSON.stringify(value))
}

function normalizeTopicId(topicId) {
	if (topicId === 'nearby') return 'daily'
	return topicId || 'recommended'
}

export async function getCommunityTopics() {
	return await api.getCommunityTopics()
}

export async function getCommunityComposeTopics(options = {}) {
	return await api.getCommunityTopics({
		composeOnly: true,
		keyword: options.keyword || '',
		limit: options.limit || 12
	})
}

export async function getCommunityPosts(options = {}) {
	const params = {
		category: normalizeTopicId(options.category || 'recommended')
	}
	if (options.keyword) {
		params.keyword = String(options.keyword).trim()
	}
	return await api.getCommunityPosts(params)
}

export async function getCommunityPost(postId) {
	if (!postId) return null
	return await api.getCommunityPostDetail(postId)
}

export async function getCommunityComments(postId, options = {}) {
	if (!postId) {
		return {
			list: [],
			total: 0,
			pageNum: 1,
			pageSize: 8,
			hasMore: false
		}
	}
	return await api.getCommunityComments(postId, {
		sortType: options.sortType || 'time',
		pageNum: options.pageNum || 1,
		pageSize: options.pageSize || 8
	})
}

export async function toggleCommunityReaction(postId, type) {
	if (!postId || !type) return null
	if (type === 'like') {
		await api.toggleCommunityLike(postId)
	}
	if (type === 'save') {
		await api.toggleCommunityFavorite(postId)
	}
	if (type === 'share') {
		await api.shareCommunityPost(postId)
	}
	return await getCommunityPost(postId)
}

export async function toggleCommunityFollow(userId) {
	if (!userId) return null
	return await api.toggleCommunityFollow(userId)
}

export async function addCommunityComment(postId, payload) {
	if (!postId) return null
	const content = String(payload && payload.content ? payload.content : '').trim()
	if (!content) return null
	return await api.addCommunityComment(postId, {
		content,
		parentId: payload && payload.parentId ? payload.parentId : '',
		replyTo: payload && payload.replyTo ? payload.replyTo : ''
	})
}

export async function publishCommunityPost(payload) {
	const title = String(payload && payload.title ? payload.title : '').trim()
	const content = String(payload && payload.content ? payload.content : '').trim()
	if (!title || !content) return null
	const imageList = Array.isArray(payload && payload.imageList) ? clone(payload.imageList) : []
	const post = await api.createCommunityPost({
		title,
		content,
		imageList
	})
	clearCommunityDraft()
	return post
}

export async function uploadCommunityImage(fileInput) {
	return await api.uploadCommunityImage(fileInput)
}

export function getCommunityDraft() {
	try {
		const draft =
			uni.getStorageSync(COMMUNITY_DRAFT_KEY) || {
				title: '',
				content: '',
				imageList: []
			}
		return {
			...clone(draft),
			imageList: Array.isArray(draft.imageList) ? clone(draft.imageList) : []
		}
	} catch (error) {
		return {
			title: '',
			content: '',
			imageList: []
		}
	}
}

export function saveCommunityDraft(draft) {
	const safeDraft = {
		title: String(draft && draft.title ? draft.title : '').slice(0, 24),
		content: String(draft && draft.content ? draft.content : '').slice(0, 500),
		imageList: Array.isArray(draft && draft.imageList) ? clone(draft.imageList) : []
	}
	uni.setStorageSync(COMMUNITY_DRAFT_KEY, safeDraft)
	return safeDraft
}

export function clearCommunityDraft() {
	uni.removeStorageSync(COMMUNITY_DRAFT_KEY)
}

export function formatCommunityTime(timestamp) {
	const diff = Math.max(0, Date.now() - Number(timestamp || 0))
	const minute = 60 * 1000
	const hour = 60 * minute
	const day = 24 * hour
	if (diff < minute) return '刚刚'
	if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
	if (diff < day) return `${Math.floor(diff / hour)} 小时前`
	return `${Math.floor(diff / day)} 天前`
}
