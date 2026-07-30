<template>
	<view class="page-shell community-page" :class="themeClass">
		<view class="primary-tab-shell">
			<view class="primary-tab-row">
				<view
					v-for="tab in primaryTabs"
					:key="tab.id"
					class="primary-tab"
					:class="{ 'primary-tab--active': activePrimaryTab === tab.id }"
					@click="switchPrimaryTab(tab.id)"
				>
					<text class="primary-tab__text">{{ tab.label }}</text>
				</view>
			</view>
		</view>

		<scroll-view scroll-y class="community-scroll" @scroll="handleScroll">
			<view class="community-inner">
				<view
					class="hero-card glass-card"
					:class="{
						'hero-card--collapsed': activePrimaryTab === 'home' && heroCollapsed,
						'hero-card--simple': activePrimaryTab !== 'home'
					}"
				>
					<view class="hero-top">
						<view class="hero-copy">
							<view v-if="activePrimaryTab === 'home'" class="hero-badge-row">
								<text class="hero-badge">早安</text>
								<text class="hero-subtitle">今天先轻松看看邻里新鲜事</text>
							</view>
							<text class="hero-title">{{ heroTitle }}</text>
							<text class="hero-desc">{{ heroDesc }}</text>
						</view>
						<view class="hero-icon">{{ heroIcon }}</view>
					</view>
					<view v-if="activePrimaryTab === 'home'" class="search-pill">
						<text class="search-emoji">⌕</text>
						<input
							v-model.trim="keyword"
							class="search-input"
							placeholder="搜帖子"
							placeholder-class="search-placeholder"
							confirm-type="search"
						/>
					</view>
				</view>

				<view v-if="activePrimaryTab === 'home'" class="section-head">
					<view>
						<text class="section-title">先逛一圈</text>
						<text class="section-desc">推荐和内容分类都能直接切着看。</text>
					</view>
					<text class="section-badge">{{ homeTopics.length }} 个话题</text>
				</view>

				<view v-if="activePrimaryTab === 'home'" class="glass-card topic-shell">
					<scroll-view scroll-x class="topic-scroll" show-scrollbar="false">
						<view class="topic-row">
							<view
								v-for="topic in homeTopics"
								:key="topic.id"
								class="topic-pill"
								:class="{ 'topic-pill--active': homeTopic === topic.id }"
								@click="switchTopic(topic.id)"
							>
								<text class="topic-pill__text">{{ topic.label }}</text>
							</view>
						</view>
					</scroll-view>
				</view>

				<view class="feed-list">
					<view
						v-for="post in filteredPosts"
						:key="post.id"
						class="feed-card glass-card"
						:class="{ 'feed-card--featured': post.featured }"
						@click="openPost(post.id)"
					>
						<view class="feed-head">
							<view class="feed-author">
								<view class="avatar-chip" :class="{ 'avatar-chip--filled': post.featured }">{{ avatarText(post.author) }}</view>
								<view class="feed-author-copy">
									<text class="feed-author-name">{{ post.author }}</text>
									<text class="feed-meta">{{ formatCommunityTime(post.publishedAt) }} · {{ post.location }}</text>
								</view>
							</view>
							<text class="feed-tag">{{ post.topicLabel }}</text>
						</view>

						<view class="feed-body">
							<text class="feed-title">{{ post.title }}</text>
							<text class="feed-topic-inline" v-if="post.topicLabel"># {{ post.topicLabel }}</text>
							<text class="feed-content">{{ post.content }}</text>
						</view>

						<view v-if="post.imageList && post.imageList.length" class="feed-image-row">
							<image
								v-for="(image, index) in post.imageList.slice(0, 3)"
								:key="index"
								class="feed-image"
								:src="image"
								mode="aspectFill"
							/>
						</view>

						<view class="feed-footer">
							<view class="feed-stats">
								<text>{{ post.stats.comments }} 条回应</text>
								<text>{{ post.stats.saves }} 人收藏</text>
							</view>
							<text class="feed-link">进去看看 ›</text>
						</view>
					</view>
				</view>

				<view v-if="!filteredPosts.length" class="empty-card glass-card">
					<text class="empty-title">{{ emptyTitle }}</text>
					<text class="empty-desc">{{ emptyDesc }}</text>
				</view>
			</view>
		</scroll-view>

		<view class="floating-compose" @click="goCompose">
			<text class="floating-compose__icon">✎</text>
			<text>发帖</text>
		</view>

		<bottom-nav current="community" />

	</view>
