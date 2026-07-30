// #ifdef H5
import './polyfill-process-h5.js'
// #endif

import App from './App'
import store from './store'
import { getStaticUrl } from './utils/static-resource.js'

const themeMixin = {
	computed: {
		themeClass() {
			return store.getters.themeClass
		},
		isDarkMode() {
			return store.getters.isDarkMode
		}
	},
	methods: {
		$staticUrl(path) {
			return getStaticUrl(path)
		}
	}
}

// #ifndef VUE3
import Vue from 'vue'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
Vue.mixin(themeMixin)
App.mpType = 'app'
const app = new Vue({
  store,
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
  const app = createSSRApp(App)
  app.use(store)
  app.mixin(themeMixin)
  return {
    app
  }
}
// #endif
