<script setup>
import { Client } from '@stomp/stompjs'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MessageCircle, Search, Send, UserRound } from 'lucide-vue-next'
import {
  getChatMessages,
  getConversations,
  markChatRead,
  searchTeachers,
  sendChatMessage,
  startConversation,
} from '../api/chat'

defineProps({ embedded: { type: Boolean, default: false } })
const emit = defineEmits(['read-change'])
const route = useRoute(),
  router = useRouter()
const me = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const conversations = ref([]),
  teachers = ref([]),
  selected = ref(null),
  messages = ref([]),
  beforeId = ref(null)
const keyword = ref(''),
  draft = ref(''),
  error = ref(''),
  loading = ref(true),
  sending = ref(false)
const loadingHistory = ref(false),
  connected = ref(false),
  stream = ref(null)
let stomp = null
const visibleConversations = computed(() =>
  conversations.value.filter(
    (c) => !keyword.value || c.other.realName.toLowerCase().includes(keyword.value.toLowerCase()),
  ),
)
const newTeachers = computed(() =>
  teachers.value.filter((t) => !conversations.value.some((c) => c.other.id === t.id)),
)
const formatTime = (value) =>
  value
    ? new Date(value).toLocaleString('zh-CN', {
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      })
    : ''

async function refreshConversations() {
  conversations.value = (await getConversations()).data || []
  if (selected.value)
    selected.value = conversations.value.find((c) => c.id === selected.value.id) || selected.value
}
async function findTeachers() {
  const query = keyword.value.trim()
  if (!query) {
    teachers.value = []
    return
  }
  try {
    const result = (await searchTeachers(query)).data || []
    if (keyword.value.trim() === query) teachers.value = result.filter((t) => t.id !== me.id)
  } catch (e) {
    error.value = e?.message || '教师搜索失败'
  }
}
async function openConversation(item) {
  selected.value = item
  messages.value = []
  beforeId.value = null
  draft.value = ''
  error.value = ''
  try {
    const page = (await getChatMessages(item.id, null, 30)).data
    messages.value = page.items || []
    beforeId.value = page.nextBeforeId
    await markChatRead(item.id)
    item.unreadCount = 0
    emit('read-change')
    scrollBottom()
  } catch (e) {
    error.value = e?.message || '聊天记录加载失败'
  }
}
async function start(teacher) {
  try {
    const result = (await startConversation(teacher.id)).data
    await refreshConversations()
    await openConversation(conversations.value.find((c) => c.id === result.id) || result)
    teachers.value = []
    keyword.value = ''
    router.replace({ name: 'message', query: { tab: 'PRIVATE' } })
  } catch (e) {
    error.value = e?.message || '创建会话失败'
  }
}
async function loadOlder() {
  if (!selected.value || !beforeId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const page = (await getChatMessages(selected.value.id, beforeId.value, 30)).data
    messages.value = [...(page.items || []), ...messages.value]
    beforeId.value = page.nextBeforeId
  } catch (e) {
    error.value = e?.message || '加载历史消息失败'
  } finally {
    loadingHistory.value = false
  }
}
async function send() {
  const content = draft.value.trim()
  if (!content || !selected.value || sending.value) return
  sending.value = true
  error.value = ''
  try {
    const saved = (await sendChatMessage(selected.value.id, content)).data
    append(saved)
    draft.value = ''
    await refreshConversations()
    scrollBottom()
  } catch (e) {
    error.value = e?.message || '发送失败，请重试'
  } finally {
    sending.value = false
  }
}
function append(item) {
  if (selected.value?.id !== item.conversationId || messages.value.some((m) => m.id === item.id)) return
  messages.value.push(item)
  scrollBottom()
  if (item.senderId !== me.id)
    markChatRead(item.conversationId)
      .then(() => {
        if (selected.value) selected.value.unreadCount = 0
        emit('read-change')
      })
      .catch(() => {})
}
function scrollBottom() {
  nextTick(() => {
    if (stream.value) stream.value.scrollTop = stream.value.scrollHeight
  })
}
function connect() {
  // HTTP 负责历史记录，WebSocket 只承载增量消息；断线后由 STOMP 自动重连。
  const token = localStorage.getItem('edumix-token')
  if (!token) return
  const scheme = location.protocol === 'https:' ? 'wss' : 'ws'
  stomp = new Client({
    brokerURL: `${scheme}://${location.host}/ws/chat`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5000,
    onConnect: () => {
      connected.value = true
      stomp.subscribe('/user/queue/chat', (frame) => {
        try {
          append(JSON.parse(frame.body))
          refreshConversations().catch(() => {})
          emit('read-change')
        } catch (_) {
          /* ignore malformed frame */
        }
      })
    },
    onDisconnect: () => {
      connected.value = false
    },
    onWebSocketClose: () => {
      connected.value = false
    },
  })
  stomp.activate()
}
onMounted(async () => {
  try {
    await refreshConversations()
    if (route.query.teacherId) {
      const result = (await startConversation(route.query.teacherId)).data
      await refreshConversations()
      await openConversation(conversations.value.find((c) => c.id === result.id) || result)
      router.replace({ name: 'message', query: { tab: 'PRIVATE' } })
    } else if (conversations.value.length) await openConversation(conversations.value[0])
  } catch (e) {
    error.value = e?.message || '私信加载失败'
  } finally {
    loading.value = false
    connect()
  }
})
onBeforeUnmount(() => stomp?.deactivate())
watch(keyword, findTeachers)
</script>

