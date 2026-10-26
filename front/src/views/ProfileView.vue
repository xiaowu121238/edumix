<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, BookOpen, Coins, FileText, Pencil, UserRound } from 'lucide-vue-next'
import { getCurrentUser } from '../api/auth'
import { getMyResources, getMyResourceStats } from '../api/resource'
import { getMyCases } from '../api/case'
import '../styles/profile-home.css'

const router = useRouter()
const profile = ref(null)
const resources = ref([])
const resourceStats = ref({ total: 0, approved: 0 })
const cases = ref([])
const loading = ref(true)
const error = ref('')
const tab = ref('resources')
const roleNames = { TEACHER: '教师', RESOURCE_MANAGER: '资源负责人', ADMIN: '管理员' }
const approvedCount = computed(
  () =>
    resourceStats.value.approved +
    cases.value.filter((item) => item.auditStatus === 'APPROVED' && item.visibility === 'PUBLIC').length,
)
const creatorLevel = computed(() => (approvedCount.value >= 10 ? 3 : approvedCount.value >= 3 ? 2 : 1))
const nextLevel = computed(() => (creatorLevel.value === 1 ? 3 : creatorLevel.value === 2 ? 10 : null))
const visibleItems = computed(() => (tab.value === 'resources' ? resources.value : cases.value).slice(0, 6))
function caseStatus(item) {
  if (item.visibility !== 'PUBLIC') return '仅自己可见'
  if (item.auditStatus === 'DRAFT') return '草稿'
  return { PENDING: '待审核', REJECTED: '已驳回', APPROVED: '已发布' }[item.auditStatus] || item.auditStatus
}
function resourceStatus(item) {
  if (item.visibility !== 'PUBLIC') return '仅自己可见'
  return { PENDING: '待审核', REJECTED: '已驳回', APPROVED: '已通过' }[item.auditStatus] || item.auditStatus
}
function openCase(item) {
  router.push({
    name:
      ['DRAFT', 'REJECTED'].includes(item.auditStatus) || item.visibility !== 'PUBLIC'
        ? 'case-edit'
        : 'case-detail',
    params: { id: item.id },
  })
}
async function load() {
  loading.value = true
  error.value = ''
  const results = await Promise.allSettled([
    getCurrentUser(),
    getMyResources({ size: 6 }),
    getMyCases(),
    getMyResourceStats(),
  ])
  if (results[0].status === 'fulfilled') {
    profile.value = results[0].value.data
    localStorage.setItem('edumix-user', JSON.stringify(profile.value))
    window.dispatchEvent(new Event('edumix-profile-changed'))
  } else error.value = results[0].reason?.message || '个人资料加载失败'
  if (results[1].status === 'fulfilled') resources.value = results[1].value.data || []
  else error.value = [error.value, '资源列表加载失败'].filter(Boolean).join('；')
  if (results[2].status === 'fulfilled') cases.value = results[2].value.data || []
  else error.value = [error.value, '案例列表加载失败'].filter(Boolean).join('；')
  if (results[3].status === 'fulfilled') resourceStats.value = results[3].value.data
  else error.value = [error.value, '资源统计加载失败'].filter(Boolean).join('；')
  loading.value = false
}
onMounted(load)
</script>

