<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  BookOpen,
  Download,
  Eye,
  File,
  FileText,
  Film,
  Heart,
  Image,
  LibraryBig,
  Search,
  X,
} from 'lucide-vue-next'
import { getCases, toggleCaseFavorite } from '../api/case'
import { getResourceFile, getResources, toggleResourceFavorite } from '../api/resource'

const router = useRouter(),
  active = ref('resource'),
  resources = ref([]),
  cases = ref([]),
  loading = ref(true),
  error = ref(''),
  keyword = ref(''),
  tag = ref('全部')
const preview = ref(null),
  previewUrl = ref('')
const typeNames = { IMAGE: '图片', PDF: 'PDF', VIDEO: '视频', DOCUMENT: '文档' }
const resourceTags = computed(() => [
  '全部',
  ...new Set(resources.value.map((item) => item.categoryName || typeNames[item.resourceType] || '未分类')),
])
const caseTags = computed(() => [
  '全部',
  ...new Set(
    cases.value.flatMap((item) => [item.courseName || '未指定课程', ...(item.knowledgePointNames || [])]),
  ),
])
const tags = computed(() => (active.value === 'resource' ? resourceTags.value : caseTags.value))
const visibleResources = computed(() =>
  resources.value.filter(
    (item) =>
      (tag.value === '全部' ||
        (item.categoryName || typeNames[item.resourceType] || '未分类') === tag.value) &&
      (!keyword.value ||
        `${item.resourceName} ${item.description || ''}`.toLowerCase().includes(keyword.value.toLowerCase())),
  ),
)
const visibleCases = computed(() =>
  cases.value.filter(
    (item) =>
      (tag.value === '全部' ||
        item.courseName === tag.value ||
        (item.knowledgePointNames || []).includes(tag.value)) &&
      (!keyword.value ||
        `${item.caseName} ${item.summary || ''}`.toLowerCase().includes(keyword.value.toLowerCase())),
  ),
)
const iconFor = (type) => ({ IMAGE: Image, PDF: FileText, VIDEO: Film, DOCUMENT: File })[type] || File
const formatSize = (size) => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB'],
    i = Math.min(Math.floor(Math.log(size) / Math.log(1024)), 3)
  return `${(size / 1024 ** i).toFixed(i ? 1 : 0)} ${units[i]}`
}
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [r, c] = await Promise.all([
      getResources({ favoriteOnly: true, size: 100 }),
      getCases({ favoriteOnly: true, size: 100 }),
    ])
    resources.value = r.data || []
    cases.value = c.data || []
  } catch (e) {
    error.value = e?.message || '收藏内容加载失败'
  } finally {
    loading.value = false
  }
}
function changeTab(value) {
  active.value = value
  tag.value = '全部'
  keyword.value = ''
}
async function removeResource(item) {
  try {
    await toggleResourceFavorite(item.id)
    resources.value = resources.value.filter((r) => r.id !== item.id)
  } catch (e) {
    error.value = e?.message || '取消收藏失败'
  }
}
async function removeCase(item) {
  try {
    await toggleCaseFavorite(item.id)
    cases.value = cases.value.filter((c) => c.id !== item.id)
  } catch (e) {
    error.value = e?.message || '取消收藏失败'
  }
}
async function fileAction(item, download) {
  try {
    const blob = await getResourceFile(item.id, download),
      url = URL.createObjectURL(blob)
    if (download) {
      const a = document.createElement('a')
      a.href = url
      a.download = item.resourceName
      a.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } else {
      if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
      previewUrl.value = url
      preview.value = item
    }
  } catch (e) {
    error.value = e?.message || '文件打开失败'
  }
}
function closePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  preview.value = null
}
onMounted(load)
onBeforeUnmount(closePreview)
</script>

