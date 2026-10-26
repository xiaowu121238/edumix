<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { CalendarCheck2, ChevronLeft, ChevronRight } from 'lucide-vue-next'
import { checkIn, getCheckInDates, getWallet } from '../api/points'
import '../styles/header-checkin.css'

const roles = JSON.parse(localStorage.getItem('edumix-user') || '{}').roles || []
const available = roles.includes('TEACHER') || roles.includes('ADMIN')
const root = ref(null),
  open = ref(false),
  busy = ref(false),
  loading = ref(false),
  error = ref(''),
  wallet = ref(null),
  dates = ref([])
function todayParts() {
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat('en-US', {
      timeZone: 'Asia/Shanghai',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    })
      .formatToParts(new Date())
      .filter((p) => p.type !== 'literal')
      .map((p) => [p.type, Number(p.value)]),
  )
  return { year: parts.year, month: parts.month, day: parts.day }
}
const today = ref(todayParts())
const viewed = ref(new Date(today.value.year, today.value.month - 1, 1))
const year = computed(() => viewed.value.getFullYear()),
  month = computed(() => viewed.value.getMonth() + 1)
const sameMonth = computed(() => year.value === today.value.year && month.value === today.value.month)
const checked = computed(() => new Set(dates.value))
const cells = computed(() => {
  const blanks = (new Date(year.value, month.value - 1, 1).getDay() + 6) % 7
  const days = new Date(year.value, month.value, 0).getDate()
  const result = [...Array(blanks).fill(null), ...Array.from({ length: days }, (_, i) => i + 1)]
  while (result.length % 7) result.push(null)
  return result
})
const dateKey = (day) =>
  `${year.value}-${String(month.value).padStart(2, '0')}-${String(day).padStart(2, '0')}`
async function refreshWallet() {
  try {
    wallet.value = (await getWallet({ skipErrorNotice: true })).data
  } catch {}
}
async function loadDates() {
  const requested = `${year.value}-${month.value}`
  loading.value = true
  error.value = ''
  try {
    const data = (await getCheckInDates(year.value, month.value)).data || []
    if (requested === `${year.value}-${month.value}`) dates.value = data
  } catch (e) {
    error.value = e?.message || '签到日历加载失败'
  } finally {
    if (requested === `${year.value}-${month.value}`) loading.value = false
  }
}
async function toggle() {
  open.value = !open.value
  if (open.value) {
    today.value = todayParts()
    viewed.value = new Date(today.value.year, today.value.month - 1, 1)
    await Promise.all([refreshWallet(), loadDates()])
  }
}
async function changeMonth(offset) {
  viewed.value = new Date(year.value, month.value - 1 + offset, 1)
  dates.value = []
  await loadDates()
}
async function returnCurrentMonth() {
  today.value = todayParts()
  viewed.value = new Date(today.value.year, today.value.month - 1, 1)
  dates.value = []
  await loadDates()
}
async function sign() {
  if (busy.value || wallet.value?.checkedInToday) return
  busy.value = true
  error.value = ''
  try {
    wallet.value = (await checkIn()).data
    await loadDates()
    window.dispatchEvent(new Event('edumix-points-changed'))
  } catch (e) {
    error.value = e?.message || '签到失败'
    await refreshWallet()
    await loadDates()
  } finally {
    busy.value = false
  }
}
function outside(event) {
  if (open.value && !root.value?.contains(event.target)) open.value = false
}
function escape(event) {
  if (event.key === 'Escape') open.value = false
}
function pointsChanged() {
  refreshWallet()
  if (open.value) loadDates()
}
onMounted(() => {
  if (!available) return
  refreshWallet()
  document.addEventListener('pointerdown', outside)
  document.addEventListener('keydown', escape)
  window.addEventListener('edumix-points-changed', pointsChanged)
})
onBeforeUnmount(() => {
  document.removeEventListener('pointerdown', outside)
  document.removeEventListener('keydown', escape)
  window.removeEventListener('edumix-points-changed', pointsChanged)
})
</script>

<template>
  <div v-if="available" ref="root" class="header-checkin">
    <button
      class="header-checkin-trigger"
      type="button"
      :aria-expanded="open"
      aria-label="打开签到日历"
      @click="toggle"
    >
      <CalendarCheck2 :size="19" /><span>{{ wallet?.checkedInToday ? '已签到' : '签到' }}</span>
    </button>
    <section v-if="open" class="checkin-popover" aria-label="签到日历">
      <header>
        <div><strong>每日签到</strong><small>每天签到可获得 5 积分</small></div>
        <span class="checkin-balance">{{ wallet?.balance ?? '—' }} 积分</span>
      </header>
      <div class="checkin-month">
        <button type="button" aria-label="上个月" @click="changeMonth(-1)"><ChevronLeft :size="18" /></button
        ><b>{{ year }} 年 {{ month }} 月</b
        ><button type="button" aria-label="下个月" @click="changeMonth(1)">
          <ChevronRight :size="18" />
        </button>
      </div>
      <div class="checkin-calendar">
        <span
          v-for="name in ['一', '二', '三', '四', '五', '六', '日']"
          :key="name"
          class="checkin-weekday"
          >{{ name }}</span
        ><span
          v-for="(day, index) in cells"
          :key="index"
          class="checkin-day"
          :class="{ done: day && checked.has(dateKey(day)), today: day && sameMonth && day === today.day }"
          :title="day && checked.has(dateKey(day)) ? '已签到' : ''"
          >{{ day || '' }}</span
        >
      </div>
      <p v-if="loading" class="checkin-hint">正在加载签到记录...</p>
      <p v-if="error" class="checkin-error">{{ error }}</p>
      <button
        v-if="sameMonth"
        type="button"
        class="checkin-action"
        :disabled="busy || wallet?.checkedInToday"
        @click="sign"
      >
        {{ wallet?.checkedInToday ? '今天已签到' : busy ? '签到中...' : '立即签到 · +5 积分' }}
      </button>
      <button v-else type="button" class="checkin-back" @click="returnCurrentMonth">返回本月签到</button>
    </section>
  </div>
</template>
