<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { FilePenLine, Plus, Search, Trash2, Eye } from 'lucide-vue-next'
import { deleteCase, getMyCases } from '../api/case'
import '../styles/my-cases.css'

const router = useRouter(),
  route = useRoute()
const cases = ref([]),
  loading = ref(true),
  error = ref(''),
  keyword = ref(''),
  tab = ref(route.query.tab === 'draft' ? 'draft' : 'all')
const groups = [
  { key: 'all', label: '全部' },
  { key: 'draft', label: '草稿箱' },
  { key: 'pending', label: '待审核' },
  { key: 'rejected', label: '已驳回' },
  { key: 'published', label: '已发布' },
]
function category(item) {
  if (item.visibility !== 'PUBLIC') return 'draft'
  return (
    { DRAFT: 'draft', PENDING: 'pending', REJECTED: 'rejected', APPROVED: 'published' }[item.auditStatus] ||
    'draft'
  )
}
const counts = computed(() =>
  Object.fromEntries(
    groups.map((g) => [
      g.key,
      g.key === 'all' ? cases.value.length : cases.value.filter((c) => category(c) === g.key).length,
    ]),
  ),
)
const visible = computed(() =>
  cases.value.filter(
    (c) =>
      (tab.value === 'all' || category(c) === tab.value) &&
      (!keyword.value ||
        `${c.caseName} ${c.courseName || ''} ${c.summary || ''}`
          .toLowerCase()
          .includes(keyword.value.toLowerCase())),
  ),
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    cases.value = (await getMyCases()).data || []
  } catch (e) {
    error.value = e?.message || '加载失败'
  } finally {
    loading.value = false
  }
}
async function remove(item) {
  if (!confirm(`确定删除“${item.caseName || '未命名草稿'}”吗？删除后无法恢复。`)) return
  try {
    await deleteCase(item.id)
    await load()
  } catch (e) {
    error.value = e?.message || '删除失败'
  }
}
function open(item) {
  router.push({
    name: ['draft', 'rejected'].includes(category(item)) ? 'case-edit' : 'case-detail',
    params: { id: item.id },
  })
}
onMounted(load)
</script>

<template>
  <div class="page my-cases-page">
    <header class="page-title-row">
      <div>
        <h1>我的案例</h1>
        <p>未完成的内容留在草稿箱；选择草稿继续编辑并提交审核，通过后才会出现在共享案例库中。</p>
      </div>
      <button class="button primary" @click="router.push({ name: 'case-new' })">
        <Plus :size="18" />新建案例
      </button>
    </header>
    <div class="my-case-tabs">
      <button
        v-for="group in groups"
        :key="group.key"
        :class="{ active: tab === group.key }"
        @click="tab = group.key"
      >
        {{ group.label }} <span>{{ counts[group.key] }}</span>
      </button>
    </div>
    <label class="my-case-search"
      ><Search :size="18" /><input v-model="keyword" placeholder="搜索我的案例或课程"
    /></label>
    <p v-if="error" class="form-error">{{ error }}</p>
    <div v-if="loading" class="panel resource-empty">正在加载我的案例...</div>
    <div v-else-if="!visible.length" class="panel resource-empty">
      {{ tab === 'draft' ? '草稿箱还没有案例，点击“新建案例”开始创作。' : '这里暂时没有案例。' }}
    </div>
    <div v-else class="my-case-list">
      <article v-for="item in visible" :key="item.id" class="panel my-case-row">
        <div class="my-case-main">
          <div class="my-case-meta">
            <span class="my-case-badge" :class="category(item)">{{
              item.visibility !== 'PUBLIC'
                ? '仅自己可见'
                : groups.find((g) => g.key === category(item))?.label
            }}</span
            ><span>{{ item.courseName || '未选课程' }}</span
            ><span v-if="item.updateTime">{{ new Date(item.updateTime).toLocaleString('zh-CN') }}</span>
          </div>
          <h2>{{ item.caseName || '未命名草稿' }}</h2>
          <p>
            {{ item.summary || (item.content || '').replace(/<[^>]*>/g, '').slice(0, 120) || '正文待填写' }}
          </p>
          <small v-if="item.auditStatus === 'REJECTED' && item.latestReviewComment"
            >审核意见：{{ item.latestReviewComment }}</small
          ><small v-if="item.visibility !== 'PUBLIC'">旧可见范围已停用；编辑后可作为草稿提交公开审核。</small>
        </div>
        <div class="my-case-actions">
          <button class="button secondary" @click="open(item)">
            <FilePenLine v-if="['draft', 'rejected'].includes(category(item))" :size="16" /><Eye
              v-else
              :size="16"
            />{{ ['draft', 'rejected'].includes(category(item)) ? '继续编辑' : '查看' }}</button
          ><button class="my-case-delete" title="删除案例" @click="remove(item)">
            <Trash2 :size="18" />
          </button>
        </div>
      </article>
    </div>
  </div>
</template>
