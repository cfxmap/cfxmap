<template>
	<view v-if="post" class="page-shell detail-page" :class="themeClass" @tap="handleOutsideTap">
		<scroll-view scroll-y class="detail-scroll" @scroll="handleContentScroll" @scrolltolower="loadMoreComments">
			<view class="detail-topbar glass-topbar">
				<view class="back-btn" @click="goBack">‹</view>
				<text class="detail-topbar__title">帖子详情</text>
				<view class="detail-topbar__placeholder"></view>
			</view>

			<view class="detail-inner">
				<view class="glass-card article-card">
					<view class="article-head">
						<view class="avatar-chip">{{ avatarText(post.author) }}</view>
						<view class="article-head__copy">
							<view class="article-head__row">
								<view class="article-head__author">
									<text class="article-author">{{ post.author }}</text>
									<text class="article-meta">{{ formatCommunityTime(post.publishedAt) }}</text>
								</view>
							<view
								v-if="!post.owner"
								class="follow-btn"
								:class="{ 'follow-btn--active': post.followingAuthor }"
								@click="toggleFollow"
							>
								{{ post.followingAuthor ? '已关注' : '关注' }}
							</view>
							</view>
						</view>
					</view>

					<view class="article-social-meta">
					<text class="article-social-meta__item">{{ post.viewCount || post.stats.views }}人看过</text>
						<text class="article-social-meta__dot">·</text>
						<text class="article-social-meta__item">{{ post.stats.comments }}条评论</text>
					</view>

					<view class="article-copy">
						<text class="article-title">{{ post.title }}</text>
						<text class="article-topic-inline" v-if="post.topicLabel"># {{ post.topicLabel }}</text>
						<text class="article-content">{{ post.content }}</text>
					</view>

					<view
						v-if="post.imageList && post.imageList.length"
						class="article-images"
						:class="{ 'article-images--single': post.imageList.length === 1 }"
					>
						<image
							v-for="(image, index) in post.imageList"
							:key="index"
							class="article-image"
							:class="{ 'article-image--single': post.imageList.length === 1 }"
							:src="image"
							mode="aspectFill"
							@click="previewImage(index)"
						/>
					</view>

				</view>

				<view class="glass-card comment-card">
					<view class="comment-head">
						<view class="comment-head__copy">
							<text class="comment-title">全部评论</text>
							<text class="comment-desc">评论和回复都留在这一页里，点到哪条就直接接着聊。</text>
						</view>
					</view>

					<view class="comment-toolbar">
						<text class="comment-toolbar__summary">{{ orderedComments.length }} 条评论 · {{ commentOrderLabel }}</text>
						<view class="toolbar-btn" @click="toggleCommentOrder">切换顺序</view>
					</view>

					<view class="comment-list">
						<view
							v-for="comment in displayComments"
							:key="comment.id"
							class="comment-item"
						>
							<view class="comment-avatar">{{ avatarText(comment.author) }}</view>
							<view class="comment-main">
								<view class="comment-hit" :class="{ 'comment-hit--selected': replyTarget && replyTarget.id === comment.id }" @tap.stop="selectReply(comment)">
									<view class="comment-hit__head">
										<view>
											<view class="comment-author-row">
												<text class="comment-author">{{ comment.author }}</text>
												<text v-if="comment.badge" class="comment-badge">{{ comment.badge }}</text>
											</view>
											<text class="comment-time">{{ formatCommunityTime(comment.createdAt) }}</text>
										</view>
										<text class="comment-action-tip">轻点可回复</text>
									</view>
									<text class="comment-content">{{ comment.content }}</text>
									<view class="comment-meta-row">
										<text>♡ {{ comment.likes }}</text>
										<text v-if="comment.replies && comment.replies.length">{{ comment.replies.length }} 条楼中楼</text>
									</view>
								</view>

								<view v-if="comment.replies && comment.replies.length" class="reply-list">
									<view
										v-for="reply in comment.replies"
										:key="reply.id"
										class="reply-item"
										@tap.stop="selectReply(comment, reply.author)"
									>
										<view class="reply-head">
											<view class="comment-author-row">
												<text class="comment-author comment-author--small">{{ reply.author }}</text>
												<text v-if="reply.badge" class="comment-badge">{{ reply.badge }}</text>
											</view>
											<text class="comment-action-tip">继续聊</text>
										</view>
										<text class="comment-time">{{ formatCommunityTime(reply.createdAt) }}<text v-if="reply.replyTo"> · 回复 {{ reply.replyTo }}</text></text>
										<text class="reply-content">{{ reply.content }}</text>
									</view>
								</view>
							</view>
						</view>
					</view>

					<view class="load-more-status">
						<text v-if="loadingMore">正在加载...</text>
						<text v-else-if="!hasMoreComments && orderedComments.length > 0">没有更多评论了</text>
						<text v-else-if="orderedComments.length === 0">快来抢沙发</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<view v-if="toastText" class="toast-tip">{{ toastText }}</view>

		<view class="detail-composer-wrap glass-topbar" @tap.stop>
			<view v-if="!composerExpanded && !replyTarget && !emojiOpen" class="compact-action-bar">
				<view class="compact-comment-trigger" @click="openComposer">
					<view class="compact-comment-trigger__icon">✎</view>
					<text class="compact-comment-trigger__text">说点什么...</text>
				</view>
				<view class="compact-actions">
					<view class="compact-action-btn" :class="{ 'compact-action-btn--active': post.liked }" @click="toggleReaction('like')">
						<text class="compact-action-btn__icon">♡</text>
						<text class="compact-action-btn__count">{{ post.stats.likes }}</text>
					</view>
					<view class="compact-action-btn" :class="{ 'compact-action-btn--active': post.saved }" @click="toggleReaction('save')">
						<text class="compact-action-btn__icon">☆</text>
						<text class="compact-action-btn__count">{{ post.stats.saves }}</text>
					</view>
					<view class="compact-action-btn" @click="openComposer">
						<text class="compact-action-btn__icon">◌</text>
						<text class="compact-action-btn__count">{{ post.stats.comments }}</text>
					</view>
				</view>
			</view>

			<view v-else class="detail-composer glass-card" :class="{ 'detail-composer--expanded': composerExpanded }">
				<view class="composer-head">
					<view class="composer-pill" :class="{ 'composer-pill--reply': !!replyTarget }">
						<text>{{ replyTarget ? `回复 ${replyName}` : '有话要说，快来评论' }}</text>
					</view>
					<view v-if="replyTarget" class="composer-head__action" @click="clearReply">结束回复</view>
				</view>
				<view class="composer-input-shell composer-input-shell--expanded">
					<textarea
						v-model="composerText"
						class="composer-textarea composer-textarea--expanded"
						auto-height
						maxlength="240"
						placeholder="说点什么..."
						placeholder-class="composer-placeholder"
						@focus="composerExpanded = true"
						@blur="handleTextareaBlur"
					/>
				</view>

				<view class="composer-extra">
					<view class="composer-toolbar xhs-toolbar">
						<view class="xhs-tool-btn" @click="toggleEmojiPanel">☺</view>
						<view class="send-btn" :class="{ 'send-btn--disabled': !canSend }" @click="sendComment">
							<text class="send-btn__label">发送</text>
						</view>
					</view>
					<view v-if="emojiOpen" class="emoji-panel">
						<view
							v-for="emoji in emojis"
							:key="emoji"
							class="emoji-chip"
							@click="appendEmoji(emoji)"
						>
							<text>{{ emoji }}</text>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
