<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BadgeCheck, BookOpen, Eye, FileText, RefreshCw, Wrench, X } from 'lucide-vue-next'
import { getPendingReviews, reviewResource } from '../api/review'
import { reviewCase } from '../api/case'
import { getPendingResourceCorrections, getResourceFile, handleResourceCorrection } from '../api/resource'

const router = useRouter(),
  items = ref([]),
  tab = ref('RESOURCE'),
  loading = ref(true),
  saving = ref(false)
const error = ref(''),
  notice = ref(''),
  selected = ref(null),
  result = ref('APPROVED'),
  comment = ref(''),
  excellent = ref(false)
const preview = ref(null),
  previewUrl = ref('')
const me = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const visible = computed(() => items.value.filter((item) => item.kind === tab.value))
const count = (kind) => items.value.filter((item) => item.kind === kind).length
const scope = { PUBLIC: '公开' }
const time = (value) => (value ? new Date(value).toLocaleString('zh-CN') : '')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [reviews, corrections] = await Promise.all([getPendingReviews(), getPendingResourceCorrections()])
    items.value = [
      ...(reviews.data || []),
      ...(corrections.data || []).map((item) => ({
        ...item,
        kind: 'CORRECTION',
        title: item.resourceName,
        summary: item.content,
        author: item.submitterName,
        authorId: item.submitterId,
        submittedAt: item.createTime,
      })),
    ]
  } catch (e) {
    error.value = e?.message || '审核任务加载失败'
  } finally {
    loading.value = false
  }
}
function openReview(item) {
  selected.value = item
  result.value = item.kind === 'CORRECTION' ? 'ACCEPTED' : 'APPROVED'
  comment.value = ''
  excellent.value = false
  error.value = ''
}
function openCase(item) {
  window.open(
    router.resolve({ name: 'case-detail', params: { id: item.id } }).href,
    '_blank',
    'noopener,noreferrer',
  )
}
const fileResourceId = (item) => (item.kind === 'CORRECTION' ? item.resourceId : item.id)
async function openResource(item) {
  error.value = ''
  try {
    const blob = await getResourceFile(fileResourceId(item), false)
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = URL.createObjectURL(blob)
    preview.value = item
  } catch (e) {
    error.value = e?.message || '资源预览失败'
  }
}
async function downloadResource(item) {
  try {
    const blob = await getResourceFile(fileResourceId(item), true),
      url = URL.createObjectURL(blob),
      link = document.createElement('a')
    link.href = url
    link.download = item.title
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } catch (e) {
    error.value = e?.message || '资源下载失败'
  }
}
function closePreview() {
  preview.value = null
  if (previewUrl.value) {
    URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
  }
}
async function submit() {
  if (!selected.value || saving.value) return
  if ((selected.value.kind === 'CORRECTION' || result.value === 'REJECTED') && !comment.value.trim()) {
    error.value =
      selected.value.kind === 'CORRECTION' ? '处理纠错时必须填写处理说明' : '驳回时必须填写审核意见'
    return
  }
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const data = { result: result.value, comment: comment.value.trim() }
    if (selected.value.kind === 'RESOURCE') await reviewResource(selected.value.id, data)
    else if (selected.value.kind === 'CASE')
      await reviewCase(selected.value.id, { ...data, excellent: excellent.value })
    else await handleResourceCorrection(selected.value.id, data)
    notice.value =
      selected.value.kind === 'CORRECTION'
        ? `纠错已${result.value === 'ACCEPTED' ? '确认属实，上传者会收到修改通知' : '驳回，提交者会收到处理结果'}。`
        : `${selected.value.kind === 'RESOURCE' ? '资源' : '案例'}已${result.value === 'APPROVED' ? '通过' : '驳回'}，作者会收到审核通知。`
    selected.value = null
    await load()
  } catch (e) {
    error.value = e?.message || '处理失败'
  } finally {
    saving.value = false
  }
}
onMounted(load)
onBeforeUnmount(closePreview)
</script>

