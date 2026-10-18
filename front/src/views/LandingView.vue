<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search,
  ArrowRight,
  BookOpen,
  Users,
  Sparkles,
  FileText,
  Image,
  Video,
  LockKeyhole,
} from 'lucide-vue-next'
import BrandLogo from '../components/BrandLogo.vue'
import { getPublicHome, searchPublicContent } from '../api/public'

const router = useRouter()
const keyword = ref('')
const homeLoading = ref(true)
const searching = ref(false)
const errorMessage = ref('')
const searched = ref(false)
const home = ref({ teacherCount: 0, resourceCount: 0, caseCount: 0, featuredCases: [] })
const results = ref({ resources: [], cases: [] })

const resultCount = computed(() => results.value.resources.length + results.value.cases.length)
const caseColors = ['blue', 'purple', 'green']

onMounted(async () => {
  try {
    const response = await getPublicHome()
    home.value = response.data
  } catch (error) {
    errorMessage.value = error.message || '公共内容加载失败，请稍后刷新页面'
  } finally {
    homeLoading.value = false
  }
})

const formatCount = (value) => new Intl.NumberFormat('zh-CN').format(Number(value || 0))
const summary = (value, fallback = '登录后可查看完整介绍和教学内容。') => value?.trim() || fallback
const caseColor = (index) => caseColors[index % caseColors.length]
const resourceIcon = (type) => {
  if (type === 'VIDEO') return Video
  if (type === 'IMAGE') return Image
  return FileText
}
const resourceType = (type) =>
  ({ VIDEO: '视频', IMAGE: '图片', PDF: 'PDF', DOCUMENT: '文档', COURSEWARE: '课件' })[type] || type || '资源'

const loginTo = (redirect) => router.push({ name: 'login', query: { redirect } })
const openCase = (id) => loginTo(`/app/case/${id}`)
const openResource = (id) => loginTo(`/app/resource?id=${id}`)
const browseAllCases = () => loginTo('/app/case')

const scrollToContent = () =>
  document.querySelector('#public-content')?.scrollIntoView({ behavior: 'smooth' })

