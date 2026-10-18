<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BookOpen, Eye, Heart, Plus, Search } from 'lucide-vue-next'
import { getPublishedCases, toggleCaseFavorite } from '../api/case'
import { getCourses } from '../api/course'
import PaginationBar from '../components/PaginationBar.vue'
import '../styles/case-access.css'

const router = useRouter(),
  cases = ref([]),
  courses = ref([]),
  loading = ref(true),
  error = ref(''),
  notice = ref('')
const filters = reactive({ keyword: '', courseId: '' })
const pagination = reactive({ page: 1, size: 10, total: 0, pages: 0 })
const user = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const canCreate = user.roles?.some((role) => ['TEACHER', 'ADMIN'].includes(role))
const visibilityName = { PUBLIC: '公开' }
function plainText(html) {
  return (html || '').replace(/<[^>]*>/g, '')
}
async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await getPublishedCases({
      keyword: filters.keyword || undefined,
      courseId: filters.courseId || undefined,
      page: pagination.page,
      size: pagination.size,
    })
    cases.value = result.data || []
    Object.assign(pagination, {
      page: Number(result.page?.page || pagination.page),
      total: Number(result.page?.total || 0),
      pages: Number(result.page?.pages || 0),
    })
  } catch (e) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}
function search() {
  pagination.page = 1
  load()
}
function changePage(page) {
  pagination.page = page
  load()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}
async function favorite(item) {
  try {
    item.favorite = (await toggleCaseFavorite(item.id)).data
    notice.value = item.favorite ? '已收藏案例' : '已取消收藏'
  } catch (e) {
    error.value = e?.message || '收藏失败'
  }
}
onMounted(async () => {
  try {
    courses.value = (await getCourses()).data || []
  } catch (e) {
    error.value = e?.message || '课程加载失败'
  }
  await load()
})
</script>

<template>
  <div class="page case-page">
    <header class="page-title-row">
      <div>
        <h1>教学案例库</h1>
        <p>这里展示已通过审核、对您可见的教学案例。可从个人草稿箱选取内容继续编辑并发布。</p>
      </div>
      <div class="button-row">
        <button v-if="canCreate" class="button secondary" @click="router.push('/app/my-cases?tab=draft')">
          从草稿箱选择</button
        ><button v-if="canCreate" class="button primary" @click="router.push('/app/case/new')">
          <Plus />创作案例
        </button>
      </div>
    </header>
    <section class="panel case-filters">
      <label
        ><Search /><input
          v-model="filters.keyword"
          placeholder="搜索已发布案例"
          @keyup.enter="search" /></label
      ><select v-model="filters.courseId" @change="search">
        <option value="">全部课程</option>
        <option v-for="course in courses" :key="course.id" :value="course.id">
          {{ course.courseName }}
        </option></select
      ><button class="button secondary" @click="search">检索</button>
    </section>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="form-success">{{ notice }}</p>
    <div v-if="loading" class="panel resource-empty">正在加载案例...</div>
    <div v-else-if="!cases.length" class="panel resource-empty">没有找到已通过审核的案例。</div>
    <section v-else class="case-feed">
      <article v-for="item in cases" :key="item.id" class="panel case-post">
        <header>
          <div>
            <span class="post-course"><BookOpen />{{ item.courseName || '未关联课程' }}</span>
            <h2>{{ item.caseName }}</h2>
            <div class="case-tags">
              <span v-for="name in item.knowledgePointNames" :key="name"># {{ name }}</span>
            </div>
          </div>
          <i class="approved">已发布</i>
        </header>
        <p class="post-summary">{{ item.summary || plainText(item.content) }}</p>
        <div class="post-meta">
          {{ item.creatorName }} · {{ visibilityName[item.visibility] || '公共' }} ·
          {{ item.accessType === 'POINTS' ? item.pointsPrice + ' 积分' : '免费访问' }} ·
          {{ item.resourceNames?.length || 0 }} 份引用资源
        </div>
        <footer>
          <button v-if="item.contentUnlocked" :class="{ active: item.favorite }" @click="favorite(item)">
            <Heart :fill="item.favorite ? 'currentColor' : 'none'" />{{
              item.favorite ? '已收藏' : '收藏'
            }}</button
          ><span v-else class="case-unlock-hint">解锁后可收藏</span
          ><button @click="router.push({ name: 'case-detail', params: { id: item.id } })">
            <Eye />{{ item.contentUnlocked ? '阅读全文' : '查看并解锁' }}
          </button>
        </footer>
      </article>
    </section>
    <PaginationBar
      :page="pagination.page"
      :pages="pagination.pages"
      :total="pagination.total"
      @change="changePage"
    />
  </div>
</template>