</template>

<script>
import BottomNav from '@/components/bottom-nav.vue'
import {
	formatCommunityTime,
	getCommunityPosts,
	getCommunityTopics
} from '@/utils/community-data.js'

export default {
	components: {
		BottomNav
	},
	data() {
		return {
			keyword: '',
			activePrimaryTab: 'home',
			homeTopic: 'recommended',
			topics: [],
			posts: [],
			heroCollapsed: false
		}
	},
	computed: {
		primaryTabs() {
			return [
				{ id: 'home', label: '主页' },
				{ id: 'following', label: '关注' },
				{ id: 'saved', label: '收藏' }
			]
		},
		homeTopics() {
			const topics = this.topics.filter((item) => !['following', 'saved'].includes(item.id))
			const recommendedIndex = topics.findIndex((item) => item.id === 'recommended')
			if (recommendedIndex <= 0) return topics
			const [recommendedTopic] = topics.splice(recommendedIndex, 1)
			topics.unshift(recommendedTopic)
			return topics
		},
		heroTitle() {
			if (this.activePrimaryTab === 'following') return '我的关注'
			if (this.activePrimaryTab === 'saved') return '我的收藏'
			return '社区'
		},
		heroDesc() {
			if (this.activePrimaryTab === 'following') return '只看你关注的人最近发了什么。'
			if (this.activePrimaryTab === 'saved') return '把你收藏过的帖子集中放在这里。'
			return '看看大家正在聊什么，顺手也能发一条自己的近况。'
		},
		heroIcon() {
			if (this.activePrimaryTab === 'following') return '◎'
			if (this.activePrimaryTab === 'saved') return '☆'
			return '✦'
		},
		filteredPosts() {
			if (this.activePrimaryTab !== 'home') return this.posts
			const keyword = String(this.keyword || '').trim().toLowerCase()
			return this.posts.filter((post) => {
				if (!keyword) return true
				return [post.title, post.content, post.author, post.location, post.topicLabel]
					.join(' ')
					.toLowerCase()
					.includes(keyword)
			})
		},
		emptyTitle() {
			if (this.activePrimaryTab === 'following') return '关注的人还没更新'
			if (this.activePrimaryTab === 'saved') return '还没有收藏内容'
			return '暂时没搜到内容'
		},
		emptyDesc() {
			if (this.activePrimaryTab === 'following') return '晚点再回来看看，或者先去主页逛逛。'
			if (this.activePrimaryTab === 'saved') return '看到想回看的帖子时点一下收藏，这里就会出现。'
			return '换个关键词，或者切到别的话题看看。'
		}
	},
	onShow() {
		uni.hideTabBar()
		this.loadData()
	},
	methods: {
		formatCommunityTime,
		getCurrentCategory() {
			if (this.activePrimaryTab === 'following') return 'following'
			if (this.activePrimaryTab === 'saved') return 'saved'
			return this.homeTopic
		},
		async loadData() {
			try {
				const [topics, posts] = await Promise.all([
					getCommunityTopics(),
					getCommunityPosts({ category: this.getCurrentCategory() })
				])
				this.topics = Array.isArray(topics) ? topics : []
				this.posts = Array.isArray(posts) ? posts : []
			} catch (error) {
				uni.showToast({
					title: '社区内容加载失败',
					icon: 'none'
				})
			}
		},
		async switchPrimaryTab(tabId) {
			if (this.activePrimaryTab === tabId) return
			this.activePrimaryTab = tabId
			this.heroCollapsed = false
			try {
				this.posts = await getCommunityPosts({ category: this.getCurrentCategory() })
			} catch (error) {
				uni.showToast({
					title: '切换失败，请稍后再试',
					icon: 'none'
				})
			}
		},
		async switchTopic(topicId) {
			this.homeTopic = topicId
			try {
				this.posts = await getCommunityPosts({ category: topicId })
			} catch (error) {
				uni.showToast({
					title: '话题加载失败',
					icon: 'none'
				})
			}
		},
		handleScroll(event) {
			if (this.activePrimaryTab !== 'home') {
				this.heroCollapsed = false
				return
			}
			const scrollTop = Number(event && event.detail ? event.detail.scrollTop : 0) || 0
			if (!this.heroCollapsed && scrollTop > 44) {
				this.heroCollapsed = true
				return
			}
			if (this.heroCollapsed && scrollTop <= 8) {
				this.heroCollapsed = false
			}
		},
		openPost(postId) {
			if (!postId) return
			uni.navigateTo({
				url: `/pages/community/post-detail?id=${postId}`
			})
		},
		goCompose() {
			uni.navigateTo({
				url: '/pages/community/compose'
			})
		},
		avatarText(name) {
			return String(name || '邻居').slice(0, 2)
		}
	}
}
</script>

