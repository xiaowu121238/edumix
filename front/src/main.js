import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './styles/main.css'
import './styles/chat.css'
import './styles/accounts.css'
import './styles/reviews.css'
import './styles/ai-favorites.css'

createApp(App).use(router).mount('#app')