<template>
  <div :class="embedded ? 'chat-embedded' : 'page chat-page'">
    <Teleport v-if="embedded" to="#chat-conversations-sidebar">
      <div class="chat-nested-list">
        <label class="chat-find"><Search /><input v-model="keyword" placeholder="搜索会话或教师" /></label>
        <p class="chat-nested-caption">我的会话 · {{ conversations.length }}</p>
        <button
          v-for="item in visibleConversations"
          :key="item.id"
          class="chat-contact"
          :class="{ active: selected?.id === item.id }"
          @click="openConversation(item)"
        >
          <span class="mini-avatar">{{ item.other.realName.slice(0, 1) }}</span
          ><span class="contact-copy"
            ><strong>{{ item.other.realName }}</strong
            ><small>{{ item.lastMessage || '开始聊天吧' }}</small></span
          ><span class="contact-meta"
            ><b v-if="item.unreadCount">{{ item.unreadCount }}</b></span
          >
        </button>
        <p v-if="!visibleConversations.length" class="contact-empty">暂无匹配会话</p>
        <template v-if="keyword.trim()"
          ><p class="chat-nested-caption">发起新私信</p>
          <button
            v-for="teacher in newTeachers"
            :key="teacher.id"
            class="chat-new-contact"
            @click="start(teacher)"
          >
            <UserRound :size="17" /><span
              >{{ teacher.realName }}<small>{{ teacher.college || '教师' }}</small></span
            >
          </button>
          <p v-if="!newTeachers.length" class="contact-empty">没有找到其他教师</p></template
        >
      </div>
    </Teleport>
    <header v-if="!embedded" class="page-title-row">
      <div>
        <h1>私信</h1>
        <p>与平台教师一对一交流，聊天记录会自动保存。</p>
      </div>
    </header>
    <p v-if="error" class="form-error">{{ error }}</p>
    <div v-if="loading" class="resource-empty">正在加载私信...</div>
    <section v-else class="chat-layout chat-single-layout">
      <main v-if="selected" class="chat-room">
        <header>
          <button
            class="chat-person"
            @click="router.push({ name: 'teacher-profile', params: { id: selected.other.id } })"
          >
            <span class="mini-avatar">{{ selected.other.realName.slice(0, 1) }}</span
            ><span
              ><strong>{{ selected.other.realName }}</strong
              ><small>{{ selected.other.college || '教师' }} · 点击查看主页</small></span
            ></button
          ><span class="chat-connect" :class="{ online: connected }">{{
            connected ? '实时连接正常' : '实时连接中断，可继续发送'
          }}</span>
        </header>
        <div ref="stream" class="chat-history">
          <button v-if="beforeId" class="older-button" :disabled="loadingHistory" @click="loadOlder">
            {{ loadingHistory ? '加载中...' : '加载更早消息' }}
          </button>
          <p v-if="!messages.length" class="chat-empty">还没有消息，打个招呼吧。</p>
          <article
            v-for="item in messages"
            :key="item.id"
            class="chat-bubble"
            :class="{ mine: item.senderId === me.id }"
          >
            <small>{{ formatTime(item.createTime) }}</small>
            <p>{{ item.content }}</p>
          </article>
        </div>
        <form class="chat-input" @submit.prevent="send">
          <textarea
            v-model="draft"
            maxlength="2000"
            placeholder="输入消息，Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="send"
          ></textarea>
          <footer>
            <span>{{ draft.length }} / 2000</span
            ><button class="button primary" :disabled="sending || !draft.trim()">
              <Send />{{ sending ? '发送中' : '发送' }}
            </button>
          </footer>
        </form>
      </main>
      <div v-else class="chat-no-room">
        <MessageCircle />
        <h2>还没有私信</h2>
        <p>在左侧“私信”下搜索教师，开始交流。</p>
      </div>
    </section>
  </div>
</template>
