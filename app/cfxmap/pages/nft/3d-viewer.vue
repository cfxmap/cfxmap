<template>
	<view class="container">
		<global-loading />
		<view class="nav-bar">
			<view class="back-btn" @click="goBack">
				<nav-back-btn on-dark-background />
			</view>
			<text class="nav-title">3D Preview</text>
		</view>
		<view class="web-view-wrapper">
			<!-- #ifdef H5 -->
			<iframe v-if="url" :src="`/hybrid/html/3d.html?url=${encodeURIComponent(url)}`" frameborder="0" scrolling="no" style="width: 100%; height: 100%; pointer-events: auto; touch-action: none;"></iframe>
			<!-- #endif -->
			<!-- #ifndef H5 -->
			<web-view v-if="url" :src="`/hybrid/html/3d.html?url=${encodeURIComponent(url)}`" :style="{ width: '100%', height: '100%' }"></web-view>
			<!-- #endif -->
		</view>
	</view>
</template>

<script>
	import NavBackBtn from '@/components/nav-back-btn.vue'

	export default {
		components: { NavBackBtn },
		data() {
			return {
				url: ''
			}
		},
		onLoad(options) {
			if (options.url) {
				this.url = decodeURIComponent(options.url);
			}
		},
		methods: {
			goBack() {
				uni.navigateBack();
			}
		}
	}
</script>

<style scoped>
	.container {
		width: 100vw;
		height: 100vh;
		background-color: #000;
		display: flex;
		flex-direction: column;
	}

	.nav-bar {
		padding: 80rpx 32rpx 20rpx;
		display: flex;
		align-items: center;
		background-color: rgba(0, 0, 0, 0.8);
		position: relative;
		z-index: 1000;
	}

	.back-btn {
		padding: 10rpx;
		margin-right: 20rpx;
	}

	.nav-title {
		font-size: 32rpx;
		font-weight: 600;
		color: #ffffff;
	}

	.web-view-wrapper {
		flex: 1;
		width: 100%;
		position: relative;
	}
</style>