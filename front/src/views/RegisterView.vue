<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, GraduationCap } from 'lucide-vue-next'
import { register } from '../api/auth'

const router = useRouter()
const form = ref({ username: '', realName: '', email: '', college: '', password: '', confirm: '' })
const busy = ref(false),
  error = ref(''),
  success = ref(false)
async function submit() {
  error.value = ''
  if (form.value.password !== form.value.confirm) {
    error.value = '两次输入的密码不一致'
    return
  }
  busy.value = true
  try {
    await register({
      username: form.value.username,
      realName: form.value.realName,
      email: form.value.email || null,
      college: form.value.college || null,
      password: form.value.password,
    })
    success.value = true
  } catch (e) {
    error.value = e?.message || '注册失败'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <main class="register-card panel">
      <button class="reader-back" @click="router.push('/login')"><ArrowLeft />返回登录</button>
      <div class="register-icon"><GraduationCap /></div>
      <h1>注册教师账号</h1>
      <p>注册成功即可登录，使用教师端功能。负责人和管理员账号由管理员创建。</p>
      <div v-if="success" class="register-success">
        <h2>注册成功</h2>
        <p>请使用新账号和密码登录。</p>
        <button class="button primary" @click="router.push('/login')">前往登录</button>
      </div>
      <form v-else @submit.prevent="submit">
        <label
          >用户名<input
            v-model.trim="form.username"
            required
            minlength="3"
            maxlength="50"
            pattern="[A-Za-z0-9_]+"
            placeholder="3–50 位字母、数字或下划线"
            autocomplete="username" /></label
        ><label
          >真实姓名<input
            v-model.trim="form.realName"
            required
            maxlength="50"
            placeholder="请输入真实姓名" /></label
        ><label>学院<input v-model.trim="form.college" maxlength="100" placeholder="选填" /></label
        ><label>邮箱<input v-model.trim="form.email" type="email" maxlength="100" placeholder="选填" /></label
        ><label
          >密码<input
            v-model="form.password"
            type="password"
            required
            minlength="8"
            maxlength="72"
            autocomplete="new-password"
            placeholder="至少 8 个字符" /></label
        ><label
          >确认密码<input
            v-model="form.confirm"
            type="password"
            required
            autocomplete="new-password"
            placeholder="再次输入密码"
        /></label>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="button primary" :disabled="busy">{{ busy ? '注册中...' : '立即注册教师账号' }}</button>
      </form>
    </main>
  </div>
</template>
