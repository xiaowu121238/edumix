<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Menu, PanelLeft, Bell, ChevronDown, User, LogOut } from 'lucide-vue-next'
import { logout as logoutRequest } from '../api/auth'
import { getUnreadCounts } from '../api/notification'
import HeaderCheckIn from './HeaderCheckIn.vue'
import '../styles/header-context.css'

defineProps({ sidebarOpen: Boolean })
const emit = defineEmits(['toggle'])
const profileOpen = ref(false)
const router = useRouter()
const route = useRoute()
const currentUser = ref(JSON.parse(localStorage.getItem('edumix-user') || '{}'))
const unreadTotal = ref(0)
let unreadTimer
async function refreshUnread() {
  try {
    const counts = (await getUnreadCounts({ skipErrorNotice: true })).data || {}
    unreadTotal.value = (counts.system || 0) + (counts.audit || 0) + (counts.privateMessages || 0)
  } catch (_) {
    unreadTotal.value = 0
  }
}
function refreshProfile() {
  currentUser.value = JSON.parse(localStorage.getItem('edumix-user') || '{}')
}
onMounted(() => {
  refreshUnread()
  unreadTimer = window.setInterval(refreshUnread, 30000)
  window.addEventListener('edumix-messages-changed', refreshUnread)
  window.addEventListener('edumix-profile-changed', refreshProfile)
})
onBeforeUnmount(() => {
  window.clearInterval(unreadTimer)
  window.removeEventListener('edumix-messages-changed', refreshUnread)
  window.removeEventListener('edumix-profile-changed', refreshProfile)
})
watch(() => route.fullPath, refreshUnread)
const pageTitle = computed(() => route.meta.title || '功能页面')
const isDashboard = computed(() => route.name === 'dashboard')
const descriptions = {
  dashboard: '查看个人教学内容与待办',
  course: '管理课程、章节与知识点',
  resource: '查找、上传与管理教学资源',
  case: '浏览审核通过的教学案例',
  'my-cases': '管理草稿、审核进度与已发布案例',
  'case-new': '创作教学案例并保存草稿',
  'case-edit': '继续编辑个人案例草稿',
  'case-detail': '阅读教学案例与关联资源',
  ai: '从个人收藏中推荐与整合素材',
  favorites: '查看收藏的资源和案例',
  points: '查看积分余额、每日签到与收支记录',
  reviews: '审核待处理的资源与案例',
  message: '查看系统公告、审核通知与教师私信',
  profile: '查看个人创作与账户概况',
  'profile-edit': '维护个人信息与修改密码',
  'admin-accounts': '管理平台账号与角色',
  'admin-points': '为平台账号发放积分并记录原因',
  'teacher-profile': '查看教师公开资料',
}
const pageDescription = computed(() => descriptions[route.name] || '')

function openProfile() {
  profileOpen.value = false
  router.push('/app/profile')
}

async function logout() {
  try {
    await logoutRequest()
  } catch (_) {
    /* local logout must still complete */
  } finally {
    localStorage.removeItem('edumix-token')
    localStorage.removeItem('edumix-user')
    router.push('/login')
  }
}
</script>

<template>
  <header class="app-header">
    <div class="header-left">
      <button class="icon-button" aria-label="切换侧边栏" @click="emit('toggle')">
        <PanelLeft v-if="sidebarOpen" :size="24" /><Menu v-else :size="24" />
      </button>
      <div class="header-context">
        <div class="header-breadcrumb">
          <RouterLink v-if="!isDashboard" to="/app/dashboard">教师工作台</RouterLink>
          <span v-if="!isDashboard" class="header-breadcrumb-separator">/</span>
          <strong>{{ pageTitle }}</strong>
        </div>
        <small v-if="pageDescription">{{ pageDescription }}</small>
      </div>
    </div>
    <div class="header-actions">
      <HeaderCheckIn />
      <RouterLink to="/app/message" class="icon-button notification" aria-label="消息中心">
        <Bell :size="21" /><span v-if="unreadTotal" class="notification-count">{{
          unreadTotal > 99 ? '99+' : unreadTotal
        }}</span>
      </RouterLink>
      <span class="header-divider" />
      <div class="profile-wrap">
        <button class="profile-trigger" @click="profileOpen = !profileOpen">
          <span class="avatar"
            ><img
              v-if="currentUser.avatarUrl"
              :src="currentUser.avatarUrl"
              alt=""
              class="header-avatar-image"
            /><template v-else>{{ currentUser.realName?.slice(0, 1) || '用' }}</template></span
          >
          <span class="profile-copy"
            ><strong>{{ currentUser.realName || currentUser.username }}</strong
            ><small>{{ currentUser.college || 'EduMix' }}</small></span
          >
          <ChevronDown :size="15" />
        </button>
        <div v-if="profileOpen" class="profile-menu">
          <button @click="openProfile"><User :size="17" />个人主页</button>
          <hr />
          <button class="danger" @click="logout"><LogOut :size="17" />退出登录</button>
        </div>
      </div>
    </div>
  </header>
</template>
