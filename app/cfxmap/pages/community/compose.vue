<template>
	<view class="page-shell compose-page" :class="themeClass">
		<scroll-view scroll-y class="compose-scroll">
			<view class="compose-inner">
				<view class="compose-header">
					<view class="back-pill" @click="goBack">
						<text class="back-pill__icon">‹</text>
						<text>返回社区</text>
					</view>
					<view class="compose-header__copy">
						<text class="compose-header__title">发帖</text>
						<text class="compose-header__desc">写下此刻想说的话，简单整理一下就能发出。</text>
					</view>
				</view>

				<view class="glass-card prompt-card">
					<view class="prompt-icon">✎</view>
					<view class="prompt-copy">
						<text class="prompt-title">轻松分享就好</text>
						<text class="prompt-desc">不需要复杂排版，标题点明主题，正文把重点说明白即可。</text>
					</view>
				</view>

				<view class="glass-card form-card">
					<view class="field-head">
						<text class="field-title">标题</text>
						<text class="field-tip">一句话即可</text>
					</view>
					<input
						v-model="form.title"
						class="field-input"
						maxlength="24"
						placeholder="比如：今天终于把阳台整理好了"
						placeholder-class="field-placeholder"
						@input="persistDraft"
					/>
					<text class="field-helper">标题和正文填好后就能发布。</text>
				</view>

				<view class="glass-card form-card">
					<view class="field-head">
						<text class="field-title">正文</text>
						<text class="field-tip">支持 # 话题</text>
					</view>
					<textarea
						v-model="form.content"
						class="field-textarea"
						auto-height
						maxlength="500"
						placeholder="把你的想法、经历或者问题写下来，像平时聊天一样自然就行。"
						placeholder-class="field-placeholder"
						@input="persistDraft"
					/>
					<view class="field-footer">
						<text class="field-helper">输入 `#` 就会带出历史话题，格式改为 `#话题 `。</text>
						<text class="field-tip">{{ form.content.length }}/500</text>
					</view>
					<view v-if="topicSuggestions.length" class="topic-suggestion-box">
						<view class="topic-suggestion-head">
							<text class="topic-suggestion-title">历史话题</text>
							<text class="topic-suggestion-tip">点一下直接带入</text>
						</view>
						<view class="topic-suggestion-list">
							<view
								v-for="topic in topicSuggestions"
								:key="topic.topicId || topic.id"
								class="topic-suggestion-pill"
								@click="applyTopicSuggestion(topic)"
							>
								<text class="topic-suggestion-pill__text"># {{ topic.label || topic.topicName || topic.id }}</text>
							</view>
						</view>
					</view>
				</view>

				<view class="glass-card form-card">
					<view class="field-head">
						<text class="field-title">图片</text>
						<text class="field-tip">可选</text>
					</view>
					<view class="image-picker" @click="chooseImage">
						<view class="image-picker__icon">⊕</view>
						<view class="image-picker__copy">
							<text class="prompt-title">添加图片</text>
							<text class="prompt-desc">有图就更直观，没有也可以直接发文字内容。</text>
						</view>
						<text class="image-picker__arrow">›</text>
					</view>

					<view v-if="form.imageList.length" class="image-grid">
						<view v-for="(image, index) in form.imageList" :key="image" class="image-grid__item">
							<image class="image-grid__img" :src="image" mode="aspectFill" @click="previewImage(index)" />
							<view class="image-grid__remove" @click.stop="removeImage(index)">×</view>
						</view>
					</view>
				</view>
			</view>
		</scroll-view>

		<view class="compose-footer glass-footer">
			<text class="compose-footer__tip">发布后会回到社区，你也可以稍后再继续补充内容。</text>
			<view class="publish-btn" @click="submitPost">
				<text>发布帖子</text>
				<text class="publish-btn__arrow">↗</text>
			</view>
		</view>
	</view>
</template>

<script>
import {
	clearCommunityDraft,
	getCommunityDraft,
	getCommunityComposeTopics,
	publishCommunityPost,
	saveCommunityDraft,
	uploadCommunityImage
} from '@/utils/community-data.js'

