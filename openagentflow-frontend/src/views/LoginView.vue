<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ArrowRight, Bot, Braces, GitBranch, Library, RefreshCw, ShieldCheck } from 'lucide-vue-next';
import { fetchCaptcha, login } from '../api/auth';

const router = useRouter();

const username = ref('admin');
const password = ref('123456');
const captcha = ref('');
const captchaKey = ref('');
const captchaImage = ref('');
const rememberMe = ref(true);
const loading = ref(false);
const captchaLoading = ref(false);
const errorMessage = ref('');

async function loadCaptcha() {
  captchaLoading.value = true;
  try {
    const result = await fetchCaptcha();
    captchaKey.value = result.captchaKey;
    captchaImage.value = result.imageBase64;
    captcha.value = '';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '验证码加载失败，请稍后重试';
  } finally {
    captchaLoading.value = false;
  }
}

async function handleLogin() {
  loading.value = true;
  errorMessage.value = '';
  try {
    await login({
      username: username.value,
      password: password.value,
      captchaKey: captchaKey.value,
      captcha: captcha.value,
      rememberMe: rememberMe.value,
    });
    await router.push('/dashboard');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请稍后重试';
    await loadCaptcha();
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void loadCaptcha();
});
</script>

<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="brand-mark large">
        <div class="brand-logo"><Bot :size="22" /></div>
        <div>
          <b>OpenAgentFlow-Java</b>
          <span>AI Agent Workflow Platform</span>
        </div>
      </div>

      <div class="login-copy">
        <h1>登录控制台</h1>
        <p>统一管理 Agent、RAG 知识库、MCP 工具、工作流编排与执行 Trace。</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <label>用户名<input v-model="username" autocomplete="username" /></label>
        <label>密码<input v-model="password" type="password" autocomplete="current-password" /></label>
        <label>
          验证码
          <div class="captcha-row">
            <input v-model="captcha" maxlength="4" placeholder="请输入验证码" />
            <button class="captcha-image" type="button" :disabled="captchaLoading" @click="loadCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <RefreshCw v-else :size="18" />
            </button>
          </div>
        </label>
        <div class="login-options">
          <label><input v-model="rememberMe" type="checkbox" /> 记住我</label>
          <a>忘记密码</a>
        </div>
        <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
        <button class="primary-button full" type="submit" :disabled="loading || captchaLoading">
          {{ loading ? '登录中' : '登录' }} <ArrowRight :size="17" />
        </button>
      </form>

      <div class="sso-row">
        <button class="secondary-button" type="button">企业微信</button>
        <button class="secondary-button" type="button">钉钉</button>
        <button class="secondary-button" type="button">飞书</button>
      </div>
    </section>

    <section class="login-visual">
      <div class="system-map">
        <div class="map-node center"><Bot :size="30" /> Agent Runtime</div>
        <div class="map-node top"><Library :size="20" /> RAG</div>
        <div class="map-node right"><Braces :size="20" /> Tool Calling</div>
        <div class="map-node bottom"><GitBranch :size="20" /> Workflow</div>
        <div class="map-node left"><ShieldCheck :size="20" /> Trace & Eval</div>
        <svg viewBox="0 0 520 360" aria-hidden="true">
          <path d="M260 178 L260 62 M318 182 L435 182 M260 212 L260 306 M202 182 L84 182" />
        </svg>
      </div>
      <div class="visual-caption">
        <h2>可配置、可追踪、可评测、可扩展</h2>
        <p>面向 Java + Vue 技术栈的企业级 AI 应用开发平台。</p>
      </div>
    </section>
  </main>
</template>