import {
	addCommunityComment,
	formatCommunityTime,
	getCommunityPost,
	toggleCommunityFollow,
	toggleCommunityReaction
} from '@/utils/community-data.js'

export default {
	data() {
		return {
			postId: '',
			post: null,
			commentPage: 1,
			commentPageSize: 8,
			commentOrder: 'time',
			loadingMore: false,
			composerText: '',
			composerExpanded: false,
			emojiOpen: false,
			replyTarget: null,
			replyName: '',
			toastText: '',
			toastTimer: null,
			ignoreOutsideCollapse: false,
			emojis: ['😊', '🥹', '😋', '🤍', '👏', '🍅', '🍲', '🌙', '✨', '🙌', '😄', '💬', '👍', '🥬']
		}
	},
	computed: {
		canSend() {
			return String(this.composerText || '').trim().length > 0
		},
		orderedComments() {
			if (!this.post || !Array.isArray(this.post.comments)) return []
			const list = this.post.comments.slice()
			return list.sort((left, right) => {
				return this.commentOrder === 'time' ? right.createdAt - left.createdAt : left.createdAt - right.createdAt
			})
		},
		displayComments() {
			const limit = this.commentPage * this.commentPageSize
			return this.orderedComments.slice(0, limit)
		},
		hasMoreComments() {
			return this.displayComments.length < this.orderedComments.length
		},
		commentOrderLabel() {
			return this.commentOrder === 'time' ? '按时间顺序' : '按最早在前'
		}
	},
	onLoad(query) {
		this.postId = query && query.id ? query.id : ''
		this.loadPost()
	},
	onShow() {
		this.loadPost()
	},
	onUnload() {
		if (this.toastTimer) {
			clearTimeout(this.toastTimer)
		}
	},
	methods: {
		formatCommunityTime,
		openComposer() {
			this.composerExpanded = true
		},
		handleOutsideTap() {
			if (this.ignoreOutsideCollapse) {
				this.ignoreOutsideCollapse = false
				return
			}
			this.collapseComposer()
		},
		handleContentScroll() {
			this.collapseComposer()
		},
		async loadPost() {
			try {
				const fallback = await getCommunityPost(this.postId)
				if (!fallback) return
				this.post = fallback
				this.postId = fallback.id
			} catch (error) {
				uni.showToast({
					title: '帖子加载失败',
					icon: 'none'
				})
			}
		},
		loadMoreComments() {
			if (!this.hasMoreComments || this.loadingMore) return
			this.loadingMore = true
			setTimeout(() => {
				this.commentPage += 1
				this.loadingMore = false
			}, 500)
		},
		goBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({
				url: '/pages/community/community'
			})
		},
		avatarText(name) {
			return String(name || '邻居').slice(0, 2)
		},
		selectReply(comment, replyName) {
			this.ignoreOutsideCollapse = true
			this.replyTarget = comment
			this.replyName = replyName || comment.author
			this.composerExpanded = true
		},
		clearReply() {
			this.replyTarget = null
			this.replyName = ''
		},
		toggleEmojiPanel() {
			this.composerExpanded = true
			this.emojiOpen = !this.emojiOpen
		},
		appendEmoji(emoji) {
			this.composerText = `${this.composerText}${emoji}`
			this.composerExpanded = true
		},
		handleTextareaBlur() {
			if (!this.composerText && !this.emojiOpen && !this.replyTarget) {
				this.composerExpanded = false
			}
		},
		collapseComposer() {
			this.composerExpanded = false
			this.emojiOpen = false
			this.clearReply()
		},
		showToast(message) {
			this.toastText = message
			if (this.toastTimer) {
				clearTimeout(this.toastTimer)
			}
			this.toastTimer = setTimeout(() => {
				this.toastText = ''
			}, 1800)
		},
		async sendComment() {
			const content = String(this.composerText || '').trim()
			if (!content) {
				this.showToast('先写点内容再发送。')
				return
			}
			try {
				const updated = await addCommunityComment(this.postId, {
					content,
					parentId: this.replyTarget ? this.replyTarget.id : '',
					replyTo: this.replyName
				})
				if (!updated) return
				this.post = updated
				this.commentPage = 1
				this.composerText = ''
				this.emojiOpen = false
				this.composerExpanded = false
				this.showToast(this.replyTarget ? `已回复 ${this.replyName}` : '已评论到帖子里')
				this.clearReply()
			} catch (error) {
				this.showToast('评论发送失败')
			}
		},
		toggleCommentOrder() {
			this.commentOrder = this.commentOrder === 'time' ? 'oldest' : 'time'
			this.commentPage = 1
			uni.pageScrollTo({ scrollTop: 0, duration: 300 })
		},
		async toggleReaction(type) {
			try {
				const updated = await toggleCommunityReaction(this.postId, type)
				if (!updated) return
				this.post = updated
				if (type === 'share') {
					this.showToast('已加入分享计数')
				}
			} catch (error) {
				this.showToast('操作失败，请稍后重试')
			}
		},
		async toggleFollow() {
			if (!this.post || !this.post.authorUserId) return
			try {
				await toggleCommunityFollow(this.post.authorUserId)
				await this.loadPost()
			} catch (error) {
				this.showToast('关注操作失败')
			}
		},
		previewImage(index) {
			if (!this.post || !this.post.imageList || !this.post.imageList.length) return
			uni.previewImage({
				current: this.post.imageList[index],
				urls: this.post.imageList
			})
		}
	}
}
</script>

