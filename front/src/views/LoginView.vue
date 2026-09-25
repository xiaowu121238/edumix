<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, LockKeyhole, Mail, CheckCircle2, Sparkles, Network, Share2 } from 'lucide-vue-next'
import BrandLogo from '../components/BrandLogo.vue'
import { login } from '../api/auth'
const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

async function submit() {
  loading.value = true
  errorMessage.value = ''
  try {
    const result = await login({ username: username.value, password: password.value })
    localStorage.setItem('edumix-token', result.data.token)
    localStorage.setItem('edumix-user', JSON.stringify(result.data.user))
    router.push(router.currentRoute.value.query.redirect || '/app/dashboard')
  } catch (error) {
    errorMessage.value = error.message || '登录失败，请检查账号和密码'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-form-side">
      <div class="login-top">
        <button class="text-button back" @click="router.push('/')"><ArrowLeft :size="17" />返回首页</button>
      </div>
      <div class="login-panel">
        <BrandLogo />
        <div class="login-title">
          <h1>欢迎回来</h1>
          <p>登录 EduMix，继续您的智能备课之旅</p>
        </div>
        <form @submit.prevent="submit">
          <label
            >账号
            <div class="input-wrap">
              <Mail :size="18" /><input v-model="username" required placeholder="请输入教师工号或邮箱" /></div
          ></label>
          <label
            >密码
            <div class="input-wrap">
              <LockKeyhole :size="18" /><input
                v-model="password"
                required
                type="password"
                placeholder="请输入登录密码"
              /></div
          ></label>
          <div class="form-options">
            <label class="remember"><input type="checkbox" checked />记住我</label><a href="#">忘记密码？</a>
          </div>
          <p v-if="route.query.passwordChanged === '1'" class="form-success">
            密码已修改，请使用新密码重新登录。
          </p>
          <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>
          <button class="button primary login-submit" :disabled="loading">
            {{ loading ? '正在登录...' : '登 录' }}
          </button>
        </form>
        <p class="register-entry">还没有教师账号？<RouterLink to="/register">注册教师账号</RouterLink></p>
      </div>
    </section>
    <section class="login-showcase">
      <div class="showcase-glow" />
      <div class="showcase-content">
        <span class="showcase-icon"><Sparkles :size="28" /></span>
        <h2>让每一次备课<br />都更高效、更有创造力</h2>
        <p>从知识点到完整授课方案，EduMix 帮助教师聚合优质资源，释放教学灵感。</p>
        <div class="feature-list">
          <div>
            <CheckCircle2 /><span><strong>智能资源匹配</strong><small>根据教学大纲精准推荐内容</small></span>
          </div>
          <div>
            <Network /><span><strong>知识图谱组织</strong><small>建立课程与资源的深层关联</small></span>
          </div>
          <div>
            <Share2 /><span><strong>校内协作共享</strong><small>沉淀和复用优秀教学实践</small></span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