export default {
	data() {
		return {
			topicSuggestions: [],
			topicRequestToken: 0,
			form: {
				title: '',
				content: '',
				imageList: []
			}
		}
	},
	async onLoad() {
		this.form = getCommunityDraft()
		await this.refreshTopicSuggestions()
	},
	methods: {
		goBack() {
			if (getCurrentPages().length > 1) {
				uni.navigateBack()
				return
			}
			uni.switchTab({
				url: '/pages/community/community'
			})
		},
		persistDraft() {
			this.form = saveCommunityDraft(this.form)
			this.refreshTopicSuggestions()
		},
		async refreshTopicSuggestions() {
			const content = String(this.form.content || '')
			const match = content.match(/(?:^|\n)#([^\s\n#]*)$/)
			if (!match) {
				this.topicSuggestions = []
				return
			}
			const keyword = String(match[1] || '').trim()
			const requestToken = ++this.topicRequestToken
			try {
				const topics = await getCommunityComposeTopics({
					keyword,
					limit: 6
				})
				if (requestToken !== this.topicRequestToken) return
				this.topicSuggestions = Array.isArray(topics) ? topics : []
			} catch (error) {
				if (requestToken !== this.topicRequestToken) return
				this.topicSuggestions = []
			}
		},
		applyTopicSuggestion(topic) {
			const label = String(topic.label || topic.topicName || topic.id || '').trim()
			if (!label) return
			this.form.content = String(this.form.content || '').replace(/(?:^|\n)#([^\s\n#]*)$/, (matched) => {
				const prefix = matched.startsWith('\n') ? '\n' : ''
				return `${prefix}#${label} `
			})
			this.persistDraft()
		},
		async chooseImage() {
			if (typeof uni.chooseImage !== 'function') {
				uni.showToast({
					title: '当前端不支持选图',
					icon: 'none'
				})
				return
			}
			uni.chooseImage({
				count: 3 - this.form.imageList.length,
				sizeType: ['compressed'],
				success: async (result) => {
					const files = result.tempFilePaths || []
					if (!files.length) return
					uni.showLoading({
						title: '上传中',
						mask: true
					})
					try {
						const uploaded = []
						for (const file of files) {
							const url = await uploadCommunityImage(file)
							if (url) {
								uploaded.push(url)
							}
						}
						this.form.imageList = this.form.imageList.concat(uploaded).slice(0, 3)
						this.persistDraft()
					} catch (error) {
						uni.showToast({
							title: '图片上传失败',
							icon: 'none'
						})
					} finally {
						uni.hideLoading()
					}
				}
			})
		},
		removeImage(index) {
			this.form.imageList.splice(index, 1)
			this.persistDraft()
		},
		previewImage(index) {
			uni.previewImage({
				current: this.form.imageList[index],
				urls: this.form.imageList
			})
		},
		async submitPost() {
			const title = String(this.form.title || '').trim()
			const content = String(this.form.content || '').trim()
			if (!title) {
				uni.showToast({
					title: '请先写标题',
					icon: 'none'
				})
				return
			}
			if (!content) {
				uni.showToast({
					title: '请先补充正文',
					icon: 'none'
				})
				return
			}

			try {
				const created = await publishCommunityPost({
					title,
					content,
					imageList: this.form.imageList
				})
				if (!created) return

				clearCommunityDraft()
				uni.showToast({
					title: '发布成功',
					icon: 'success'
				})
				setTimeout(() => {
					uni.switchTab({
						url: '/pages/community/community'
					})
				}, 500)
			} catch (error) {
				uni.showToast({
					title: '发布失败，请稍后重试',
					icon: 'none'
				})
			}
		}
	}
}
</script>

<style scoped lang="scss">
.compose-page {
	min-height: 100vh;
	padding-bottom: calc(180rpx + env(safe-area-inset-bottom));
}

.compose-scroll {
	height: 100vh;
}

.compose-inner {
	padding: calc(24rpx + env(safe-area-inset-top)) 32rpx 240rpx;
	display: flex;
	flex-direction: column;
	gap: 24rpx;
	box-sizing: border-box;
}

.glass-card,
.glass-footer {
	background: var(--glass-bg);
	border: var(--card-border);
	box-shadow: var(--card-shadow-sm);
	backdrop-filter: blur(28rpx);
	-webkit-backdrop-filter: blur(28rpx);
	box-sizing: border-box;
}

.compose-header {
	display: flex;
	align-items: flex-start;
	gap: 20rpx;
}