<style scoped lang="scss">
.detail-page {
	min-height: 100vh;
}

.detail-scroll {
	height: 100vh;
}

.glass-card,
.glass-topbar {
	background: var(--glass-bg);
	border: var(--card-border);
	box-shadow: var(--card-shadow-sm);
	backdrop-filter: blur(28rpx);
	-webkit-backdrop-filter: blur(28rpx);
	box-sizing: border-box;
}

.detail-topbar {
	position: sticky;
	top: 0;
	z-index: 10;
	padding: calc(16rpx + env(safe-area-inset-top)) 32rpx 16rpx;
	display: flex;
	align-items: center;
	justify-content: space-between;
}

.back-btn,
.follow-btn,
.toolbar-btn,
.icon-btn,
.detail-action-btn {
	display: inline-flex;
	align-items: center;
	justify-content: center;
}

.back-btn {
	width: 72rpx;
	height: 72rpx;
	border-radius: 20rpx;
	background: rgba(255, 255, 255, 0.78);
	border: 1px solid var(--border-color);
	box-shadow: 0 8rpx 22rpx rgba(15, 23, 42, 0.1);
	color: var(--text-primary);
	font-size: 46rpx;
	font-weight: 500;
	line-height: 1;
}

.theme-dark .back-btn {
	background: rgba(18, 30, 52, 0.82);
	border-color: rgba(255, 255, 255, 0.16);
	box-shadow: 0 8rpx 22rpx rgba(2, 8, 20, 0.34);
}

