<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { Coins, CalendarCheck, RefreshCw } from 'lucide-vue-next'
import { checkIn, getPointHistory, getWallet } from '../api/points'
import '../styles/points.css'

const wallet = ref(null),
  history = ref([]),
  loading = ref(true),
  busy = ref(false),
  error = ref(''),
  notice = ref('')
const roles = JSON.parse(localStorage.getItem('edumix-user') || '{}').roles || []
const canCheckIn = roles.includes('TEACHER') || roles.includes('ADMIN')
const names = {
  REGISTER: '注册奖励',
  DAILY_CHECKIN: '每日签到',
  CASE_APPROVED_FREE: '免费案例审核通过',
  CASE_APPROVED_PAID: '积分案例审核通过',
  CASE_UNLOCK_SPEND: '解锁案例',
  CASE_UNLOCK_INCOME: '案例被解锁',
  ADMIN_GRANT: '管理员发放',
  CASE_APPROVED: '旧版案例奖励',
  CASE_UNLOCK: '旧版案例解锁',
}
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [a, b] = await Promise.all([getWallet(), getPointHistory()])
    wallet.value = a.data
    history.value = b.data || []
  } catch (e) {
    error.value = e?.message || '积分信息加载失败'
  } finally {
    loading.value = false
  }
}
async function sign() {
  busy.value = true
  error.value = ''
  notice.value = ''
  try {
    wallet.value = (await checkIn()).data
    notice.value = '签到成功，获得 5 积分'
    history.value = (await getPointHistory()).data || []
    window.dispatchEvent(new Event('edumix-points-changed'))
  } catch (e) {
    error.value = e?.message || '签到失败'
  } finally {
    busy.value = false
  }
}
onMounted(() => {
  load()
  window.addEventListener('edumix-points-changed', load)
})
onBeforeUnmount(() => window.removeEventListener('edumix-points-changed', load))
</script>

<template>
  <div class="page points-page">
    <header class="page-title-row">
      <div>
        <h1>我的积分</h1>
        <p>查看积分余额、每日签到和每一笔收支。</p>
      </div>
      <button class="button secondary" @click="load"><RefreshCw :size="17" />刷新</button>
    </header>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="form-success">{{ notice }}</p>
    <div v-if="loading" class="panel resource-empty">正在加载积分...</div>
    <template v-else-if="wallet"
      ><section class="points-overview panel">
        <div>
          <Coins :size="28" /><span>当前积分</span><strong>{{ wallet.balance }}</strong
          ><small>累计获得 {{ wallet.totalIncome }} · 累计支出 {{ wallet.totalExpense }}</small>
        </div>
        <div v-if="canCheckIn" class="points-checkin">
          <p>每日签到可获得 <b>5 积分</b></p>
          <button class="button primary" :disabled="busy || wallet.checkedInToday" @click="sign">
            <CalendarCheck :size="17" />{{ wallet.checkedInToday ? '今日已签到' : '立即签到' }}
          </button>
        </div>
      </section>
      <section class="panel points-rules">
        <h2>积分规则</h2>
        <p>
          注册教师账号 +20；每日签到 +5；免费案例审核通过 +20；积分案例审核通过 +10。积分案例定价 1～3
          分，首次解锁扣除价格并同额转给作者。
        </p>
      </section>
      <section class="panel points-history">
        <h2>积分明细</h2>
        <div v-if="!history.length" class="resource-empty">暂无积分记录</div>
        <div v-for="entry in history" :key="entry.id" class="points-entry">
          <div>
            <strong>{{ names[entry.businessType] || entry.businessType }}</strong
            ><small>{{ entry.remark }} · {{ new Date(entry.createTime).toLocaleString('zh-CN') }}</small>
          </div>
          <div>
            <b :class="entry.changeAmount > 0 ? 'income' : 'expense'"
              >{{ entry.changeAmount > 0 ? '+' : '' }}{{ entry.changeAmount }}</b
            ><small>余额 {{ entry.balanceAfter }}</small>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