.back-pill,
.publish-btn,
.image-grid__remove {
	display: inline-flex;
	align-items: center;
	justify-content: center;
}

.back-pill {
	height: 88rpx;
	padding: 0 28rpx;
	border-radius: 999rpx;
	background: var(--glass-bg);
	border: 1px solid var(--border-color);
	color: var(--text-primary);
	font-size: 28rpx;
	font-weight: 700;
	gap: 8rpx;
	flex-shrink: 0;
}

.back-pill__icon {
	font-size: 38rpx;
}

.compose-header__copy {
	flex: 1;
	min-width: 0;
}

.compose-header__title,
.prompt-title,
.field-title {
	display: block;
	color: var(--text-primary);
	font-size: 30rpx;
	font-weight: 700;
}

.compose-header__desc,
.prompt-desc,
.field-tip,
.field-helper,
.compose-footer__tip {
	display: block;
	color: var(--text-secondary);
	font-size: 24rpx;
	line-height: 1.6;
}

.compose-header__desc {
	margin-top: 8rpx;
}

.prompt-card,
.form-card {
	border-radius: 32rpx;
	padding: 28rpx;
}

.prompt-card {
	display: flex;
	align-items: flex-start;
	gap: 20rpx;
}

.prompt-icon,
.image-picker__icon {
	width: 88rpx;
	height: 88rpx;
	border-radius: 28rpx;
	background: var(--control-bg);
	display: flex;
	align-items: center;
	justify-content: center;
	color: var(--accent-color);
	font-size: 36rpx;
	font-weight: 700;
	flex-shrink: 0;
}

.prompt-copy {
	flex: 1;
	min-width: 0;
}

.prompt-desc {
	margin-top: 8rpx;
}

.field-head,
.field-footer,
.image-picker {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 16rpx;
}

.field-input,
.field-textarea {
	margin-top: 20rpx;
	width: 100%;
	border-radius: 28rpx;
	border: 1px solid var(--border-color);
	background: var(--card-bg-strong);
	color: var(--text-primary);
	font-size: 30rpx;
}

.field-input {
	height: 88rpx;
	padding: 0 24rpx;
}

.field-textarea {
	min-height: 260rpx;
	padding: 24rpx;
	line-height: 1.8;
}

.field-placeholder {
	color: var(--text-tertiary);
}

.field-helper {
	margin-top: 16rpx;
}

.field-footer {
	margin-top: 16rpx;
	align-items: flex-start;
}

.topic-suggestion-box {
	margin-top: 18rpx;
	padding: 20rpx;
	border-radius: 24rpx;
	background: var(--card-bg-strong);
	border: 1px solid var(--border-color);
}

.topic-suggestion-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12rpx;
}

.topic-suggestion-title {
	color: var(--text-primary);
	font-size: 24rpx;
	font-weight: 700;
}

.topic-suggestion-tip {
	color: var(--text-tertiary);
	font-size: 20rpx;
}

.topic-suggestion-list {
	margin-top: 16rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 12rpx;
}

.topic-suggestion-pill {
	min-height: 68rpx;
	padding: 0 20rpx;
	border-radius: 999rpx;
	background: rgba(66, 200, 255, 0.1);
	border: 1px solid rgba(66, 200, 255, 0.12);
	display: inline-flex;
	align-items: center;
	justify-content: center;
}

.topic-suggestion-pill__text {
	color: var(--accent-color);
	font-size: 24rpx;
	font-weight: 700;
}

.image-picker {
	margin-top: 20rpx;
	padding: 24rpx;
	border-radius: 28rpx;
	border: 1px dashed var(--border-color);
	background: var(--card-bg-strong);
	align-items: flex-start;
}

.image-picker__copy {
	flex: 1;
	min-width: 0;
}

.image-picker__arrow {
	color: var(--text-secondary);
	font-size: 36rpx;
}

.image-grid {
	margin-top: 20rpx;
	display: flex;
	flex-wrap: wrap;
	gap: 16rpx;
}

.image-grid__item {
	position: relative;
	width: calc((100% - 32rpx) / 3);
}

.image-grid__img {
	width: 100%;
	height: 180rpx;
	border-radius: 24rpx;
}

.image-grid__remove {
	position: absolute;
	top: 12rpx;
	right: 12rpx;
	width: 48rpx;
	height: 48rpx;
	border-radius: 999rpx;
	background: rgba(15, 23, 42, 0.7);
	color: #ffffff;
	font-size: 28rpx;
}