<template>
  <div class="page personal-home">
    <p v-if="error" class="form-error">{{ error }}</p>
    <div v-if="loading" class="panel resource-empty">正在加载个人主页...</div>
    <template v-else-if="profile">
      <section class="personal-hero panel">
        <div class="personal-cover"></div>
        <div class="personal-identity">
          <div class="personal-avatar">
            <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="个人头像" /><UserRound
              v-else
              :size="42"
            />
          </div>
          <div class="personal-name">
            <span class="personal-eyebrow">我的个人主页</span>
            <h1>{{ profile.realName || profile.username }}</h1>
            <p>
              @{{ profile.username }} <span v-if="profile.college">· {{ profile.college }}</span
              ><span v-if="profile.title"> · {{ profile.title }}</span>
            </p>
            <div class="personal-roles">
              <span v-for="role in profile.roles || []" :key="role">{{ roleNames[role] || role }}</span>
            </div>
          </div>
          <button class="button primary personal-edit" @click="router.push({ name: 'profile-edit' })">
            <Pencil :size="17" />编辑资料
          </button>
        </div>
      </section>

      <section class="personal-stats" aria-label="账户概况">
        <div class="panel personal-stat">
          <span class="personal-stat-icon blue"><FileText :size="22" /></span>
          <div>
            <small>上传资源</small><strong>{{ resourceStats.total }}</strong>
          </div>
        </div>
        <div class="panel personal-stat">
          <span class="personal-stat-icon purple"><BookOpen :size="22" /></span>
          <div>
            <small>创作案例</small><strong>{{ cases.length }}</strong>
          </div>
        </div>
        <div class="panel personal-stat">
          <span class="personal-stat-icon amber"><Coins :size="22" /></span>
          <div>
            <small>当前积分</small><strong>{{ profile.pointBalance || 0 }}</strong>
          </div>
        </div>
        <div class="panel personal-stat personal-level">
          <span class="personal-level-mark">Lv.{{ creatorLevel }}</span>
          <div>
            <small>创作等级</small
            ><strong>{{
              creatorLevel === 3 ? '资深创作者' : creatorLevel === 2 ? '活跃创作者' : '新晋创作者'
            }}</strong
            ><em>{{
              nextLevel
                ? `再有 ${nextLevel - approvedCount} 项审核通过的公开作品可升级`
                : '已达到当前最高等级'
            }}</em>
          </div>
        </div>
      </section>

      <section class="panel personal-work">
        <div class="personal-section-head">
          <div>
            <span class="personal-eyebrow">MY CREATIONS</span>
            <h2>我的创作</h2>
            <p>这里汇总自己上传的资源和案例，包括待审核内容。</p>
          </div>
          <button
            class="personal-more"
            @click="
              router.push(tab === 'resources' ? { name: 'resource', query: { mine: '1' } } : '/app/my-cases')
            "
          >
            {{ tab === 'resources' ? '查看我的资源' : '查看全部案例' }}<ArrowRight :size="17" />
          </button>
        </div>
        <div class="personal-tabs">
          <button :class="{ active: tab === 'resources' }" @click="tab = 'resources'">
            上传的资源 <span>{{ resourceStats.total }}</span></button
          ><button :class="{ active: tab === 'cases' }" @click="tab = 'cases'">
            创作的案例 <span>{{ cases.length }}</span>
          </button>
        </div>
        <div v-if="!visibleItems.length" class="personal-empty">
          {{
            tab === 'resources'
              ? '还没有上传资源，去资源库开始上传吧。'
              : '还没有创作案例，去案例编辑器开始创作吧。'
          }}
        </div>
        <div v-else class="personal-list">
          <button
            v-for="item in visibleItems"
            :key="item.id"
            class="personal-item"
            @click="
              tab === 'resources'
                ? router.push({ name: 'resource', query: { mine: '1', id: item.id } })
                : openCase(item)
            "
          >
            <span class="personal-item-icon"
              ><FileText v-if="tab === 'resources'" :size="20" /><BookOpen v-else :size="20"
            /></span>
            <span class="personal-item-main"
              ><strong>{{ tab === 'resources' ? item.resourceName : item.caseName || '未命名草稿' }}</strong
              ><small
                >{{
                  tab === 'resources'
                    ? `${item.categoryName || '未分类'} · ${item.resourceType || '文件'}`
                    : item.courseName || '未选课程'
                }}<span v-if="item.createTime">
                  · {{ new Date(item.createTime).toLocaleDateString('zh-CN') }}</span
                ></small
              ></span
            >
            <span class="personal-item-status">{{
              tab === 'resources' ? resourceStatus(item) : caseStatus(item)
            }}</span
            ><ArrowRight :size="18" class="personal-item-arrow" />
          </button>
        </div>
      </section>
    </template>
  </div>
</template>
