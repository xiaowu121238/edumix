<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { BadgeCheck, Bell, ChevronDown, Info, MessageCircle, Plus, X } from 'lucide-vue-next'
import { getNotifications, getUnreadCounts, publishSystemNotice, readNotification } from '../api/notification'
import ChatView from './ChatView.vue'
import '../styles/message-center.css'

const route = useRoute(),
  router = useRouter()
const roles = JSON.parse(localStorage.getItem('edumix-user') || '{}').roles || []
const isAdmin = roles.includes('ADMIN'),
  canChat = roles.includes('TEACHER')
const tabs = [
  { id: 'SYSTEM', name: '系统消息', icon: Bell },
  { id: 'AUDIT', name: '审核通知', icon: BadgeCheck },
  ...(canChat ? [{ id: 'PRIVATE', name: '私信', icon: MessageCircle }] : []),
]
const active = computed(() => (tabs.some((tab) => tab.id === route.query.tab) ? route.query.tab : 'SYSTEM'))
const items = ref([]),
  unread = ref({ system: 0, audit: 0, privateMessages: 0 })
const loading = ref(true),
  saving = ref(false),
  error = ref(''),
  notice = ref(''),
  publishOpen = ref(false)
const opened = ref(null)
const privateExpanded = ref(true)
const form = reactive({ title: '', content: '' })
let refreshTimer
const shown = computed(() => items.value.filter((item) => item.category === active.value))
const count = (category) =>
  category === 'SYSTEM'
    ? unread.value.system
    : category === 'AUDIT'
      ? unread.value.audit
      : unread.value.privateMessages
const formatTime = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '')

async function refreshCounts() {
  try {
    unread.value = (await getUnreadCounts()).data || unread.value
    window.dispatchEvent(new Event('edumix-messages-changed'))
  } catch (_) {
    /* notification list still works */
  }
}
async function load() {
  loading.value = true
  error.value = ''
  try {
    items.value = (await getNotifications()).data || []
    await refreshCounts()
  } catch (e) {
    error.value = e?.message || '通知加载失败'
  } finally {
    loading.value = false
  }
}
function selectTab(id) {
  if (id === 'PRIVATE' && active.value === 'PRIVATE') {
    privateExpanded.value = !privateExpanded.value
    return
  }
  if (id === 'PRIVATE') privateExpanded.value = true
  router.replace({ name: 'message', query: { tab: id } })
}
async function refreshSilently() {
  try {
    items.value = (await getNotifications()).data || items.value
    await refreshCounts()
  } catch (_) {
    /* keep the last successful view */
  }
}
async function openNotice(item) {
  opened.value = item
  error.value = ''
  if (item.read) return
  try {
    await readNotification(item.id)
    items.value = items.value.map((current) =>
      current.id === item.id ? { ...current, read: true } : current,
    )
    opened.value = { ...item, read: true }
    await refreshCounts()
  } catch (e) {
    error.value = e?.message || '标记已读失败，请重试'
  }
}
async function publish() {
  if (!form.title.trim() || !form.content.trim()) return
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    await publishSystemNotice({ title: form.title.trim(), content: form.content.trim() })
    publishOpen.value = false
    form.title = ''
    form.content = ''
    selectTab('SYSTEM')
    await load()
    notice.value = '系统消息已发送给当前启用的账号'
  } catch (e) {
    error.value = e?.message || '发布失败'
  } finally {
    saving.value = false
  }
}
onMounted(() => {
  load()
  refreshTimer = window.setInterval(refreshSilently, 30000)
})
onBeforeUnmount(() => window.clearInterval(refreshTimer))
watch(active, (tab) => {
  if (tab === 'PRIVATE') refreshCounts()
  else load()
})
</script>

