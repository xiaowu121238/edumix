<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Coins } from 'lucide-vue-next'
import { listAccounts } from '../api/accounts'
import { grantPoints } from '../api/points'
import '../styles/points.css'
import '../styles/admin-points.css'

const accounts = ref([]),
  loading = ref(true),
  busy = ref(false),
  error = ref(''),
  notice = ref('')
const me = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const form = reactive({ recipientId: '', amount: 20, reason: '', password: '' })
const recipients = computed(() =>
  accounts.value.filter((account) => account.id !== me.id && account.status === 1),
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    accounts.value = (await listAccounts()).data || []
  } catch (e) {
    error.value = e?.message || '账号加载失败'
  } finally {
    loading.value = false
  }
}
async function submit() {
  if (!confirm(`确定向所选账号发放 ${form.amount} 积分吗？`)) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await grantPoints({ ...form, recipientId: Number(form.recipientId), amount: Number(form.amount) })
    notice.value = '积分已发放，流水已记录'
    form.password = ''
    form.reason = ''
  } catch (e) {
    error.value = e?.message || '发放失败'
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>

<template>
  <div class="page points-page">
    <header class="page-title-row">
      <div>
        <h1>积分发放</h1>
        <p>仅管理员可操作。每笔发放都需要验证当前管理员的登录密码，并留下原因和流水。</p>
      </div>
    </header>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="form-success">{{ notice }}</p>
    <div v-if="loading" class="panel resource-empty">正在加载账号...</div>
    <form v-else class="panel admin-point-form" @submit.prevent="submit">
      <h2><Coins :size="20" />给账号发放积分</h2>
      <label
        >接收账号<select v-model="form.recipientId" required>
          <option value="" disabled>请选择启用的账号</option>
          <option v-for="account in recipients" :key="account.id" :value="account.id">
            {{ account.realName }}（{{ account.username }}）
          </option>
        </select></label
      ><label
        >发放数量<input
          v-model.number="form.amount"
          type="number"
          min="1"
          max="1000"
          step="1"
          required /></label
      ><label
        >发放原因<textarea
          v-model.trim="form.reason"
          required
          maxlength="500"
          placeholder="请写明发放原因"
        /></label
      ><label
        >当前管理员的登录密码<input
          v-model="form.password"
          type="password"
          autocomplete="current-password"
          required
          placeholder="用于再次确认操作"
      /></label>
      <p>单次限 1～1000 分，不允许给自己发放。初始密码如仍为 123456，请先修改账户密码。</p>
      <button class="button primary" :disabled="busy || !form.recipientId">
        {{ busy ? '发放中...' : '确认发放积分' }}
      </button>
    </form>
  </div>
</template>