.compose-footer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
	z-index: 20;
	box-sizing: border-box;
}

.theme-dark .compose-footer {
	background: var(--ios-glass-bg);
	border-top: 1px solid var(--ios-glass-border);
	box-shadow:
		0 -16rpx 40rpx rgba(2, 8, 20, 0.34),
		var(--ios-glass-highlight);
	backdrop-filter: blur(34rpx) saturate(165%);
	-webkit-backdrop-filter: blur(34rpx) saturate(165%);
}

.theme-dark .back-pill,
.theme-dark .field-input,
.theme-dark .field-textarea,
.theme-dark .image-picker,
.theme-dark .topic-suggestion-box {
	background: var(--ios-glass-bg-soft);
	border-color: rgba(255, 255, 255, 0.08);
	box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
	backdrop-filter: blur(24rpx) saturate(150%);
	-webkit-backdrop-filter: blur(24rpx) saturate(150%);
}

.publish-btn {
	margin-top: 16rpx;
	width: 100%;
	height: 104rpx;
	border-radius: 999rpx;
	background: linear-gradient(135deg, var(--accent-color), var(--accent-color-strong));
	color: #ffffff;
	font-size: 30rpx;
	font-weight: 700;
	gap: 12rpx;
	box-shadow: var(--btn-shadow);
}

.publish-btn__arrow {
	font-size: 28rpx;
}

@media (min-width: 600px) {
	.compose-inner {
		max-width: 840rpx;
		margin: 0 auto;
	}

	.compose-footer {
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
	.compose-page {
		padding-bottom: calc(148rpx + env(safe-area-inset-bottom));
	}

	.compose-inner {
		padding: calc(12rpx + env(safe-area-inset-top)) 20rpx 188rpx;
		gap: 14rpx;
	}

	.compose-header {
		flex-direction: column;
		gap: 10rpx;
	}

	.back-pill {
		height: 72rpx;
		padding: 0 20rpx;
		font-size: 24rpx;
	}

	.back-pill__icon {
		font-size: 30rpx;
	}

	.compose-header__title,
	.prompt-title,
	.field-title {
		font-size: 26rpx;
	}

	.compose-header__desc,
	.prompt-desc,
	.field-tip,
	.field-helper,
	.compose-footer__tip {
		font-size: 20rpx;
		line-height: 1.48;
	}

	.prompt-card,
	.form-card {
		border-radius: 24rpx;
		padding: 20rpx;
	}

	.prompt-card {
		gap: 14rpx;
	}

	.prompt-icon,
	.image-picker__icon {
		width: 68rpx;
		height: 68rpx;
		font-size: 28rpx;
	}

	.field-head,
	.field-footer,
	.image-picker {
		align-items: flex-start;
		flex-wrap: wrap;
	}

	.field-input,
	.field-textarea {
		margin-top: 14rpx;
		font-size: 26rpx;
	}

	.field-input {
		height: 72rpx;
		padding: 0 18rpx;
	}

	.field-textarea {
		min-height: 196rpx;
		padding: 18rpx;
	}

	.field-helper,
	.field-footer {
		margin-top: 10rpx;
	}

	.topic-suggestion-box {
		margin-top: 14rpx;
		padding: 16rpx;
		border-radius: 18rpx;
	}

	.topic-suggestion-list {
		margin-top: 12rpx;
		gap: 10rpx;
	}

	.topic-suggestion-pill {
		min-height: 58rpx;
		padding: 0 16rpx;
	}

	.topic-suggestion-pill__text {
		font-size: 20rpx;
	}

	.image-picker {
		margin-top: 14rpx;
		padding: 18rpx;
		gap: 12rpx;
	}

	.image-grid {
		margin-top: 14rpx;
		gap: 12rpx;
	}

	.image-grid__item {
		width: calc((100% - 12rpx) / 2);
	}

	.image-grid__img {
		height: 188rpx;
		border-radius: 18rpx;
	}

	.compose-footer {
		padding: 12rpx 20rpx calc(12rpx + env(safe-area-inset-bottom));
	}

	.publish-btn {
		margin-top: 10rpx;
		height: 84rpx;
		font-size: 24rpx;
	}
}
</style>
