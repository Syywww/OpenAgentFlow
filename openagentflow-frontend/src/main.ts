import { createApp } from 'vue';
import '@vue-flow/core/dist/style.css';
import './styles/main.css';
import App from './App.vue';
import router from './router';
import { installOverflowTooltip } from './utils/overflowTooltip';

installOverflowTooltip();
createApp(App).use(router).mount('#app');
