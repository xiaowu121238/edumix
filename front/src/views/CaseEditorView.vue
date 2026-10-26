<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CheckCircle2, ChevronDown, FileText, Hash, Save, Send, Settings2 } from 'lucide-vue-next'
import CaseRichEditor from '../components/CaseRichEditor.vue'
import { createCaseDraft, getCase, submitCase, updateCaseDraft } from '../api/case'
import { getCourses, getCourseTree } from '../api/course'
import { getResourcesByIds } from '../api/resource'

const route = useRoute(),
  router = useRouter(),
  courses = ref([]),
  points = ref([]),
  resources = ref([]),
  loading = ref(true),
  saving = ref(false),
  settingsOpen = ref(true),
  error = ref(''),
  notice = ref(''),
  saveState = ref('待编辑')
const form = reactive({
  caseName: '',
  courseId: '',
  knowledgePointIds: [],
  summary: '',
  content: '',
  resourceIds: [],
  visibility: 'PUBLIC',
  accessType: 'FREE',
  pointsPrice: 0,
})
const editing = computed(() => Boolean(route.params.id)),
  wordCount = computed(() => form.content.replace(/<[^>]*>/g, '').replace(/\s/g, '').length)
const user = JSON.parse(localStorage.getItem('edumix-user') || '{}')
let draftId = route.params.id || null,
  timer = null,
  request = null,
  lastSaved = '',
  initialized = false,
  blocked = false,
  legacyPrivate = false
