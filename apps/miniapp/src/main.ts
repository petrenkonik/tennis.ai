import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import { router } from './router';
import { initTma } from './tma';
import './styles.css';

initTma();

createApp(App).use(createPinia()).use(router).mount('#app');