<style scoped lang="scss">
.community-page {
	min-height: 100vh;
}

.community-scroll {
	height: 100vh;
}

.community-inner {
	padding: calc(168rpx + env(safe-area-inset-top)) 32rpx calc(220rpx + env(safe-area-inset-bottom));
	display: flex;
	flex-direction: column;
	gap: 24rpx;
	box-sizing: border-box;
}

.glass-card {
	background: var(--glass-bg);
	border: var(--card-border);
	box-shadow: var(--card-shadow-sm);
	backdrop-filter: blur(28rpx);
	-webkit-backdrop-filter: blur(28rpx);
	box-sizing: border-box;
}

.hero-card,
.topic-shell,
.feed-card,
.creator-card,
.empty-card {
	border-radius: 32rpx;
	padding: 28rpx;
}

.hero-card {
	overflow: hidden;
	transition:
		padding 0.24s ease,
		border-radius 0.24s ease,
		box-shadow 0.24s ease;
}

.primary-tab-shell {
	position: fixed;
	left: 32rpx;
	right: 32rpx;
	top: calc(12rpx + env(safe-area-inset-top));
	z-index: 12;
	padding: 12rpx;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.78);
	border: 1px solid rgba(255, 255, 255, 0.3);
	box-shadow:
		0 12rpx 30rpx rgba(15, 23, 42, 0.08),
		inset 0 1px 0 rgba(255, 255, 255, 0.4);
	backdrop-filter: blur(24rpx);
	-webkit-backdrop-filter: blur(24rpx);
}

.theme-dark .primary-tab-shell {
	background: var(--ios-glass-bg);
	border-color: var(--ios-glass-border);
	box-shadow:
		var(--ios-glass-shadow),
		var(--ios-glass-highlight);
	backdrop-filter: blur(36rpx) saturate(165%);
	-webkit-backdrop-filter: blur(36rpx) saturate(165%);
}

.primary-tab-row {
	display: flex;
	align-items: center;
	gap: 8rpx;
}

.primary-tab {
	flex: 1;
	min-width: 0;
	height: 76rpx;
	border-radius: 999rpx;
	background: rgba(255, 255, 255, 0.12);
	display: inline-flex;
	align-items: center;
	justify-content: center;
	padding: 0 22rpx;
	transition:
		background 0.24s ease,
		box-shadow 0.24s ease,
		color 0.24s ease,
		transform 0.2s ease;
}