const backupKey = () => `edumix-case-draft:${user.id || 'unknown'}:${draftId || 'new'}`
const headings = computed(() => {
  const doc = new DOMParser().parseFromString(form.content, 'text/html')
  return [...doc.querySelectorAll('h1,h2,h3')].map((h, i) => ({
    id: i,
    text: h.textContent || `标题 ${i + 1}`,
  }))
})
function legacyContent(item) {
  return [
    item.teachingObjective && `<h2>教学目标</h2><p>${item.teachingObjective}</p>`,
    item.keyPoints && `<h2>教学重点</h2><p>${item.keyPoints}</p>`,
    item.difficultPoints && `<h2>教学难点</h2><p>${item.difficultPoints}</p>`,
    item.teachingProcess && `<h2>实施过程</h2><p>${item.teachingProcess}</p>`,
    item.teachingReflection && `<h2>教学反思</h2><p>${item.teachingReflection}</p>`,
  ]
    .filter(Boolean)
    .join('')
}
async function loadPoints(reset = true) {
  if (reset) form.knowledgePointIds = []
  points.value = []
  if (!form.courseId) return
  const tree = (await getCourseTree(form.courseId)).data
  points.value = (tree?.chapters || []).flatMap((c) =>
    c.knowledgePoints.map((p) => ({ ...p, chapterName: c.chapterName })),
  )
}
function addResource(resource) {
  resources.value.unshift(resource)
}
function snapshot() {
  return JSON.stringify({
    ...form,
    courseId: form.courseId || null,
    pointsPrice: form.accessType === 'POINTS' ? Number(form.pointsPrice) || 0 : 0,
  })
}
function localBackup() {
  localStorage.setItem(backupKey(), snapshot())
}
function changed() {
  if (!initialized || blocked) return
  localBackup()
  saveState.value = '待保存'
  clearTimeout(timer)
  timer = setTimeout(() => persist(false, true), 1200)
}
watch(form, changed, { deep: true })
async function saveCurrent() {
  if (request) await request
  const serialized = snapshot()
  if (draftId && serialized === lastSaved) return draftId
  const oldKey = backupKey(),
    payload = JSON.parse(serialized)
  saving.value = true
  saveState.value = '保存中...'
  request = draftId ? updateCaseDraft(draftId, payload) : createCaseDraft(payload)
  try {
    const result = await request
    draftId = result.data.id
    lastSaved = serialized
    localStorage.removeItem(oldKey)
    localStorage.removeItem('edumix-ai-draft')
    if (snapshot() !== serialized) localBackup()
    saveState.value = snapshot() === serialized ? '已保存到草稿箱' : '待保存'
    if (!editing.value) await router.replace({ name: 'case-edit', params: { id: draftId } })
    return draftId
  } finally {
    request = null
    saving.value = false
  }
}
async function persist(submitAfter = false, automatic = false) {
  if (blocked) return
  clearTimeout(timer)
  error.value = ''
  if (!automatic) notice.value = ''
  try {
    if (submitAfter) {
      if (!form.caseName.trim()) throw new Error('请输入案例标题')
      if (!form.courseId) throw new Error('请选择所属课程')
      if (!form.knowledgePointIds.length) throw new Error('请至少选择一个知识点')
      if (
        !new DOMParser().parseFromString(form.content, 'text/html').body.textContent?.trim() &&
        !form.content.includes('data-resource-id')
      )
        throw new Error('请填写正文')
      if (
        form.accessType === 'POINTS' &&
        (!Number.isInteger(Number(form.pointsPrice)) ||
          Number(form.pointsPrice) < 1 ||
          Number(form.pointsPrice) > 3)
      )
        throw new Error('积分案例定价须为1到3分')
    }
    const id = await saveCurrent()
    if (submitAfter) {
      await submitCase(id)
      await router.push({ name: 'my-cases' })
      return
    }
    if (!automatic) notice.value = '草稿已保存，可以在“我的案例”继续编辑'
    if (snapshot() !== lastSaved) {
      clearTimeout(timer)
      timer = setTimeout(() => persist(false, true), 1200)
    }
  } catch (e) {
    saveState.value = '保存失败'
    error.value = e?.message || '保存失败，本地备份仍在浏览器中'
    localBackup()
  }
}
onMounted(async () => {
  try {
    courses.value = (await getCourses()).data || []
    if (editing.value) {
      const item = (await getCase(route.params.id)).data
      if (
        item.visibility === 'PUBLIC' &&
        (item.auditStatus === 'PENDING' || item.auditStatus === 'APPROVED')
      ) {
        blocked = true
        error.value = '审核中或已发布的案例不能直接编辑，请在“我的案例”查看'
        return
      }
      Object.assign(form, {
        caseName: item.caseName || '',
        courseId: item.courseId || '',
        knowledgePointIds: [...(item.knowledgePointIds || [])],
        summary: item.summary || '',
        content: item.content || legacyContent(item),
        resourceIds: [...(item.resourceIds || [])],
        visibility: 'PUBLIC',
        accessType: item.accessType || 'FREE',
        pointsPrice: item.pointsPrice || 0,
      })
      await loadPoints(false)
      if (item.visibility !== 'PUBLIC') {
        legacyPrivate = true
        notice.value = '旧可见范围已停用；该案例目前仅自己可见。编辑后保存为草稿，提交审核通过后才会公开。'
      }
    } else {
      let draft = null
      try {
        draft = JSON.parse(localStorage.getItem('edumix-ai-draft') || 'null')
      } catch {}
      const matchedCourse =
        draft?.courseId && courses.value.some((c) => c.id === draft.courseId) ? draft.courseId : null
      if (matchedCourse) {
        form.courseId = matchedCourse
        await loadPoints()
        if (draft?.courseId === matchedCourse)
          form.knowledgePointIds = (draft.knowledgePointIds || []).filter((id) =>
            points.value.some((p) => p.id === id),
          )
      }
      if (draft) {
        form.caseName = draft.title || ''
        form.summary = draft.summary || ''
        form.content = draft.content || ''
        form.resourceIds = draft.resourceIds || []
        notice.value = '已载入 AI 草稿，保存后进入我的案例'
      }
    }
    const backup = localStorage.getItem(backupKey())
    let restored = false
    if (backup) {
      try {
        const local = JSON.parse(backup)
        if (confirm('检测到这台浏览器中未同步的案例内容，是否恢复？')) {
          Object.assign(form, local)
          form.visibility = 'PUBLIC'
          await loadPoints(false)
          restored = true
          notice.value = '已恢复浏览器中的未同步内容，请保存草稿'
        }
      } catch {}
    }
    lastSaved =
      editing.value && !restored && !legacyPrivate ? snapshot() : backup && !restored ? snapshot() : ''
    const ids = [...form.resourceIds]
    for (const match of form.content.matchAll(/\[\[resource:(\d+)\|/g)) ids.push(Number(match[1]))
    const body = new DOMParser().parseFromString(form.content, 'text/html')
    for (const node of body.querySelectorAll('[data-resource-id]')) ids.push(Number(node.dataset.resourceId))
    resources.value = (await getResourcesByIds(ids)).data
    initialized = true
    saveState.value = editing.value && !restored && !legacyPrivate ? '已保存到草稿箱' : '待保存'
  } catch (e) {
    error.value = e?.message || '编辑器加载失败'
  } finally {
    loading.value = false
  }
})
onBeforeUnmount(() => {
  clearTimeout(timer)
  if (initialized && snapshot() !== lastSaved) localBackup()
})
</script>

<template>
  <div class="case-writing-page">
    <header class="writing-topbar">
      <button @click="router.push('/app/my-cases')"><ArrowLeft /></button>
      <div>
        <strong>{{ editing ? '编辑草稿' : '创作案例' }}</strong
        ><small>我的案例 · 本地草稿箱</small>
      </div>
      <span class="save-state"><CheckCircle2 />{{ saveState }}</span
      ><button class="settings-toggle" @click="settingsOpen = !settingsOpen">
        <Settings2 />发布设置<ChevronDown />
      </button>
    </header>
    <div v-if="loading" class="writing-loading">正在打开编辑器...</div>
    <div v-else-if="blocked" class="writing-loading">
      <p>{{ error }}</p>
      <button class="button secondary" @click="router.push('/app/my-cases')">返回我的案例</button>
    </div>
    <template v-else>
      <div class="writing-workspace" :class="{ compact: !settingsOpen }">
        <aside class="writing-outline">
          <h3>文章目录</h3>
          <button v-for="heading in headings" :key="heading.id"><Hash />{{ heading.text }}</button>
          <p v-if="!headings.length">在正文中添加标题后，将自动生成目录。</p>
        </aside>
        <main class="writing-paper">
          <input
            v-model="form.caseName"
            class="writing-title"
            maxlength="200"
            placeholder="请输入案例标题"
          /><textarea
            v-model="form.summary"
            class="writing-summary"
            maxlength="1000"
            placeholder="填写案例摘要，将展示在案例库列表中"
          ></textarea
          ><CaseRichEditor
            v-model="form.content"
            v-model:resource-ids="form.resourceIds"
            :resources="resources"
            @resource-created="addResource"
          />
        </main>
        <aside v-if="settingsOpen" class="writing-settings">
          <h3>发布设置</h3>
          <label
            >所属课程<select v-model="form.courseId" @change="loadPoints()">
              <option value="">暂不选择课程</option>
              <option v-for="course in courses" :key="course.id" :value="course.id">
                {{ course.courseName }}
              </option>
            </select></label
          >
          <fieldset>
            <legend>知识点标签</legend>
            <label v-for="point in points" :key="point.id"
              ><input v-model="form.knowledgePointIds" type="checkbox" :value="point.id" /><span>{{
                point.pointName
              }}</span></label
            >
            <p v-if="!points.length">该课程暂无知识点</p>
          </fieldset>
          <label>可见范围<input value="公开（审核通过后）" disabled /></label
          ><label
            >访问类型<select v-model="form.accessType">
              <option value="FREE">免费访问</option>
              <option value="POINTS">积分访问</option>
            </select></label
          ><label v-if="form.accessType === 'POINTS'"
            >所需积分（1～3）<input v-model.number="form.pointsPrice" type="number" min="1" max="3" step="1"
          /></label>
          <div class="selected-info">
            <FileText />正文 {{ wordCount }} 字<br /><Hash />已选
            {{ form.knowledgePointIds.length }} 个知识点<br /><Save />引用
            {{ form.resourceIds.length }} 份资源
          </div>
        </aside>
      </div>
      <footer class="writing-footer">
        <span>共 {{ wordCount }} 字 · 自动保存到我的案例草稿箱</span>
        <p v-if="error" class="form-error">{{ error }}</p>
        <p v-if="notice" class="form-success">{{ notice }}</p>
        <button class="button secondary" :disabled="saving" @click="persist(false)"><Save />保存草稿</button
        ><button class="button primary" :disabled="saving" @click="persist(true)">
          <Send />保存并提交审核
        </button>
      </footer>
    </template>
  </div>
</template>
