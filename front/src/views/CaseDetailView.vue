<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, BookOpen, Calendar, GraduationCap, Heart, User } from 'lucide-vue-next'
import { getCase, getPublishedCases, toggleCaseFavorite, unlockCase } from '../api/case'
import { getResourceFile, getResourcesByIds } from '../api/resource'
import '../styles/case-paywall.css'

const route = useRoute(),
  router = useRouter(),
  item = ref(null),
  recommendations = ref([]),
  resourceMap = ref(new Map()),
  articleBody = ref(null),
  loading = ref(true),
  error = ref(''),
  unlocking = ref(false)
const blobUrls = new Map(),
  visibilityName = { PRIVATE: '仅自己', PUBLIC: '公开' }
const legacyContent = computed(() => {
  const c = item.value || {}
  return [
    c.teachingObjective && `教学目标\n${c.teachingObjective}`,
    c.keyPoints && `教学重点\n${c.keyPoints}`,
    c.difficultPoints && `教学难点\n${c.difficultPoints}`,
    c.teachingProcess && `实施过程\n${c.teachingProcess}`,
    c.teachingReflection && `教学反思\n${c.teachingReflection}`,
  ]
    .filter(Boolean)
    .join('\n\n')
})
const renderedContent = computed(() => {
  if (item.value?.contentUnlocked === false)
    return `<div class="case-paywall"><h2>此案例需要积分解锁</h2><p>解锁需 ${Number(item.value.pointsPrice) || 0} 积分，首次解锁后可反复阅读；积分将转给案例作者。</p><button data-unlock-case type="button">${unlocking.value ? '正在解锁...' : '解锁完整案例'}</button></div>`
  let content = item.value?.content || legacyContent.value
  if (!/<[a-z][\s\S]*>/i.test(content)) content = content.replace(/\n/g, '<br>')
  return content.replace(/\[\[resource:(\d+)\|([^\]]+)]]/g, (_, id, name) => {
    const r = resourceMap.value.get(Number(id))
    if (r?.resourceType === 'IMAGE')
      return `<figure class="embedded-image"><img data-resource-id="${id}" alt="${name}"><figcaption>${name}</figcaption></figure>`
    if (r?.resourceType === 'VIDEO')
      return `<figure class="embedded-video"><video data-resource-id="${id}" controls preload="metadata"></video><figcaption>${name}</figcaption></figure>`
    return `<p><a class="embedded-resource" data-resource-id="${id}" data-resource-name="${name}">📎 ${name}</a></p>`
  })
})
function cleanup() {
  blobUrls.forEach(URL.revokeObjectURL)
  blobUrls.clear()
}
async function hydrateImages() {
  await nextTick()
  if (!articleBody.value) return
  for (const media of articleBody.value.querySelectorAll('img[data-resource-id],video[data-resource-id]')) {
    const id = Number(media.dataset.resourceId)
    try {
      const blob = await getResourceFile(id, false),
        url = URL.createObjectURL(blob)
      blobUrls.set(id, url)
      media.src = url
    } catch {
      media.setAttribute('aria-label', '媒体资源暂不可访问')
    }
  }
}
async function load() {
  loading.value = true
  error.value = ''
  cleanup()
  try {
    item.value = (await getCase(route.params.id)).data
    const ids = [...(item.value.resourceIds || [])]
    const content = item.value.content || ''
    for (const match of content.matchAll(/\[\[resource:(\d+)\|/g)) ids.push(Number(match[1]))
    const doc = new DOMParser().parseFromString(content, 'text/html')
    for (const element of doc.querySelectorAll('[data-resource-id]'))
      ids.push(Number(element.dataset.resourceId))
    const all = item.value.contentUnlocked === false ? [] : (await getResourcesByIds(ids)).data
    resourceMap.value = new Map(all.map((r) => [r.id, r]))
    recommendations.value = item.value.courseId
      ? ((await getPublishedCases({ courseId: item.value.courseId, size: 100 })).data || [])
          .filter((c) => c.id !== item.value.id)
          .slice(0, 6)
      : []
  } catch (e) {
    error.value = e?.message || '案例加载失败'
  } finally {
    loading.value = false
    hydrateImages()
  }
}
async function download(id, name) {
  try {
    const blob = await getResourceFile(id, true),
      url = URL.createObjectURL(blob),
      a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    error.value = e?.message || '资源下载失败'
  }
}
async function favorite() {
  try {
    item.value.favorite = (await toggleCaseFavorite(item.value.id)).data
  } catch (e) {
    error.value = e?.message || '收藏操作失败'
  }
}
async function unlock() {
  if (unlocking.value) return
  if (!confirm(`确定花费 ${item.value.pointsPrice} 积分解锁此案例吗？`)) return
  unlocking.value = true
  try {
    item.value = (await unlockCase(item.value.id)).data
    await hydrateImages()
  } catch (e) {
    error.value = e?.message || '解锁失败'
  } finally {
    unlocking.value = false
  }
}
function articleClick(event) {
  if (event.target.closest('button[data-unlock-case]')) {
    unlock()
    return
  }
  const link = event.target.closest('a[data-resource-id]')
  if (link) {
    event.preventDefault()
    download(Number(link.dataset.resourceId), link.dataset.resourceName || link.textContent.trim())
  }
}
const formatDate = (value) => (value ? new Date(value).toLocaleDateString('zh-CN') : '—')
watch(() => route.params.id, load, { immediate: true })
onBeforeUnmount(cleanup)
</script>
<template>
  <div class="case-reader-page">
    <button class="reader-back" @click="router.push('/app/case')"><ArrowLeft />返回案例库</button>
    <div v-if="loading" class="panel resource-empty">正在加载文章...</div>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <div v-else class="reader-layout">
      <main class="panel reader-main">
        <header class="article-header">
          <button
            v-if="item.contentUnlocked"
            class="article-favorite"
            :class="{ active: item.favorite }"
            @click="favorite"
          >
            <Heart :fill="item.favorite ? 'currentColor' : 'none'" />{{
              item.favorite ? '已收藏' : '收藏案例'
            }}
          </button>
          <div class="article-badges">
            <span>{{ visibilityName[item.visibility] }}</span
            ><span v-if="item.excellent">优秀案例</span
            ><span>{{ item.accessType === 'POINTS' ? item.pointsPrice + ' 积分' : '免费阅读' }}</span>
          </div>
          <h1>{{ item.caseName }}</h1>
          <p v-if="item.summary" class="article-lead">{{ item.summary }}</p>
          <button
            class="article-author teacher-link"
            @click="router.push({ name: 'teacher-profile', params: { id: item.creatorId } })"
          >
            <span class="author-avatar"><User /></span
            ><span
              ><strong>{{ item.creatorName }}</strong
              ><small
                ><GraduationCap />{{ item.creatorCollege || '未注明学院' }}<Calendar />发布于
                {{ formatDate(item.createTime) }}</small
              ></span
            >
          </button>
          <div class="article-tags">
            <span><BookOpen />{{ item.courseName }}</span
            ><span v-for="tag in item.knowledgePointNames" :key="tag"># {{ tag }}</span>
          </div>
        </header>
        <article
          ref="articleBody"
          class="article-content rich-article"
          v-html="renderedContent"
          @click="articleClick"
        />
        <footer v-if="item.latestReviewComment" class="article-review">
          审核意见：{{ item.latestReviewComment }}
        </footer>
      </main>
      <aside class="reader-sidebar">
        <section class="panel recommendation">
          <h2>相关推荐</h2>
          <p>与“{{ item.courseName }}”相关的案例</p>
          <button
            v-for="rec in recommendations"
            :key="rec.id"
            @click="router.push({ name: 'case-detail', params: { id: rec.id } })"
          >
            <span>{{ rec.caseName }}</span
            ><small>{{ rec.knowledgePointNames.slice(0, 2).join(' · ') || rec.courseName }}</small>
          </button>
          <div v-if="!recommendations.length">暂无相关案例</div>
        </section>
      </aside>
    </div>
  </div>
</template>