<template>
  <div class="page review-page">
    <header class="page-title-row">
      <div>
        <h1>审核中心</h1>
        <p>审核共享资源和案例，并核实用户提交的资源纠错。</p>
      </div>
      <button class="button secondary" @click="load"><RefreshCw />刷新待办</button>
    </header>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="form-success">{{ notice }}</p>
    <nav class="review-tabs">
      <button :class="{ active: tab === 'RESOURCE' }" @click="tab = 'RESOURCE'">
        <FileText />待审核资源 <b>{{ count('RESOURCE') }}</b>
      </button>
      <button :class="{ active: tab === 'CASE' }" @click="tab = 'CASE'">
        <BookOpen />待审核案例 <b>{{ count('CASE') }}</b>
      </button>
      <button :class="{ active: tab === 'CORRECTION' }" @click="tab = 'CORRECTION'">
        <Wrench />资源纠错 <b>{{ count('CORRECTION') }}</b>
      </button>
    </nav>
    <div v-if="loading" class="panel resource-empty">正在加载审核任务...</div>
    <div v-else-if="!visible.length" class="panel resource-empty">
      当前没有待处理的{{ tab === 'RESOURCE' ? '资源' : tab === 'CASE' ? '案例' : '资源纠错' }}。
    </div>
    <div v-else class="review-list">
      <article v-for="item in visible" :key="item.kind + item.id" class="panel review-item">
        <div>
          <span class="review-kind">{{
            item.kind === 'CORRECTION'
              ? '资源纠错'
              : item.kind === 'RESOURCE'
                ? item.resourceType || '资源'
                : '教学案例'
          }}</span
          ><span class="review-scope">{{
            item.kind === 'CORRECTION' ? '等待核实' : scope[item.visibility] || item.visibility
          }}</span>
        </div>
        <h2>{{ item.title }}</h2>
        <p>{{ item.summary || '暂无简介' }}</p>
        <footer>
          <small
            >{{ item.kind === 'CORRECTION' ? '纠错人' : '提交人' }}：{{ item.author }} ·
            {{ time(item.submittedAt) }}</small
          ><span
            ><button v-if="item.kind === 'CASE'" @click="openCase(item)"><Eye />查看案例</button
            ><button v-else-if="item.resourceType === 'DOCUMENT'" @click="downloadResource(item)">
              <Eye />下载查看</button
            ><button v-else @click="openResource(item)"><Eye />预览资源</button
            ><button
              class="review-action"
              :disabled="item.authorId === me.id"
              :title="item.authorId === me.id ? '不能处理自己提交的内容' : ''"
              @click="openReview(item)"
            >
              <BadgeCheck />{{
                item.authorId === me.id ? '本人提交' : item.kind === 'CORRECTION' ? '核实纠错' : '开始审核'
              }}
            </button></span
          >
        </footer>
      </article>
    </div>
    <div v-if="selected" class="modal-mask" @click.self="selected = null">
      <form class="modal-card review-dialog" @submit.prevent="submit">
        <header>
          <h2>
            {{
              selected.kind === 'CORRECTION'
                ? '处理纠错'
                : '审核' + (selected.kind === 'RESOURCE' ? '资源' : '案例')
            }}：{{ selected.title }}
          </h2>
          <button type="button" @click="selected = null"><X /></button>
        </header>
        <div class="modal-fields">
          <label
            >{{ selected.kind === 'CORRECTION' ? '核实结果' : '审核结果'
            }}<select v-model="result">
              <template v-if="selected.kind === 'CORRECTION'"
                ><option value="ACCEPTED">确认属实</option>
                <option value="REJECTED">问题不成立</option></template
              ><template v-else
                ><option value="APPROVED">通过</option>
                <option value="REJECTED">驳回</option></template
              >
            </select></label
          ><label v-if="selected.kind === 'CASE' && result === 'APPROVED'" class="check-label"
            ><input v-model="excellent" type="checkbox" />标记为优秀案例</label
          ><label class="wide"
            >{{ selected.kind === 'CORRECTION' ? '处理说明' : '审核意见'
            }}<textarea
              v-model="comment"
              :required="selected.kind === 'CORRECTION' || result === 'REJECTED'"
              maxlength="1000"
              :placeholder="
                selected.kind === 'CORRECTION'
                  ? '请说明核实依据及后续处理要求'
                  : result === 'REJECTED'
                    ? '请说明驳回原因'
                    : '可填写通过意见'
              "
            ></textarea>
          </label>
        </div>
        <footer>
          <button type="button" class="button secondary" @click="selected = null">取消</button
          ><button class="button primary" :disabled="saving">
            {{ saving ? '提交中...' : selected.kind === 'CORRECTION' ? '确认处理' : '确认审核' }}
          </button>
        </footer>
      </form>
    </div>
    <div v-if="preview" class="modal-mask" @click.self="closePreview">
      <section class="modal-card preview-card">
        <header>
          <h2>{{ preview.title }}</h2>
          <button @click="closePreview"><X /></button>
        </header>
        <img v-if="preview.resourceType === 'IMAGE'" :src="previewUrl" alt="资源预览" /><video
          v-else-if="preview.resourceType === 'VIDEO'"
          :src="previewUrl"
          controls
        /><iframe v-else :src="previewUrl" title="PDF资源预览" />
      </section>
    </div>
  </div>
</template>
