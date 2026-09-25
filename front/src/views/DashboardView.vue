<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  BadgeCheck,
  BookOpen,
  Coins,
  FolderOpen,
  Heart,
  Lightbulb,
  Plus,
  Sparkles,
  Upload,
  WandSparkles,
} from 'lucide-vue-next'
import { getDashboardSummary } from '../api/dashboard'
import '../styles/dashboard.css'

const router = useRouter()
const user = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const roles = user.roles || []
const canTeach = roles.includes('TEACHER') || roles.includes('ADMIN')
const canReview = roles.includes('RESOURCE_MANAGER') || roles.includes('ADMIN')
const isAdmin = roles.includes('ADMIN')
const summary = ref(null),
  loading = ref(true),
  error = ref('')
const greeting = computed(() => {
  const hour = new Date().getHours()
  return hour < 11 ? '早上好' : hour < 18 ? '下午好' : '晚上好'
})
const displayName = computed(() => summary.value?.realName || user.realName || user.username || '老师')
const stats = computed(() => {
  const data = summary.value || {}
  const items = [
    { title: '我的课程', value: data.courseCount ?? 0, icon: BookOpen, color: 'blue', route: '/app/course' },
    {
      title: '我上传的资源',
      value: data.resourceCount ?? 0,
      icon: FolderOpen,
      color: 'indigo',
      route: '/app/resource',
    },
    {
      title: '我的案例',
      value: data.caseCount ?? 0,
      icon: Lightbulb,
      color: 'purple',
      route: '/app/my-cases',
    },
    {
      title: '我的收藏',
      value: (data.resourceFavoriteCount ?? 0) + (data.caseFavoriteCount ?? 0),
      icon: Heart,
      color: 'cyan',
      route: '/app/favorites',
    },
  ]
  if (canReview)
    items.push({
      title: '待我审核',
      value: (data.pendingResourceCount ?? 0) + (data.pendingCaseCount ?? 0),
      icon: BadgeCheck,
      color: 'blue',
      route: '/app/reviews',
    })
  return items
})
const actions = computed(() => [
  ...(canTeach
    ? [
        { name: '管理课程', icon: BookOpen, color: 'blue', route: '/app/course' },
        { name: '上传资源', icon: Upload, color: 'indigo', route: '/app/resource?action=upload' },
        { name: '创作案例', icon: Plus, color: 'green', route: '/app/case/new' },
        { name: '我的案例', icon: Lightbulb, color: 'purple', route: '/app/my-cases' },
        { name: 'AI资源整合', icon: WandSparkles, color: 'amber', route: '/app/ai' },
      ]
    : []),
  ...(canReview ? [{ name: '审核中心', icon: BadgeCheck, color: 'amber', route: '/app/reviews' }] : []),
  ...(isAdmin ? [{ name: '账号管理', icon: BookOpen, color: 'blue', route: '/app/admin/accounts' }] : []),
  { name: '个人收藏', icon: Heart, color: 'green', route: '/app/favorites' },
  { name: '我的积分', icon: Coins, color: 'amber', route: '/app/points' },
])
const dateText = (value) => (value ? new Date(value).toLocaleDateString('zh-CN') : '—')
async function load() {
  loading.value = true
  error.value = ''
  try {
    summary.value = (await getDashboardSummary()).data
  } catch (e) {
    error.value = e?.message || '工作台加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>

<template>
  <div class="page dashboard-page">
    <section class="welcome-card panel">
      <div>
        <h1>{{ greeting }}，{{ displayName }}！</h1>
        <p>这里汇总了您自己的课程、资源、案例和收藏。选择一项工作继续吧。</p>
        <div class="button-row">
          <button v-if="canTeach" class="button primary" @click="router.push('/app/course')">
            <BookOpen :size="18" />管理我的课程</button
          ><button
            v-if="canTeach"
            class="button secondary"
            @click="router.push('/app/resource?action=upload')"
          >
            <Upload :size="18" />上传新资源</button
          ><button v-if="canReview && !canTeach" class="button primary" @click="router.push('/app/reviews')">
            <BadgeCheck :size="18" />处理审核
          </button>
        </div>
      </div>
      <div class="dashboard-welcome-art"><BookOpen :size="72" /></div>
    </section>

    <p v-if="error" class="form-error">
      {{ error }} <button class="dashboard-retry" @click="load">重新加载</button>
    </p>
    <section v-if="loading" class="panel dashboard-loading">正在加载工作台数据...</section>
    <template v-else-if="summary">
      <section class="stat-grid" :class="{ 'five-stats': canReview }">
        <button
          v-for="stat in stats"
          :key="stat.title"
          class="stat-card panel dashboard-stat-button"
          @click="router.push(stat.route)"
        >
          <div>
            <span class="stat-icon" :class="stat.color"><component :is="stat.icon" :size="21" /></span
            ><ArrowRight :size="16" class="stat-arrow" />
          </div>
          <strong>{{ stat.value }}</strong>
          <p>{{ stat.title }}</p>
        </button>
      </section>
      <section class="dashboard-grid">
        <article class="panel course-progress">
          <header>
            <h2>我的最近课程</h2>
            <button @click="router.push('/app/course')">查看全部 <ArrowRight :size="15" /></button>
          </header>
          <div v-if="!summary.recentCourses?.length" class="dashboard-empty">
            <p>还没有创建课程。建立课程后，就可以整理章节、知识点并关联收藏资源。</p>
            <button v-if="canTeach" class="button primary" @click="router.push('/app/course')">
              去新建课程
            </button>
          </div>
          <div v-else class="course-list">
            <button
              v-for="course in summary.recentCourses"
              :key="course.id"
              class="course-row dashboard-course-button"
              @click="router.push({ path: '/app/course', query: { courseId: course.id } })"
            >
              <span class="course-icon"><BookOpen :size="23" /></span
              ><span class="course-copy"
                ><strong>{{ course.courseName }}</strong
                ><span class="dashboard-course-meta"
                  >{{ course.chapterCount }} 个章节 · {{ course.knowledgePointCount }} 个知识点 · 更新于
                  {{ dateText(course.updateTime) }}</span
                ></span
              ><ArrowRight :size="16" />
            </button>
          </div>
        </article>
        <aside class="dashboard-side">
          <article v-if="canReview" class="panel dashboard-review-card">
            <h3><BadgeCheck :size="19" />审核待办</h3>
            <p>资源 {{ summary.pendingResourceCount }} 件 · 案例 {{ summary.pendingCaseCount }} 篇</p>
            <button class="button secondary" @click="router.push('/app/reviews')">
              进入审核中心 <ArrowRight :size="15" />
            </button>
          </article>
          <article v-if="canTeach" class="ai-card">
            <Sparkles class="ai-watermark" :size="100" />
            <h3><WandSparkles :size="20" />AI 资源整合</h3>
            <p>从个人收藏的资源和案例中选择素材，获取推荐或进行对话整合。</p>
            <button @click="router.push('/app/ai')">打开 AI 工作台</button>
          </article>
          <article class="panel quick-actions">
            <h3>快捷操作</h3>
            <div>
              <button
                v-for="action in actions"
                :key="action.name"
                :class="action.color"
                @click="router.push(action.route)"
              >
                <component :is="action.icon" :size="23" />{{ action.name }}
              </button>
            </div>
          </article>
        </aside>
      </section>
    </template>
  </div>
</template>