.back-btn:active {
	transform: scale(0.97);
}

.detail-topbar__title {
	color: var(--text-primary);
	font-size: 30rpx;
	font-weight: 700;
}

.detail-topbar__placeholder {
	width: 72rpx;
}

.detail-inner {
	padding: 24rpx 32rpx calc(280rpx + env(safe-area-inset-bottom));
	display: flex;
	flex-direction: column;
	gap: 20rpx;
	box-sizing: border-box;
}

.article-card,
.comment-card,
.detail-composer {
	border-radius: 32rpx;
	padding: 28rpx;
}

.article-head {
	display: flex;
	align-items: flex-start;
	gap: 20rpx;
}

.avatar-chip,
.comment-avatar {
	width: 88rpx;
	height: 88rpx;
	border-radius: 999rpx;
	background: var(--control-bg);
	color: var(--accent-color);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 26rpx;
	font-weight: 700;
	flex-shrink: 0;
}

.comment-avatar {
	width: 72rpx;
	height: 72rpx;
}

.article-head__copy {
	flex: 1;
	min-width: 0;
}

.article-head__row,
.comment-head,
.comment-toolbar,
.composer-main,
.composer-toolbar,
.detail-action-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.article-head__author {
	flex: 1;
	min-width: 0;
}

.article-author,
.comment-title,
.article-title,
.comment-author {
	display: block;
	color: var(--text-primary);
	font-weight: 700;
}

.article-author {
	font-size: 30rpx;
}

.article-meta,
.comment-desc,
.comment-time,
.comment-action-tip {
	display: block;
	color: var(--text-secondary);
	font-size: 22rpx;
	line-height: 1.6;
}

.follow-btn,
.toolbar-btn,
.comment-page-badge,
.composer-pill,
.comment-badge {
	padding: 0 20rpx;
	min-height: 48rpx;
	border-radius: 999rpx;
	background: var(--control-bg);
	color: var(--text-secondary);
	font-size: 22rpx;
	font-weight: 600;
}

.article-social-meta {
	margin-top: 18rpx;
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 8rpx;
}

.article-social-meta__item,
.article-social-meta__dot {
	color: var(--text-tertiary);
	font-size: 22rpx;
	line-height: 1.4;
}

.comment-page-badge,
.composer-pill--reply {
	color: var(--accent-color);
}

.follow-btn--active {
	background: rgba(66, 200, 255, 0.14);
	color: var(--accent-color);
}

.article-copy {
	margin-top: 22rpx;
}

.article-title {
	font-size: 42rpx;
	line-height: 1.28;
	letter-spacing: -0.02em;
}

.article-content,
.comment-content,
.reply-content {
	display: block;
	margin-top: 16rpx;
	color: var(--text-primary);
	font-size: 28rpx;
	line-height: 1.68;
}

.article-topic-inline {
	display: inline-block;
	margin-top: 14rpx;
	color: var(--accent-color);
	font-size: 24rpx;
	font-weight: 700;
}

.article-images {
	margin-top: 24rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 12rpx;
}