<template>
  <div class="page favorites-page">
    <header class="page-title-row">
      <div>
        <h1>个人收藏</h1>
        <p>将收藏的教学资源与案例分开管理，并按标签快速归类。</p>
      </div>
      <span class="favorite-total"><Heart />共 {{ resources.length + cases.length }} 项收藏</span>
    </header>
    <section class="panel favorite-toolbar">
      <nav>
        <button :class="{ active: active === 'resource' }" @click="changeTab('resource')">
          <LibraryBig />收藏的资源 <b>{{ resources.length }}</b></button
        ><button :class="{ active: active === 'case' }" @click="changeTab('case')">
          <BookOpen />收藏的案例 <b>{{ cases.length }}</b>
        </button>
      </nav>
      <label
        ><Search /><input
          v-model="keyword"
          :placeholder="active === 'resource' ? '搜索收藏资源' : '搜索收藏案例'"
      /></label>
    </section>
    <div class="favorite-tags">
      <span>标签分类</span
      ><button v-for="item in tags" :key="item" :class="{ active: tag === item }" @click="tag = item">
        {{ item }}
      </button>
    </div>
    <p v-if="error" class="form-error">{{ error }}</p>
    <div v-if="loading" class="panel resource-empty">正在整理个人收藏...</div>
    <template v-else-if="active === 'resource'"
      ><div v-if="!visibleResources.length" class="panel favorite-empty">
        <Heart />
        <h2>暂无符合条件的资源收藏</h2>
        <p>前往教学资源库，点击心形按钮即可收藏。</p>
        <button class="button primary" @click="router.push('/app/resource')">浏览教学资源</button>
      </div>
      <section v-else class="favorite-card-grid">
        <article v-for="item in visibleResources" :key="item.id" class="panel favorite-card">
          <span class="favorite-card-icon" :class="item.resourceType.toLowerCase()"
            ><component :is="iconFor(item.resourceType)"
          /></span>
          <div>
            <small
              >{{ item.categoryName || typeNames[item.resourceType] || '未分类' }} ·
              {{ typeNames[item.resourceType] }}</small
            >
            <h2>{{ item.resourceName }}</h2>
            <p>{{ item.description || '暂无资源简介' }}</p>
            <em>{{ formatSize(item.fileSize) }}</em>
          </div>
          <footer>
            <button class="unfavorite" @click="removeResource(item)">
              <Heart fill="currentColor" />取消收藏</button
            ><button v-if="item.previewable" @click="fileAction(item, false)"><Eye />预览</button
            ><button @click="fileAction(item, true)"><Download />下载</button>
          </footer>
        </article>
      </section></template
    >
    <template v-else
      ><div v-if="!visibleCases.length" class="panel favorite-empty">
        <Heart />
        <h2>暂无符合条件的案例收藏</h2>
        <p>前往教学案例库，收藏值得参考的教学实践。</p>
        <button class="button primary" @click="router.push('/app/case')">浏览教学案例</button>
      </div>
      <section v-else class="favorite-case-list">
        <article v-for="item in visibleCases" :key="item.id" class="panel favorite-case">
          <header>
            <span><BookOpen />{{ item.courseName || '未指定课程' }}</span
            ><button @click="removeCase(item)"><Heart fill="currentColor" />取消收藏</button>
          </header>
          <h2 @click="router.push(`/app/case/${item.id}`)">{{ item.caseName }}</h2>
          <p>{{ item.summary || '暂无案例摘要' }}</p>
          <div>
            <span v-for="point in item.knowledgePointNames" :key="point"># {{ point }}</span>
          </div>
          <footer>
            <small>{{ item.creatorName }} · 引用 {{ item.resourceNames.length }} 份资源</small
            ><button @click="router.push(`/app/case/${item.id}`)">阅读全文</button>
          </footer>
        </article>
      </section></template
    >
    <div v-if="preview" class="modal-mask" @click.self="closePreview">
      <section class="modal-card preview-card">
        <header>
          <h2>{{ preview.resourceName }}</h2>
          <button @click="closePreview"><X /></button>
        </header>
        <img v-if="preview.resourceType === 'IMAGE'" :src="previewUrl" /><video
          v-else-if="preview.resourceType === 'VIDEO'"
          :src="previewUrl"
          controls
        ></video
        ><iframe v-else :src="previewUrl" />
      </section>
    </div>
  </div>
</template>
