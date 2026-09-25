<script setup>
import { computed, onMounted, ref } from 'vue'
import { KeyRound, Plus, RefreshCw, ShieldCheck } from 'lucide-vue-next'
import {
  createAccount,
  listAccounts,
  resetAccountPassword,
  setAccountRoles,
  setAccountStatus,
} from '../api/accounts'

const accounts = ref([]),
  busy = ref(false),
  error = ref(''),
  notice = ref(''),
  showCreate = ref(false)
const me = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const form = ref({
  username: '',
  realName: '',
  password: '',
  email: '',
  college: '',
  title: '',
  accountType: 'RESOURCE_MANAGER',
})
const roleName = (roles) =>
  roles?.includes('ADMIN') ? '管理员' : roles?.includes('RESOURCE_MANAGER') ? '资源负责人' : '教师'
const roleType = (roles) =>
  roles?.includes('ADMIN') ? 'ADMIN' : roles?.includes('RESOURCE_MANAGER') ? 'RESOURCE_MANAGER' : 'TEACHER'
const total = computed(() => accounts.value.length)
async function load() {
  error.value = ''
  try {
    accounts.value = (await listAccounts()).data || []
  } catch (e) {
    error.value = e?.message || '账号列表加载失败'
  }
}
async function create() {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await createAccount(form.value)
    showCreate.value = false
    form.value = {
      username: '',
      realName: '',
      password: '',
      email: '',
      college: '',
      title: '',
      accountType: 'RESOURCE_MANAGER',
    }
    notice.value = '账号已创建，请通过安全渠道把初始密码交给使用者。'
    await load()
  } catch (e) {
    error.value = e?.message || '创建失败'
  } finally {
    busy.value = false
  }
}
async function changeRole(item, event) {
  const type = event.target.value
  event.target.value = roleType(item.roles)
  if (type === roleType(item.roles)) return
  if (
    !confirm(
      `确定将 ${item.username} 改为${type === 'ADMIN' ? '管理员' : type === 'RESOURCE_MANAGER' ? '资源负责人' : '教师'}吗？`,
    )
  )
    return
  busy.value = true
  error.value = ''
  try {
    const roles =
      type === 'ADMIN'
        ? ['ADMIN']
        : type === 'RESOURCE_MANAGER'
          ? ['TEACHER', 'RESOURCE_MANAGER']
          : ['TEACHER']
    await setAccountRoles(item.id, roles)
    await load()
  } catch (e) {
    error.value = e?.message || '角色修改失败'
  } finally {
    busy.value = false
  }
}
async function toggle(item) {
  if (!confirm(`确定${item.status === 1 ? '停用' : '启用'}账号 ${item.username} 吗？`)) return
  busy.value = true
  error.value = ''
  try {
    await setAccountStatus(item.id, item.status === 1 ? 0 : 1)
    await load()
  } catch (e) {
    error.value = e?.message || '状态修改失败'
  } finally {
    busy.value = false
  }
}
async function resetPassword(item) {
  const password = prompt(`输入 ${item.username} 的新密码（至少 8 个字符）：`)
  if (password === null) return
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    await resetAccountPassword(item.id, password)
    notice.value = `${item.username} 的密码已重置，请安全告知本人。`
  } catch (e) {
    error.value = e?.message || '密码重置失败'
  } finally {
    busy.value = false
  }
}
onMounted(load)
</script>

<template>
  <div class="page account-page">
    <header class="page-title-row">
      <div>
        <h1>账号管理</h1>
        <p>教师可以自行注册；资源负责人和管理员由管理员创建或授权。</p>
      </div>
      <button class="button primary" @click="showCreate = !showCreate"><Plus />创建账号</button>
    </header>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="account-notice">{{ notice }}</p>
    <section v-if="showCreate" class="panel account-create">
      <h2>创建平台账号</h2>
      <form @submit.prevent="create">
        <label
          >账号<input
            v-model.trim="form.username"
            required
            minlength="3"
            maxlength="50"
            pattern="[A-Za-z0-9_]+" /></label
        ><label>姓名<input v-model.trim="form.realName" required maxlength="50" /></label
        ><label
          >初始密码<input
            v-model="form.password"
            type="password"
            required
            minlength="8"
            maxlength="72" /></label
        ><label
          >账号类型<select v-model="form.accountType">
            <option value="RESOURCE_MANAGER">资源负责人（兼具教师权限）</option>
            <option value="ADMIN">管理员</option>
            <option value="TEACHER">教师</option>
          </select></label
        ><label>学院<input v-model.trim="form.college" maxlength="100" /></label
        ><label>邮箱<input v-model.trim="form.email" type="email" maxlength="100" /></label
        ><label>职称<input v-model.trim="form.title" maxlength="50" /></label>
        <div class="account-create-actions">
          <button type="button" class="button secondary" @click="showCreate = false">取消</button
          ><button class="button primary" :disabled="busy">{{ busy ? '创建中...' : '确认创建' }}</button>
        </div>
      </form>
    </section>
    <section class="panel account-list">
      <header>
        <h2>
          平台账号 <small>共 {{ total }} 个</small>
        </h2>
        <button @click="load"><RefreshCw />刷新</button>
      </header>
      <div class="account-table-wrap">
        <table>
          <thead>
            <tr>
              <th>账号</th>
              <th>姓名</th>
              <th>学院</th>
              <th>角色</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in accounts" :key="item.id">
              <td>{{ item.username }}</td>
              <td>{{ item.realName }}</td>
              <td>{{ item.college || '—' }}</td>
              <td>
                <select :value="roleType(item.roles)" :disabled="busy" @change="changeRole(item, $event)">
                  <option value="TEACHER">教师</option>
                  <option value="RESOURCE_MANAGER">负责人＋教师</option>
                  <option value="ADMIN">管理员</option>
                </select>
              </td>
              <td>
                <span :class="['account-status', item.status === 1 ? 'active' : 'disabled']">{{
                  item.status === 1 ? '已启用' : '已停用'
                }}</span>
              </td>
              <td class="account-actions">
                <button :disabled="busy" @click="resetPassword(item)"><KeyRound />重置密码</button
                ><button :disabled="busy || item.id === me.id" @click="toggle(item)">
                  <ShieldCheck />{{ item.status === 1 ? '停用' : '启用' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-if="!accounts.length" class="resource-empty">暂无账号</p>
    </section>
  </div>
</template>