.article-image {
	width: calc((100% - 12rpx) / 2);
	height: 220rpx;
	border-radius: 24rpx;
	box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.08);
}

.article-images--single .article-image--single {
	width: 100%;
	height: 340rpx;
}

.comment-title {
	font-size: 30rpx;
}

.comment-toolbar {
	margin-top: 24rpx;
	padding-top: 24rpx;
	border-top: 1px solid var(--border-color);
	align-items: flex-start;
}

.comment-toolbar__summary {
	color: var(--text-primary);
	font-size: 24rpx;
	font-weight: 600;
}

.comment-list {
	margin-top: 24rpx;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
}

.comment-item {
	display: flex;
	align-items: flex-start;
	gap: 16rpx;
}

.comment-main {
	flex: 1;
	min-width: 0;
}

.comment-hit {
	border-radius: 28rpx;
	padding: 24rpx;
	border: 1px solid transparent;
	background: transparent;
}

.comment-hit--selected {
	background: rgba(245, 250, 255, 0.9);
	border-color: var(--border-color);
}

.theme-dark .comment-hit--selected {
	background: rgba(22, 38, 68, 0.9);
}

.comment-hit__head,
.comment-author-row,
.comment-meta-row,
.reply-head,
.emoji-panel {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 12rpx;
}

.comment-hit__head,
.reply-head {
	justify-content: space-between;
}

.comment-author {
	font-size: 28rpx;
}

.comment-author--small {
	font-size: 24rpx;
}

.comment-meta-row {
	margin-top: 16rpx;
	color: var(--text-secondary);
	font-size: 22rpx;
}

.reply-list {
	margin-top: 16rpx;
	padding-left: 20rpx;
	border-left: 1px solid var(--border-color);
	display: flex;
	flex-direction: column;
	gap: 12rpx;
}

.reply-item {
	padding: 20rpx;
	border-radius: 24rpx;
	background: rgba(255, 255, 255, 0.45);
}

.theme-dark .reply-item {
	background: rgba(16, 26, 48, 0.54);
}

.load-more-status {
	padding: 40rpx 0;
	display: flex;
	justify-content: center;
	align-items: center;
	color: var(--text-tertiary);
	font-size: 24rpx;
}

.toast-tip {
	position: fixed;
	left: 32rpx;
	right: 32rpx;
	bottom: calc(250rpx + env(safe-area-inset-bottom));
	padding: 24rpx;
	border-radius: 28rpx;
	background: var(--card-bg-strong);
	border: 1px solid var(--border-color);
	color: var(--text-primary);
	font-size: 24rpx;
	text-align: center;
	z-index: 30;
}

.detail-composer-wrap {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
	z-index: 20;
	background: rgba(255, 255, 255, 0.94);
	border-top: 1px solid rgba(15, 23, 42, 0.06);
	box-shadow: 0 -12rpx 36rpx rgba(15, 23, 42, 0.06);
	backdrop-filter: blur(28rpx) saturate(160%);
	-webkit-backdrop-filter: blur(28rpx) saturate(160%);
	box-sizing: border-box;
}

.theme-dark .detail-composer-wrap {
	background: var(--ios-glass-bg);
	border-top-color: var(--ios-glass-border);
	box-shadow:
		0 -16rpx 40rpx rgba(2, 8, 20, 0.34),
		var(--ios-glass-highlight);
	backdrop-filter: blur(34rpx) saturate(165%);
	-webkit-backdrop-filter: blur(34rpx) saturate(165%);
}

.detail-composer {
	padding: 0;
	border-radius: 0;
	background: transparent;
	border: 0;
	box-shadow: none;
}

.detail-composer--expanded {
	box-shadow: none;
}

.composer-input-shell {
	padding: 18rpx 24rpx;
	border-radius: 28rpx;
	background: var(--card-bg-strong);
	border: 1px solid var(--border-color);
	box-sizing: border-box;
}

.compact-action-bar,
.composer-head,
.compact-actions,
.composer-toolbar {
	display: flex;
	align-items: center;
}

.compact-action-bar {
	gap: 20rpx;
}

.compact-comment-trigger {
	flex: 1;
	min-width: 0;
	height: 72rpx;
	padding: 0 22rpx;
	border-radius: 999rpx;
	background: var(--card-bg-strong);
	border: 1px solid var(--border-color);
	display: flex;
	align-items: center;
	gap: 14rpx;
}