.theme-dark .primary-tab {
	background: rgba(255, 255, 255, 0.04);
	box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.primary-tab--active {
	background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
	box-shadow: 0 10rpx 24rpx rgba(66, 200, 255, 0.28);
}

.theme-dark .primary-tab--active {
	background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
	box-shadow: 0 10rpx 24rpx rgba(39, 179, 255, 0.3);
}

.primary-tab__text {
	color: var(--text-secondary);
	font-size: 26rpx;
	font-weight: 700;
	letter-spacing: 0.02em;
}

.primary-tab--active .primary-tab__text {
	color: #ffffff;
}

.hero-card--collapsed {
	padding-top: 22rpx;
	padding-bottom: 20rpx;
}

.hero-card--simple .hero-top {
	align-items: center;
}

.hero-card--simple .hero-title {
	margin-top: 0;
	font-size: 44rpx;
	line-height: 1.12;
}

.hero-card--simple .hero-desc {
	margin-top: 10rpx;
	max-width: none;
}

.hero-top {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 24rpx;
	transition: gap 0.24s ease;
}

.hero-copy {
	flex: 1;
	min-width: 0;
}

.hero-badge-row {
	display: flex;
	align-items: center;
	flex-wrap: wrap;
	gap: 12rpx;
	transition: gap 0.24s ease;
}

.hero-badge,
.section-badge,
.feed-tag,
.creator-badge {
	display: inline-flex;
	align-items: center;
	justify-content: center;
	padding: 0 20rpx;
	min-height: 44rpx;
	border-radius: 999rpx;
	font-size: 22rpx;
	font-weight: 600;
}

.hero-badge,
.section-badge,
.feed-tag {
	background: var(--control-bg);
	color: var(--accent-color);
}

.hero-subtitle,
.section-desc,
.feed-meta,
.feed-content,
.creator-desc,
.empty-desc {
	display: block;
	color: var(--text-secondary);
	font-size: 24rpx;
	line-height: 1.6;
}

.hero-subtitle {
	max-height: 80rpx;
	overflow: hidden;
	opacity: 1;
	transition:
		max-height 0.24s ease,
		opacity 0.2s ease;
}

.hero-title {
	display: block;
	margin-top: 20rpx;
	color: var(--text-primary);
	font-size: 60rpx;
	font-weight: 700;
	line-height: 1.06;
	letter-spacing: -0.04em;
	transition:
		margin-top 0.24s ease,
		font-size 0.24s ease,
		line-height 0.24s ease;
}

.hero-desc {
	display: block;
	margin-top: 16rpx;
	max-width: 480rpx;
	color: var(--text-secondary);
	font-size: 28rpx;
	line-height: 1.6;
	max-height: 160rpx;
	overflow: hidden;
	opacity: 1;
	transition:
		margin-top 0.24s ease,
		max-height 0.24s ease,
		opacity 0.2s ease;
}

.hero-icon {
	width: 96rpx;
	height: 96rpx;
	border-radius: 999rpx;
	display: flex;
	align-items: center;
	justify-content: center;
	background: var(--control-bg);
	color: var(--accent-color);
	font-size: 40rpx;
	font-weight: 700;
	flex-shrink: 0;
	transition:
		width 0.24s ease,
		height 0.24s ease,
		font-size 0.24s ease,
		opacity 0.24s ease;
}

.search-pill {
	margin-top: 20rpx;
	min-height: 72rpx;
	max-height: 80rpx;
	border-radius: 999rpx;
	padding: 0 20rpx;
	background: rgba(255, 255, 255, 0.58);
	border: 1px solid var(--border-color);
	display: flex;
	align-items: center;
	gap: 12rpx;
	overflow: hidden;
	opacity: 1;
	transform: translateY(0);
	transition:
		margin-top 0.24s ease,
		max-height 0.24s ease,
		opacity 0.18s ease,
		transform 0.24s ease,
		border-color 0.24s ease,
		padding 0.24s ease;
}

.search-emoji,
.search-input {
	color: var(--text-primary);
}

.search-emoji {
	color: var(--text-secondary);
	font-size: 22rpx;
	flex-shrink: 0;
}

.search-input {
	flex: 1;
	min-width: 0;
	height: 72rpx;
	font-size: 26rpx;
}

.search-placeholder {
	color: var(--text-tertiary);
}

.theme-dark .search-pill {
	background: var(--ios-glass-bg-soft);
	border-color: rgba(255, 255, 255, 0.08);
	box-shadow:
		inset 0 1px 0 rgba(255, 255, 255, 0.08),
		0 8rpx 24rpx rgba(2, 8, 20, 0.22);
	backdrop-filter: blur(28rpx) saturate(155%);
	-webkit-backdrop-filter: blur(28rpx) saturate(155%);
}

.hero-card--collapsed .search-emoji,
.hero-card--collapsed .search-input {
	opacity: 0;
}

.hero-card--collapsed .hero-top {
	gap: 16rpx;
}

.hero-card--collapsed .hero-badge-row {
	gap: 8rpx;
}

.hero-card--collapsed .hero-subtitle,
.hero-card--collapsed .hero-desc {
	max-height: 0;
	opacity: 0;
}

.hero-card--collapsed .hero-desc {
	margin-top: 0;
}

.hero-card--collapsed .hero-title {
	margin-top: 10rpx;
	font-size: 42rpx;
	line-height: 1.12;
}

.hero-card--collapsed .hero-icon {
	width: 72rpx;
	height: 72rpx;
	font-size: 30rpx;
	opacity: 0.9;
}

.hero-card--collapsed .search-pill {
	margin-top: 0;
	min-height: 0;
	max-height: 0;
	padding-top: 0;
	padding-bottom: 0;
	border-color: transparent;
	opacity: 0;
	transform: translateY(-12rpx);
	pointer-events: none;
}

.section-head {
	padding: 4rpx 8rpx 0;
	display: flex;
	align-items: flex-end;
	justify-content: space-between;
	gap: 24rpx;
}

.section-title {
	display: block;
	color: var(--text-primary);
	font-size: 30rpx;
	font-weight: 700;
}

.section-title--large {
	font-size: 38rpx;
}

.ghost-btn {
	min-width: 108rpx;
	height: 72rpx;
	border-radius: 999rpx;
	background: var(--glass-bg);
	border: 1px solid var(--border-color);
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--text-secondary);
	font-size: 24rpx;
}

