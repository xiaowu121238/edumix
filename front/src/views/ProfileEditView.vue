<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Camera, KeyRound, Save, UserRound, X } from 'lucide-vue-next'
import { changePassword, getCurrentUser, logout, updateProfile, uploadAvatar } from '../api/auth'
import { notifyError } from '../services/notice'
import '../styles/profile-home.css'

const router = useRouter()
const form = reactive({ realName: '', email: '', phone: '', avatarUrl: '', college: '', title: '' })
const username = ref('')
const loading = ref(true),
  saving = ref(false),
  uploading = ref(false)
const message = ref(''),
  error = ref(''),
  passwordOpen = ref(false),
  passwordSaving = ref(false)
const password = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
function applyProfile(profile) {
  username.value = profile.username
  Object.keys(form).forEach((key) => {
    form[key] = profile[key] || ''
  })
  localStorage.setItem('edumix-user', JSON.stringify(profile))
  window.dispatchEvent(new Event('edumix-profile-changed'))
}
onMounted(async () => {
  try {
    applyProfile((await getCurrentUser()).data)
  } catch (e) {
    error.value = e.message || '资料加载失败'
  } finally {
    loading.value = false
  }
})
async function save() {
  saving.value = true
  message.value = ''
  error.value = ''
  try {
    applyProfile((await updateProfile(form)).data)
    message.value = '资料已保存'
  } catch (e) {
    error.value = e.message || '保存失败'
  } finally {
    saving.value = false
  }
}
async function chooseAvatar(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 2 * 1024 * 1024) {
    error.value = '请上传不超过 2MB 的 JPG、PNG 或 WebP 图片'
    notifyError(error.value, { title: '头像文件不符合要求' })
    return
  }
  uploading.value = true
  message.value = ''
  error.value = ''
  try {
    applyProfile((await uploadAvatar(file)).data)
    message.value = '头像已更新'
  } catch (e) {
    error.value = e.message || '头像上传失败'
  } finally {
    uploading.value = false
  }
}
async function submitPassword() {
  error.value = ''
  message.value = ''
  if (password.newPassword !== password.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    notifyError(error.value, { title: '请检查密码' })
    return
  }
  passwordSaving.value = true
  try {
    await changePassword({ oldPassword: password.oldPassword, newPassword: password.newPassword })
    try {
      await logout()
    } catch (_) {
      /* still clear local credentials */
    }
    localStorage.removeItem('edumix-token')
    localStorage.removeItem('edumix-user')
    router.replace({ name: 'login', query: { passwordChanged: '1' } })
  } catch (e) {
    error.value = e.message || '修改密码失败'
  } finally {
    passwordSaving.value = false
  }
}

function openPasswordDialog() {
  passwordOpen.value = true
  error.value = ''
}
</script>

<template>
  <div class="page personal-edit-page">
    <button class="personal-back" @click="router.push({ name: 'profile' })">
      <ArrowLeft :size="18" />返回个人主页
    </button>
    <div class="personal-edit-heading">
      <div>
        <span class="personal-eyebrow">PROFILE SETTINGS</span>
        <h1>编辑个人资料</h1>
        <p>更新您的头像、姓名和联系方式。</p>
      </div>
      <button class="button secondary" @click="openPasswordDialog"><KeyRound :size="17" />修改密码</button>
    </div>
    <div v-if="loading" class="panel resource-empty">正在加载资料...</div>
    <template v-else>
      <section class="panel personal-avatar-card">
        <span class="personal-avatar edit"
          ><img v-if="form.avatarUrl" :src="form.avatarUrl" alt="个人头像" /><UserRound v-else :size="40"
        /></span>
        <div>
          <h2>个人头像</h2>
          <p>支持 JPG、PNG、WebP，文件不超过 2MB。上传后立即生效。</p>
        </div>
        <label class="button secondary personal-upload"
          ><Camera :size="17" />{{ uploading ? '上传中...' : '上传头像'
          }}<input
            type="file"
            accept="image/jpeg,image/png,image/webp"
            :disabled="uploading"
            @change="chooseAvatar"
        /></label>
      </section>
      <form class="panel personal-edit-form" @submit.prevent="save">
        <div class="personal-form-header">
          <h2>基本信息</h2>
          <p>用户名 @{{ username }} 不可在此修改。</p>
        </div>
        <div class="field-grid">
          <label>姓名<input v-model="form.realName" required maxlength="50" /></label
          ><label>邮箱<input v-model="form.email" type="email" maxlength="100" /></label
          ><label>手机号<input v-model="form.phone" maxlength="20" /></label
          ><label>学院<input v-model="form.college" maxlength="100" /></label
          ><label>职称<input v-model="form.title" maxlength="50" /></label>
        </div>
        <p v-if="message" class="form-success">{{ message }}</p>
        <p v-if="error && !passwordOpen" class="form-error">{{ error }}</p>
        <div class="personal-form-actions">
          <button class="button primary" :disabled="saving">
            <Save :size="17" />{{ saving ? '保存中...' : '保存资料' }}
          </button>
        </div>
      </form>
    </template>
    <div v-if="passwordOpen" class="modal-mask" @click.self="passwordOpen = false">
      <form class="modal-card personal-password-modal" @submit.prevent="submitPassword">
        <header>
          <h2>修改密码</h2>
          <button type="button" aria-label="关闭" @click="passwordOpen = false"><X :size="20" /></button>
        </header>
        <div class="personal-password-fields">
          <label
            >原密码<input
              v-model="password.oldPassword"
              type="password"
              autocomplete="current-password"
              required /></label
          ><label
            >新密码<input
              v-model="password.newPassword"
              type="password"
              autocomplete="new-password"
              minlength="8"
              maxlength="72"
              required /></label
          ><label
            >确认新密码<input
              v-model="password.confirmPassword"
              type="password"
              autocomplete="new-password"
              minlength="8"
              maxlength="72"
              required
          /></label>
          <p>修改成功后需要重新登录。</p>
          <p v-if="error" class="form-error">{{ error }}</p>
        </div>
        <footer>
          <button type="button" class="button secondary" @click="passwordOpen = false">取消</button
          ><button class="button primary" :disabled="passwordSaving">
            {{ passwordSaving ? '修改中...' : '确认修改' }}
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>