.compact-comment-trigger__icon {
	color: var(--text-tertiary);
	font-size: 24rpx;
}

.compact-comment-trigger__text {
	color: var(--text-tertiary);
	font-size: 28rpx;
}

.compact-actions {
	gap: 28rpx;
	flex-shrink: 0;
}

.compact-action-btn {
	display: flex;
	align-items: center;
	gap: 8rpx;
	color: var(--text-secondary);
	font-size: 24rpx;
	font-weight: 500;
}

.compact-action-btn__icon {
	font-size: 44rpx;
	line-height: 1;
}

.compact-action-btn__count {
	min-width: 24rpx;
	font-size: 24rpx;
}

.compact-action-btn--active {
	color: var(--accent-color);
}

.composer-head {
	justify-content: space-between;
	margin-bottom: 16rpx;
}

.composer-pill {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	max-width: 100%;
	min-height: 44rpx;
	padding: 0 18rpx;
	border-radius: 999rpx;
	background: rgba(255, 120, 148, 0.12);
	color: #c26680;
	font-size: 22rpx;
	line-height: 1;
	box-sizing: border-box;
}

.composer-head__action {
	color: var(--text-tertiary);
	font-size: 24rpx;
}

.composer-pill--reply {
	background: rgba(255, 95, 122, 0.16);
	color: #ff5f7a;
}

.composer-input-shell--expanded {
	min-height: 156rpx;
}

.composer-textarea {
	display: block;
	width: 100%;
	min-height: 48rpx;
	max-height: 220rpx;
	color: var(--text-primary);
	font-size: 28rpx;
	line-height: 1.5;
	box-sizing: border-box;
	background: transparent;
}

.composer-placeholder {
	color: var(--text-tertiary);
}

.composer-extra {
	margin-top: 16rpx;
}

.composer-toolbar {
	gap: 18rpx;
}

.xhs-toolbar {
	justify-content: flex-start;
}

.xhs-tool-btn {
	width: 52rpx;
	height: 52rpx;
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--text-secondary);
	font-size: 32rpx;
	line-height: 1;
	flex-shrink: 0;
}

.send-btn {
	display: flex;
	align-items: center;
	justify-content: center;
	margin-left: auto;
	width: 132rpx;
	height: 64rpx;
	border-radius: 999rpx;
	background: #ffb8c7;
	color: #ffffff;
	font-size: 30rpx;
	font-weight: 700;
	flex-shrink: 0;
	box-shadow: none;
	border: 0;
}

.send-btn--disabled {
	background: #f3c7d1;
	color: rgba(255, 255, 255, 0.92);
}

.theme-dark .composer-pill {
	background: rgba(255, 120, 148, 0.16);
	color: #ffb6c6;
}

.theme-dark .composer-input-shell,
.theme-dark .compact-comment-trigger,
.theme-dark .emoji-chip {
	background: var(--ios-glass-bg-soft);
	border-color: rgba(255, 255, 255, 0.08);
	box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
	backdrop-filter: blur(22rpx) saturate(150%);
	-webkit-backdrop-filter: blur(22rpx) saturate(150%);
}

.theme-dark .composer-pill--reply {
	background: rgba(255, 95, 122, 0.22);
	color: #ff9fb3;
}