const search = async () => {
  const value = keyword.value.trim()
  if (!value) {
    errorMessage.value = '请输入资源或案例关键词'
    document.querySelector('.public-search input')?.focus()
    return
  }
  searching.value = true
  searched.value = true
  errorMessage.value = ''
  try {
    const response = await searchPublicContent(value)
    results.value = response.data
    await nextTick()
    document.querySelector('#search-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  } catch (error) {
    results.value = { resources: [], cases: [] }
    errorMessage.value = error.message || '搜索失败，请稍后重试'
  } finally {
    searching.value = false
  }
}
</script>

<template>
  <div class="landing-page">
    <header class="public-header container-wide">
      <BrandLogo />
      <nav>
        <a href="#features">平台能力</a><a href="#public-content">公开内容</a><a href="#about">关于平台</a>
      </nav>
      <form class="public-search" @submit.prevent="search">
        <button type="submit" aria-label="搜索公开内容"><Search :size="16" /></button>
        <input v-model="keyword" maxlength="50" placeholder="搜索公开案例或资源..." />
      </form>
      <button class="text-button" @click="router.push('/login')">登录</button>
      <button class="button primary pill" @click="router.push('/login')">进入平台</button>
    </header>

    <main>
      <section id="features" class="hero">
        <div class="hero-glow glow-one" />
        <div class="hero-glow glow-two" />
        <div class="hero-badge"><Sparkles :size="14" /> AI 驱动的新一代教学资源平台</div>
        <h1>重塑备课体验，<br /><span>让优质资源触手可及</span></h1>
        <p>汇聚课程、知识点、教学案例与多媒体资源，借助智能技术快速形成高质量案例草稿。</p>
        <div class="hero-actions">
          <button class="button primary large" @click="router.push('/login')">
            立即开始备课 <ArrowRight :size="18" />
          </button>
          <button class="button secondary large" @click="scrollToContent">
            <BookOpen :size="19" />浏览公开内容
          </button>
        </div>
        <div v-if="!homeLoading" class="hero-proof" aria-label="平台真实数据统计">
          <span>已有</span><strong>{{ formatCount(home.teacherCount) }}</strong
          ><span>名教师</span><i /> <strong>{{ formatCount(home.resourceCount) }}</strong
          ><span>份公开资源</span><i /> <strong>{{ formatCount(home.caseCount) }}</strong
          ><span>篇公开案例</span>
        </div>
      </section>

      <section id="public-content" class="case-section container">
        <div class="section-heading">
          <div>
            <small>POPULAR RESOURCES</small>
            <h2>热门教学案例</h2>
            <p>数据来自已通过审核的公开案例，优秀案例会优先展示。</p>
          </div>
          <button class="section-link" @click="browseAllCases">
            登录后查看全部 <ArrowRight :size="16" />
          </button>
        </div>
        <div v-if="homeLoading" class="public-state">正在读取公开内容...</div>
        <div v-else-if="home.featuredCases.length" class="case-grid">
          <article v-for="(item, index) in home.featuredCases" :key="item.id" class="case-card">
            <div class="case-cover" :class="caseColor(index)">
              <Sparkles v-if="item.excellent" :size="52" />
              <BookOpen v-else :size="52" />
              <span>{{ item.excellent ? '优秀案例' : item.courseName || '教学案例' }}</span>
            </div>
            <div class="case-content">
              <small>{{ item.courseName || '暂未关联课程' }} · {{ item.creatorName }}</small>
              <h3>{{ item.name }}</h3>
              <p>{{ summary(item.summary) }}</p>
              <div class="public-card-meta">
                <span v-if="item.accessType === 'POINTS'"
                  ><LockKeyhole :size="13" />{{ item.pointsPrice }} 积分</span
                >
                <span v-else>免费公开</span>
                <button @click="openCase(item.id)">登录后查看 <ArrowRight :size="14" /></button>
              </div>
            </div>
          </article>
        </div>
        <div v-else class="public-state">目前还没有审核通过的公开案例。</div>

        <form class="public-search-panel" @submit.prevent="search">
          <div>
            <strong>搜索公共资源库与案例库</strong>
            <span>搜索结果仅包含公开且审核通过的内容</span>
          </div>
          <label
            ><Search :size="18" /><input v-model="keyword" maxlength="50" placeholder="输入名称、课程或教师"
          /></label>
          <button class="button primary" type="submit" :disabled="searching">
            {{ searching ? '搜索中...' : '开始搜索' }}
          </button>
        </form>

        <p v-if="errorMessage" class="public-error">{{ errorMessage }}</p>

        <section v-if="searched" id="search-results" class="search-results">
          <div class="search-result-title">
            <div>
              <small>SEARCH RESULTS</small>
              <h2>“{{ results.keyword || keyword }}”的搜索结果</h2>
            </div>
            <span>共找到 {{ resultCount }} 项公开内容</span>
          </div>

          <template v-if="resultCount">
            <div v-if="results.resources.length" class="result-group">
              <h3>
                教学资源 <span>{{ results.resources.length }}</span>
              </h3>
              <div class="public-result-grid">
                <article
                  v-for="item in results.resources"
                  :key="`resource-${item.id}`"
                  class="public-result-card"
                >
                  <div class="result-icon resource">
                    <component :is="resourceIcon(item.type)" :size="23" />
                  </div>
                  <div>
                    <small>{{ resourceType(item.type) }} · {{ item.uploaderName }}</small>
                    <h4>{{ item.name }}</h4>
                    <p>{{ summary(item.description, '该资源暂未填写简介。') }}</p>
                    <button @click="openResource(item.id)">登录后查看资源 <ArrowRight :size="14" /></button>
                  </div>
                </article>
              </div>
            </div>
            <div v-if="results.cases.length" class="result-group">
              <h3>
                教学案例 <span>{{ results.cases.length }}</span>
              </h3>
              <div class="public-result-grid">
                <article v-for="item in results.cases" :key="`case-${item.id}`" class="public-result-card">
                  <div class="result-icon case">
                    <Sparkles v-if="item.excellent" :size="23" /><BookOpen v-else :size="23" />
                  </div>
                  <div>
                    <small
                      >{{ item.excellent ? '优秀案例' : item.courseName || '教学案例' }} ·
                      {{ item.creatorName }}</small
                    >
                    <h4>{{ item.name }}</h4>
                    <p>{{ summary(item.summary) }}</p>
                    <button @click="openCase(item.id)">登录后查看案例 <ArrowRight :size="14" /></button>
                  </div>
                </article>
              </div>
            </div>
          </template>
          <div v-else class="public-state">没有找到匹配的公开内容，请尝试其他关键词。</div>
        </section>
      </section>
    </main>

    <footer id="about" class="public-footer">
      <BrandLogo />
      <p>EduMix 聚合课程、教学资源与案例，所有公共内容均经过平台审核。</p>
      <button class="text-button" @click="router.push('/register')">注册教师账号</button>
    </footer>
  </div>
</template>