<template>
  <div class="page message-page">
    <header class="page-title-row">
      <div>
        <h1>消息中心</h1>
        <p>系统公告、审核结果和教师私信集中在这里；打开消息后才会标记已读。</p>
      </div>
      <div class="message-title-actions">
        <button v-if="isAdmin" class="button primary" @click="publishOpen = true">
          <Plus :size="17" />发布系统消息
        </button>
      </div>
    </header>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="form-success">{{ notice }}</p>
    <section class="message-layout panel">
      <aside class="message-tabs">
        <template v-for="tab in tabs" :key="tab.id"
          ><button :class="{ active: active === tab.id }" @click="selectTab(tab.id)">
            <component :is="tab.icon" :size="18" /><span>{{ tab.name }}</span
            ><b v-if="count(tab.id)">{{ count(tab.id) }}</b
            ><ChevronDown
              v-if="tab.id === 'PRIVATE'"
              :size="14"
              class="private-chevron"
              :class="{ expanded: active === 'PRIVATE' && privateExpanded }"
            />
          </button>
          <div
            v-if="tab.id === 'PRIVATE' && active === 'PRIVATE'"
            v-show="privateExpanded"
            id="chat-conversations-sidebar"
            class="message-private-submenu"
        /></template>
      </aside>
      <ChatView v-if="active === 'PRIVATE'" embedded @read-change="refreshCounts" />
      <div v-else class="message-list">
        <div v-if="loading" class="resource-empty">正在加载通知...</div>
        <div v-else-if="!shown.length" class="resource-empty">
          暂无{{ active === 'SYSTEM' ? '系统消息' : '审核通知' }}
        </div>
        <button
          v-for="item in shown"
          :key="item.id"
          type="button"
          class="message-item message-openable"
          :class="{ unread: !item.read }"
          @click="openNotice(item)"
        >
          <span class="message-icon" :class="item.category === 'AUDIT' ? 'green' : 'blue'"
            ><BadgeCheck v-if="item.category === 'AUDIT'" :size="20" /><Info v-else :size="20" /></span
          ><span class="message-copy"
            ><strong>{{ item.title }}<i v-if="!item.read" /></strong><span>{{ item.content }}</span
            ><small>{{ formatTime(item.createTime) }}</small></span
          >
        </button>
      </div>
    </section>
    <div v-if="publishOpen" class="modal-mask" @click.self="publishOpen = false">
      <form class="modal-card system-publish-dialog" @submit.prevent="publish">
        <header>
          <h2>发布全站系统消息</h2>
          <button type="button" @click="publishOpen = false"><X /></button>
        </header>
        <div class="modal-fields">
          <p class="wide system-publish-hint">发送给当前所有启用的账号；新注册账号不会自动收到历史公告。</p>
          <label class="wide"
            >标题<input v-model="form.title" required maxlength="255" placeholder="请输入公告标题" /></label
          ><label class="wide"
            >内容<textarea v-model="form.content" required maxlength="2000" placeholder="请输入公告内容" />
          </label>
        </div>
        <p v-if="error" class="form-error">{{ error }}</p>
        <footer>
          <button type="button" class="button secondary" @click="publishOpen = false">取消</button
          ><button class="button primary" :disabled="saving || !form.title.trim() || !form.content.trim()">
            {{ saving ? '发布中...' : '确认发布' }}
          </button>
        </footer>
      </form>
    </div>
    <div v-if="opened" class="modal-mask" @click.self="opened = null">
      <article class="modal-card message-detail-dialog">
        <header>
          <div>
            <small
              >{{ opened.category === 'SYSTEM' ? '系统消息' : '审核通知' }} ·
              {{ formatTime(opened.createTime) }}</small
            >
            <h2>{{ opened.title }}</h2>
          </div>
          <button type="button" aria-label="关闭消息" @click="opened = null"><X /></button>
        </header>
        <div class="message-detail-content">{{ opened.content }}</div>
        <footer><button class="button primary" @click="opened = null">关闭</button></footer>
      </article>
    </div>
  </div>
</template>
