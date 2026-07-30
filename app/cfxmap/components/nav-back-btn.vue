<template>
	<image
		class="nav-back-btn-img"
		:class="{ 'nav-back-btn-img--light': onDarkBackground }"
		:src="$staticUrl('/static/icons/nav-back-arrow.png')"
		mode="aspectFit"
		aria-role="button"
	/>
</template>

<script>
	export default {
		name: 'NavBackBtn',
		props: {
			/** 深色底导航条等场景：将黑色箭头反色为浅色 */
			onDarkBackground: {
				type: Boolean,
				default: false
			}
		}
	}
</script>

<style scoped lang="scss">
	.nav-back-btn-img {
		width: 48rpx;
		height: 48rpx;
		display: block;
	}

	.theme-dark .nav-back-btn-img {
		filter: brightness(0) invert(1);
	}

	.nav-back-btn-img--light {
		filter: brightness(0) invert(1);
	}
</style>