.topic-scroll {
	white-space: nowrap;
}

.topic-row {
	display: inline-flex;
	gap: 16rpx;
}

.topic-pill {
	min-width: 112rpx;
	height: 80rpx;
	padding: 0 28rpx;
	border-radius: 999rpx;
	background: var(--control-bg);
	display: inline-flex;
	align-items: center;
	justify-content: center;
}

.topic-pill--active {
	background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
	box-shadow: var(--btn-shadow);
}

.topic-pill__text {
	color: var(--text-secondary);
	font-size: 26rpx;
	font-weight: 600;
}

.topic-pill--active .topic-pill__text {
	color: #ffffff;
}

.feed-list,
.creator-list {
	display: flex;
	flex-direction: column;
	gap: 20rpx;
}
.feed-title,
.creator-title,
.empty-title {
	display: block;
	color: var(--text-primary);
	font-weight: 700;
}

.feed-card--featured {
	box-shadow: var(--card-shadow-md);
}

.feed-head,
.feed-footer,
.creator-item {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 20rpx;
}

.feed-author {
	display: flex;
	align-items: center;
	gap: 20rpx;
	flex: 1;
	min-width: 0;
}

.avatar-chip {
	width: 88rpx;
	height: 88rpx;
	border-radius: 999rpx;
	background: var(--control-bg);
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--accent-color);
	font-size: 26rpx;
	font-weight: 700;
	flex-shrink: 0;
}

.avatar-chip--filled {
	background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
	color: #ffffff;
}

.avatar-chip--soft {
	width: 72rpx;
	height: 72rpx;
}

.feed-author-copy,
.creator-copy {
	flex: 1;
	min-width: 0;
}

.feed-author-name,
.creator-name {
	display: block;
	color: var(--text-primary);
	font-size: 28rpx;
	font-weight: 700;
}

.feed-body {
	margin-top: 24rpx;
}

.feed-title {
	font-size: 36rpx;
}

.feed-content {
	margin-top: 16rpx;
	max-height: 134rpx;
	overflow: hidden;
}

.feed-topic-inline {
	display: inline-block;
	margin-top: 14rpx;
	color: var(--accent-color);
	font-size: 24rpx;
	font-weight: 700;
}

.feed-image-row {
	margin-top: 20rpx;
	display: flex;
	align-items: center;
	gap: 12rpx;
	overflow: hidden;
}

.feed-image {
	flex: 1;
	min-width: 0;
	height: 184rpx;
	border-radius: 24rpx;
}

.feed-footer {
	margin-top: 24rpx;
	padding-top: 24rpx;
	border-top: 1px solid var(--border-color);
	align-items: flex-start;
}

.feed-stats {
	display: flex;
	flex-wrap: wrap;
	gap: 24rpx;
	color: var(--text-secondary);
	font-size: 24rpx;
}

.feed-link {
	color: var(--accent-color);
	font-size: 26rpx;
	font-weight: 600;
	flex-shrink: 0;
}

.creator-title,
.empty-title {
	font-size: 30rpx;
}

.creator-item {
	align-items: flex-start;
}

.creator-badge,
.creator-badge {
	background: var(--control-bg);
	color: var(--text-secondary);
	border-radius: 20rpx;
}

.empty-card {
	display: flex;
	flex-direction: column;
	justify-content: center;
	gap: 12rpx;
	align-items: center;
	text-align: center;
}

.floating-compose {
	position: fixed;
	right: 32rpx;
	bottom: calc(156rpx + env(safe-area-inset-bottom));
	height: 112rpx;
	padding: 0 34rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
	box-shadow: var(--btn-shadow);
	display: flex;
	align-items: center;
	justify-content: center;
	gap: 12rpx;
	color: #ffffff;
	font-size: 30rpx;
	font-weight: 700;
	z-index: 20;
}

.floating-compose__icon {
	font-size: 28rpx;
}

@media (min-width: 600px) {
	.primary-tab-shell {
		left: 50%;
		right: auto;
		width: calc(100% - 64px);
		max-width: 840rpx;
		transform: translateX(-50%);
	}

	.community-inner {
		max-width: 840rpx;
		margin: 0 auto;
	}

	.floating-compose {
		left: 50%;
		right: auto;
		transform: translateX(252rpx);
	}
}

