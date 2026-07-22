import { createApp } from 'vue';
import '@vue-flow/core/dist/style.css';
import './styles/main.css';
import App from './App.vue';
import router from './router';
import { installOverflowTooltip } from './utils/overflowTooltip';
import { permissionDirective } from './utils/permission';

installOverflowTooltip();
const app = createApp(App);
app.directive('permission', permissionDirective);
app.use(router).mount('#app');