.theme-dark .send-btn {
	background: linear-gradient(135deg, #ff7c9a, #ff5f7a);
}

.theme-dark .send-btn--disabled {
	background: rgba(255, 124, 154, 0.34);
	color: rgba(255, 255, 255, 0.78);
}

.emoji-panel {
	margin-top: 12rpx;
	justify-content: flex-start;
}

.emoji-chip {
	width: 72rpx;
	height: 72rpx;
	border-radius: 24rpx;
	background: var(--card-bg-strong);
	display: flex;
	align-items: center;
	justify-content: center;
	font-size: 32rpx;
}

@media (min-width: 600px) {
	.detail-inner {
		max-width: 840rpx;
		margin: 0 auto;
	}

	.detail-composer-wrap {
		left: 50%;
		right: auto;
		width: calc(100% - 64px);
		max-width: 840rpx;
		margin: 0;
		transform: translateX(-50%);
		box-sizing: border-box;
	}
}

@media (max-width: 480px) {
	.detail-page {
		min-height: 100vh;
	}

	.detail-topbar {
		padding: calc(10rpx + env(safe-area-inset-top)) 20rpx 10rpx;
	}

	.back-btn,
	.detail-topbar__placeholder {
		width: 56rpx;
		height: 56rpx;
	}

	.back-btn {
		border-radius: 16rpx;
		font-size: 34rpx;
	}

	.detail-topbar__title {
		font-size: 26rpx;
	}

	.detail-inner {
		padding: 16rpx 20rpx calc(224rpx + env(safe-area-inset-bottom));
		gap: 14rpx;
	}

	.article-card,
	.comment-card,
	.detail-composer {
		border-radius: 24rpx;
		padding: 20rpx;
	}

	.avatar-chip {
		width: 72rpx;
		height: 72rpx;
		font-size: 22rpx;
	}

	.comment-avatar {
		width: 58rpx;
		height: 58rpx;
		font-size: 20rpx;
	}

	.article-head {
		gap: 14rpx;
	}

	.article-head__row,
	.comment-head,
	.comment-toolbar {
		align-items: flex-start;
		flex-wrap: wrap;
	}

	.article-author,
	.comment-title {
		font-size: 26rpx;
	}

	.follow-btn,
	.toolbar-btn,
	.comment-page-badge,
	.composer-pill,
	.comment-badge {
		padding: 0 18rpx;
		min-height: 44rpx;
		font-size: 20rpx;
	}

	.article-copy,
	.article-social-meta,
	.comment-list {
		margin-top: 16rpx;
	}

	.article-social-meta__item,
	.article-social-meta__dot {
		font-size: 18rpx;
	}

	.article-title {
		font-size: 32rpx;
		line-height: 1.24;
	}

	.article-content,
	.comment-content,
	.reply-content {
		margin-top: 12rpx;
		font-size: 24rpx;
		line-height: 1.6;
	}

	.article-topic-inline {
		margin-top: 10rpx;
		font-size: 20rpx;
	}

	.article-images {
		margin-top: 16rpx;
		gap: 10rpx;
	}

	.article-image {
		width: calc((100% - 10rpx) / 2);
		height: 152rpx;
		border-radius: 18rpx;
		box-shadow: 0 8rpx 20rpx rgba(15, 23, 42, 0.08);
	}

	.article-images--single .article-image--single {
		width: 100%;
		height: 252rpx;
	}

	.comment-toolbar__summary {
		font-size: 20rpx;
	}

	.comment-item {
		gap: 10rpx;
	}

	.comment-hit {
		padding: 16rpx;
		border-radius: 20rpx;
	}

	.comment-action-tip {
		display: none;
	}

	.reply-list {
		padding-left: 16rpx;
	}

	.reply-item {
		padding: 16rpx;
	}

	.load-more-status {
		padding: 32rpx 0;
		font-size: 22rpx;
	}

	.toast-tip {
		left: 20rpx;
		right: 20rpx;
		bottom: calc(194rpx + env(safe-area-inset-bottom));
		padding: 16rpx;
		font-size: 20rpx;
	}

	.detail-composer-wrap {
		padding: 12rpx 20rpx calc(12rpx + env(safe-area-inset-bottom));
	}

	.compact-action-bar {
		gap: 12rpx;
	}

	.compact-comment-trigger {
		height: 60rpx;
		padding: 0 16rpx;
		gap: 10rpx;
	}

	.compact-comment-trigger__text {
		font-size: 24rpx;
	}

	.compact-actions {
		gap: 14rpx;
	}

	.compact-action-btn__icon {
		font-size: 34rpx;
	}

	.compact-action-btn__count {
		font-size: 20rpx;
	}

	.composer-head {
		margin-bottom: 10rpx;
		align-items: flex-start;
		flex-wrap: wrap;
	}

	.composer-head__action {
		font-size: 20rpx;
	}

	.composer-input-shell {
		padding: 14rpx 18rpx;
		border-radius: 20rpx;
	}

	.composer-input-shell--expanded {
		min-height: 124rpx;
	}

	.composer-textarea {
		font-size: 24rpx;
	}

	.composer-extra {
		margin-top: 10rpx;
	}

	.xhs-tool-btn {
		width: 44rpx;
		height: 44rpx;
		font-size: 24rpx;
	}

	.send-btn {
		width: 104rpx;
		height: 52rpx;
		font-size: 24rpx;
	}

	.emoji-chip {
		width: 56rpx;
		height: 56rpx;
		border-radius: 18rpx;
		font-size: 24rpx;
	}
}
</style>
