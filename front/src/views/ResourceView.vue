<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  Download,
  Eye,
  File,
  FileText,
  Film,
  Heart,
  Image,
  Pencil,
  Plus,
  Search,
  Trash2,
  Upload,
  X,
  Wrench,
} from 'lucide-vue-next'
import {
  deleteResource,
  getMyResources,
  getResourceCategories,
  getResourceFile,
  getResourceReviewHistory,
  getResources,
  resubmitResource,
  submitResourceCorrection,
  toggleResourceFavorite,
  uploadResource,
} from '../api/resource'
import { useRoute, useRouter } from 'vue-router'
import PaginationBar from '../components/PaginationBar.vue'
import '../styles/resource-resubmit.css'

const route = useRoute(),
  router = useRouter()

const resources = ref([]),
  categories = ref([]),
  loading = ref(true),
  saving = ref(false),
  progress = ref(0)
const error = ref(''),
  notice = ref(''),
  modal = ref(null),
  current = ref(null),
  previewUrl = ref('')
const reviewHistory = ref([])
const pagination = reactive({ page: 1, size: 12, total: 0, pages: 0 })
const filters = reactive({
  keyword: '',
  type: '',
  categoryId: '',
  favoriteOnly: false,
  mineOnly: route.query.mine === '1',
})
const form = reactive({
  resourceName: '',
  description: '',
  categoryId: '',
  visibility: 'PUBLIC',
  file: null,
  content: '',
})
const user = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const canUpload = computed(() => (user.roles || []).some((r) => r === 'TEACHER' || r === 'ADMIN'))
const typeNames = { IMAGE: '图片', PDF: 'PDF', VIDEO: 'MP4 视频', DOCUMENT: '文档' }
const visibilityNames = { PRIVATE: '仅自己', PUBLIC: '公开' }
const formatSize = (size) => {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.min(Math.floor(Math.log(size) / Math.log(1024)), 3)
  return `${(size / 1024 ** i).toFixed(i ? 1 : 0)} ${units[i]}`
}
function clearMessage() {
  error.value = ''
  notice.value = ''
}
function showError(e) {
  error.value = e?.message || '操作失败'
}
async function load() {
  loading.value = true
  clearMessage()
  try {
    const params = {
      keyword: filters.keyword || undefined,
      type: filters.type || undefined,
      categoryId: filters.categoryId || undefined,
      favoriteOnly: filters.favoriteOnly,
      page: pagination.page,
      size: pagination.size,
    }
    const result = filters.mineOnly ? await getMyResources(params) : await getResources(params)
    resources.value = result.data || []
    Object.assign(pagination, {
      page: Number(result.page?.page || pagination.page),
      total: Number(result.page?.total || 0),
      pages: Number(result.page?.pages || 0),
    })
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
  if (route.query.id) {
    await nextTick()
    document
      .getElementById(`resource-${route.query.id}`)
      ?.scrollIntoView({ block: 'center', behavior: 'smooth' })
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
function resetForm() {
  Object.assign(form, {
    resourceName: '',
    description: '',
    categoryId: '',
    visibility: 'PUBLIC',
    file: null,
    content: '',
  })
}
function openUpload() {
  resetForm()
  current.value = null
  modal.value = 'upload'
}
async function openEdit(item) {
  current.value = item
  reviewHistory.value = []
  Object.assign(form, {
    resourceName: item.resourceName,
    description: item.description || '',
    categoryId: item.categoryId || '',
    visibility: ['PRIVATE', 'PUBLIC'].includes(item.visibility) ? item.visibility : 'PRIVATE',
    file: null,
  })
  modal.value = 'edit'
  try {
    reviewHistory.value = (await getResourceReviewHistory(item.id)).data || []
  } catch (e) {
    error.value = e?.message || '审核记录加载失败'
  }
}
function openCorrection(item) {
  current.value = item
  form.content = ''
  modal.value = 'correction'
}
function closeModal() {
  modal.value = null
  current.value = null
  reviewHistory.value = []
  progress.value = 0
}
function chooseFile(event) {
  form.file = event.target.files[0]
  if (form.file && !form.resourceName) form.resourceName = form.file.name
}
async function save() {
  saving.value = true
  clearMessage()
  try {
    const action = modal.value
    if (modal.value === 'upload') {
      if (!form.file) throw new Error('请选择文件')
      const data = new FormData()
      data.append('file', form.file)
      data.append('resourceName', form.resourceName)
      data.append('description', form.description)
      data.append('visibility', form.visibility)
      if (form.categoryId) data.append('categoryId', form.categoryId)
      await uploadResource(data, (e) => {
        if (e.total) progress.value = Math.round((e.loaded * 100) / e.total)
      })
    } else if (modal.value === 'edit')
      await resubmitResource(
        current.value.id,
        {
          resourceName: form.resourceName,
          description: form.description,
          categoryId: form.categoryId || null,
          visibility: form.visibility,
        },
        form.file,
      )
    else await submitResourceCorrection(current.value.id, form.content)
    closeModal()
    await load()
    notice.value =
      action === 'correction'
        ? '纠错已提交'
        : form.visibility === 'PRIVATE'
          ? '私有资源已保存，无需审核'
          : '共享资源已提交审核，通过后其他符合可见范围的用户才能查看'
  } catch (e) {
    showError(e)
  } finally {
    saving.value = false
  }
}
async function favorite(item) {
  try {
    item.favorite = (await toggleResourceFavorite(item.id)).data
  } catch (e) {
    showError(e)
  }
}
async function remove(item) {
  if (!confirm(`确定删除资源“${item.resourceName}”吗？`)) return
  try {
    await deleteResource(item.id)
    await load()
    notice.value = '资源已删除'
  } catch (e) {
    showError(e)
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
      current.value = item
      modal.value = 'preview'
    }
  } catch (e) {
    showError(e)
  }
}
const iconFor = (type) => ({ IMAGE: Image, PDF: FileText, VIDEO: Film, DOCUMENT: File })[type] || File
onMounted(async () => {
  try {
    categories.value = (await getResourceCategories()).data || []
  } catch (e) {
    showError(e)
  }
  await load()
  if (route.query.action === 'upload' && canUpload.value) {
    openUpload()
    router.replace({ path: route.path, query: { ...route.query, action: undefined } })
  }
})
onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <div class="page resource-page">
    <header class="page-title-row">
      <div>
        <h1>教学资源库</h1>
        <p>统一管理图片、PDF、视频和教学文档。</p>
      </div>
      <button v-if="canUpload" class="button primary" @click="openUpload">
        <Upload :size="17" />上传资源
      </button>
    </header>
    <section class="panel resource-filters">
      <label
        ><Search /><input
          v-model="filters.keyword"
          placeholder="搜索资源名称或简介"
          @keyup.enter="search" /></label
      ><select v-model="filters.type" @change="search">
        <option value="">全部类型</option>
        <option v-for="(name, key) in typeNames" :key="key" :value="key">{{ name }}</option></select
      ><select v-model="filters.categoryId" @change="search">
        <option value="">全部分类</option>
        <option v-for="item in categories" :key="item.id" :value="item.id">
          {{ item.categoryName }}
        </option></select
      ><label class="favorite-filter"
        ><input v-model="filters.favoriteOnly" type="checkbox" @change="search" />只看收藏</label
      ><label class="favorite-filter"
        ><input v-model="filters.mineOnly" type="checkbox" @change="search" />只看我的</label
      ><button class="button secondary" @click="search">检索</button>
    </section>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="form-success">{{ notice }}</p>
    <div v-if="loading" class="panel resource-empty">正在加载资源...</div>
    <div v-else-if="!resources.length" class="panel resource-empty">没有找到符合条件的资源。</div>
    <section v-else class="resource-grid">
      <article
        v-for="item in resources"
        :id="`resource-${item.id}`"
        :key="item.id"
        class="panel resource-card"
        :class="{ 'resource-highlight': String(route.query.id) === String(item.id) }"
      >
        <div class="resource-type-icon" :class="item.resourceType.toLowerCase()">
          <component :is="iconFor(item.resourceType)" />
        </div>
        <div class="resource-card-copy">
          <div>
            <span class="resource-tag">{{ typeNames[item.resourceType] || item.resourceType }}</span
            ><span class="audit-tag" :class="item.auditStatus.toLowerCase()">{{
              item.visibility === 'PRIVATE'
                ? '无需审核'
                : item.auditStatus === 'APPROVED'
                  ? '已通过'
                  : item.auditStatus === 'REJECTED'
                    ? '未通过'
                    : '待审核'
            }}</span>
          </div>
          <h3>{{ item.resourceName }}</h3>
          <p>{{ item.description || '暂无资源简介' }}</p>
          <small
            >{{ formatSize(item.fileSize) }} · {{ item.categoryName || '未分类' }} ·
            {{ visibilityNames[item.visibility] }}</small
          >
          <p
            v-if="item.auditStatus === 'REJECTED' && item.latestReviewComment"
            class="resource-rejection-note"
          >
            审核意见：{{ item.latestReviewComment }}
          </p>
        </div>
        <div class="resource-actions">
          <button :class="{ active: item.favorite }" title="收藏" @click="favorite(item)">
            <Heart :fill="item.favorite ? 'currentColor' : 'none'" /></button
          ><button v-if="item.previewable" title="预览" @click="fileAction(item, false)"><Eye /></button
          ><button title="下载" @click="fileAction(item, true)"><Download /></button
          ><button v-if="item.uploaderId !== user.id" title="纠错" @click="openCorrection(item)">
            <Wrench /></button
          ><button
            v-if="
              canUpload &&
              (item.uploaderId === user.id || user.roles?.includes('ADMIN')) &&
              !(item.auditStatus === 'PENDING' && item.visibility === 'PUBLIC')
            "
            :title="item.auditStatus === 'REJECTED' ? '修改并重新提交' : '编辑资源'"
            @click="openEdit(item)"
          >
            <Pencil /></button
          ><button
            v-if="canUpload && (item.uploaderId === user.id || user.roles?.includes('ADMIN'))"
            class="danger-action"
            title="删除"
            @click="remove(item)"
          >
            <Trash2 />
          </button>
        </div>
        <button
          v-if="item.auditStatus === 'REJECTED' && item.uploaderId === user.id"
          class="resource-resubmit-entry"
          @click="openEdit(item)"
        >
          修改并重新提交
        </button>
      </article>
    </section>
    <PaginationBar
      :page="pagination.page"
      :pages="pagination.pages"
      :total="pagination.total"
      @change="changePage"
    />

    <div
      v-if="modal"
      class="modal-mask"
      :class="{ 'resource-dialog-mask': modal === 'edit' }"
      @click.self="closeModal"
    >
      <section v-if="modal === 'preview'" class="modal-card preview-card">
        <header>
          <h2>{{ current.resourceName }}</h2>
          <button @click="closeModal"><X /></button>
        </header>
        <img v-if="current.resourceType === 'IMAGE'" :src="previewUrl" /><video
          v-else-if="current.resourceType === 'VIDEO'"
          :src="previewUrl"
          controls
        /><iframe v-else :src="previewUrl" />
      </section>
      <form
        v-else
        class="modal-card resource-edit-dialog"
        :class="{ 'resubmit-dialog': modal === 'edit' }"
        @submit.prevent="save"
      >
        <header>
          <h2>
            {{
              modal === 'upload'
                ? '上传资源'
                : modal === 'edit'
                  ? current.auditStatus === 'REJECTED'
                    ? '修改并重新提交'
                    : '编辑资源'
                  : '提交纠错'
            }}
          </h2>
          <button type="button" @click="closeModal"><X /></button>
        </header>
        <div class="resource-edit-scroll">
          <div v-if="modal === 'edit' && form.visibility !== 'PRIVATE'" class="resource-review-warning">
            保存后资源将进入待审核状态；审核通过前，其他教师无法查看或下载。
          </div>
          <div v-if="modal === 'edit' && current.latestReviewComment" class="resource-latest-review">
            最近审核意见：{{ current.latestReviewComment }}
          </div>
          <div class="modal-fields">
            <template v-if="modal !== 'correction'"
              ><label class="wide file-picker"
                ><input
                  type="file"
                  :required="modal === 'upload'"
                  accept=".jpg,.jpeg,.png,.gif,.webp,.pdf,.mp4,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,.7z"
                  @change="chooseFile"
                /><Upload />{{
                  form.file?.name ||
                  (modal === 'edit' ? '选择替换文件（可选，不选则保留原文件）' : '选择文件（最大200MB）')
                }}</label
              ><label>资源名称<input v-model="form.resourceName" required maxlength="200" /></label
              ><label
                >资源分类<select v-model="form.categoryId">
                  <option value="">未分类</option>
                  <option v-for="item in categories" :key="item.id" :value="item.id">
                    {{ item.categoryName }}
                  </option>
                </select></label
              ><label
                >可见范围<select v-model="form.visibility">
                  <option value="PRIVATE">仅自己</option>
                  <option value="PUBLIC">公开</option>
                </select></label
              ><label class="wide">资源简介<textarea v-model="form.description" maxlength="1000" /></label>
              <div v-if="saving && modal === 'upload'" class="wide upload-progress">
                <i :style="{ width: progress + '%' }" /><span>{{ progress }}%</span>
              </div></template
            ><label v-else class="wide"
              >问题描述<textarea
                v-model="form.content"
                required
                minlength="20"
                maxlength="2000"
                placeholder="请至少用20个字说明具体位置、错误内容和建议修改方式"
              />
            </label>
          </div>
          <section v-if="modal === 'edit' && reviewHistory.length" class="resource-review-history">
            <h3>历次审核记录</h3>
            <ol>
              <li v-for="record in reviewHistory" :key="record.submissionNo">
                <strong>{{ record.result === 'APPROVED' ? '审核通过' : '审核未通过' }}</strong
                ><span
                  >第 {{ record.submissionNo }} 次 · {{ record.reviewerName }} ·
                  {{ record.reviewedAt ? new Date(record.reviewedAt).toLocaleString('zh-CN') : '' }}</span
                >
                <p v-if="record.comment">{{ record.comment }}</p>
              </li>
            </ol>
          </section>
          <p v-if="error" class="form-error">{{ error }}</p>
        </div>
        <footer>
          <button type="button" class="button secondary" @click="closeModal">取消</button
          ><button class="button primary" :disabled="saving">
            {{
              saving
                ? '处理中...'
                : modal === 'edit' && form.visibility !== 'PRIVATE'
                  ? '保存并提交审核'
                  : '确认'
            }}
          </button>
        </footer>
      </form>
    </div>
  </div>
</template>