@media (min-width: 900px) {
	.primary-tab-shell {
		max-width: 1040rpx;
	}

	.community-inner {
		max-width: 1040rpx;
	}

	.floating-compose {
		transform: translateX(352rpx);
	}
}

@media (max-width: 480px) {
	.community-page {
		min-height: 100vh;
	}

	.community-inner {
		padding: calc(128rpx + env(safe-area-inset-top)) 20rpx calc(184rpx + env(safe-area-inset-bottom));
		gap: 16rpx;
	}

	.hero-card,
	.topic-shell,
	.feed-card,
	.creator-card,
	.empty-card {
		border-radius: 24rpx;
		padding: 22rpx;
	}

	.hero-card--collapsed {
		padding-top: 18rpx;
		padding-bottom: 16rpx;
	}

	.primary-tab-shell {
		left: 20rpx;
		right: 20rpx;
		padding: 10rpx;
		top: calc(8rpx + env(safe-area-inset-top));
	}

	.primary-tab-row {
		gap: 6rpx;
	}

	.primary-tab {
		height: 62rpx;
		padding: 0 14rpx;
	}

	.primary-tab__text {
		font-size: 22rpx;
	}

	.hero-top {
		gap: 14rpx;
	}

	.hero-title {
		margin-top: 12rpx;
		font-size: 48rpx;
	}

	.hero-desc {
		max-width: none;
		font-size: 24rpx;
	}

	.hero-icon {
		width: 76rpx;
		height: 76rpx;
		font-size: 30rpx;
	}

	.search-pill {
		margin-top: 14rpx;
		min-height: 64rpx;
		max-height: 70rpx;
		padding: 0 16rpx;
		gap: 8rpx;
	}

	.search-input {
		height: 64rpx;
		font-size: 24rpx;
	}

	.search-emoji {
		font-size: 20rpx;
	}

	.hero-card--collapsed .hero-title {
		margin-top: 8rpx;
		font-size: 34rpx;
	}

	.hero-card--simple .hero-title {
		font-size: 34rpx;
	}

	.hero-card--collapsed .hero-icon {
		width: 56rpx;
		height: 56rpx;
		font-size: 22rpx;
	}

	.section-head {
		padding: 0 4rpx;
		align-items: flex-start;
		flex-wrap: wrap;
		gap: 10rpx;
	}

	.section-title {
		font-size: 26rpx;
	}

	.section-title--large {
		font-size: 30rpx;
	}

	.section-badge,
	.hero-badge,
	.feed-tag,
	.creator-badge {
		padding: 0 18rpx;
		min-height: 36rpx;
		font-size: 18rpx;
	}

	.ghost-btn {
		min-width: 88rpx;
		height: 56rpx;
		font-size: 20rpx;
	}

	.topic-shell {
		padding: 18rpx 22rpx;
	}

	.topic-row {
		gap: 10rpx;
	}

	.topic-pill {
		min-width: 92rpx;
		height: 64rpx;
		padding: 0 18rpx;
	}

	.topic-pill__text {
		font-size: 22rpx;
	}

	.feed-list,
	.creator-list {
		gap: 16rpx;
	}

	.feed-head,
	.feed-footer,
	.creator-item {
		align-items: flex-start;
	}

	.feed-head {
		gap: 16rpx;
	}

	.feed-author {
		gap: 14rpx;
	}

	.avatar-chip {
		width: 72rpx;
		height: 72rpx;
		font-size: 22rpx;
	}

	.feed-title {
		font-size: 30rpx;
	}

	.feed-content {
		margin-top: 10rpx;
		max-height: 108rpx;
		font-size: 22rpx;
	}

	.feed-topic-inline {
		margin-top: 10rpx;
		font-size: 20rpx;
	}

	.feed-image {
		height: 152rpx;
		border-radius: 18rpx;
	}

	.feed-footer {
		margin-top: 16rpx;
		padding-top: 16rpx;
		flex-wrap: wrap;
		gap: 10rpx;
	}

	.feed-stats {
		gap: 16rpx;
		font-size: 22rpx;
	}

	.feed-link {
		font-size: 22rpx;
	}

	.creator-badge {
		flex-shrink: 0;
	}

	.floating-compose {
		right: 16rpx;
		bottom: calc(120rpx + env(safe-area-inset-bottom));
		height: 84rpx;
		padding: 0 22rpx;
		gap: 6rpx;
		font-size: 24rpx;
	}

	.floating-compose__icon {
		font-size: 22rpx;
	}
}
</style>
