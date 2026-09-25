<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  BookOpen,
  ChevronDown,
  ChevronRight,
  Lightbulb,
  Link2,
  Pencil,
  Plus,
  Trash2,
  X,
} from 'lucide-vue-next'
import {
  createChapter,
  createCourse,
  createKnowledgePoint,
  deleteChapter,
  deleteCourse,
  deleteKnowledgePoint,
  getCourses,
  getCourseTree,
  getKnowledgePointResources,
  getKnowledgePointResourceCandidates,
  linkKnowledgePointResources,
  unlinkKnowledgePointResource,
  updateChapter,
  updateCourse,
  updateKnowledgePoint,
} from '../api/course'
import { getResourceFile } from '../api/resource'
import { useRoute } from 'vue-router'
import '../styles/course-resources.css'
import '../styles/course-objectives.css'

const courses = ref([]),
  selectedId = ref(''),
  chapters = ref([]),
  loading = ref(true),
  saving = ref(false)
const error = ref(''),
  notice = ref(''),
  modal = ref(null),
  editing = ref(null),
  openIds = ref(new Set())
const form = reactive({})
const route = useRoute()
const resourcePoint = ref(null),
  linked = ref([]),
  candidates = ref([]),
  selectedResources = ref([])
const resourceSearch = ref(''),
  resourceCategory = ref(''),
  resourceBusy = ref(false),
  resourceError = ref('')
const previewResource = ref(null),
  previewUrl = ref('')
const categories = computed(() => [...new Set(candidates.value.map((r) => r.categoryName || '未分类'))])
const filteredCandidates = computed(() =>
  candidates.value.filter(
    (r) =>
      (!resourceCategory.value || (r.categoryName || '未分类') === resourceCategory.value) &&
      (!resourceSearch.value ||
        `${r.resourceName} ${r.description || ''}`
          .toLowerCase()
          .includes(resourceSearch.value.trim().toLowerCase())),
  ),
)
const user = JSON.parse(localStorage.getItem('edumix-user') || '{}')
const canEdit = computed(() => (user.roles || []).some((role) => role === 'TEACHER' || role === 'ADMIN'))
const currentCourse = computed(() =>
  courses.value.find((course) => String(course.id) === String(selectedId.value)),
)
const formatDate = (value) => (value ? new Date(value).toLocaleDateString('zh-CN') : '—')
function resetMessages() {
  error.value = ''
  notice.value = ''
}
function showError(e) {
  error.value = e?.message || '操作失败，请稍后重试'
}

async function loadCourses(preferredId) {
  loading.value = true
  resetMessages()
  try {
    courses.value = (await getCourses()).data || []
    const target = preferredId || selectedId.value
    selectedId.value = courses.value.some((c) => String(c.id) === String(target))
      ? String(target)
      : courses.value[0]?.id
        ? String(courses.value[0].id)
        : ''
    await loadTree()
  } catch (e) {
    showError(e)
  } finally {
    loading.value = false
  }
}
async function loadTree() {
  if (!selectedId.value) {
    chapters.value = []
    return
  }
  try {
    chapters.value = (await getCourseTree(selectedId.value)).data?.chapters || []
    openIds.value = new Set(chapters.value.map((c) => c.id))
  } catch (e) {
    chapters.value = []
    showError(e)
  }
}
function toggle(id) {
  const next = new Set(openIds.value)
  next.has(id) ? next.delete(id) : next.add(id)
  openIds.value = next
}
function openModal(type, item = null) {
  editing.value = item
  modal.value = type
  Object.keys(form).forEach((key) => delete form[key])
  if (type === 'course')
    Object.assign(form, {
      courseCode: item?.courseCode || '',
      courseName: item?.courseName || '',
      college: item?.college || '',
      semester: item?.semester || '',
      description: item?.description || '',
      knowledgeObjective: item?.knowledgeObjective || '',
      abilityObjective: item?.abilityObjective || '',
      qualityObjective: item?.qualityObjective || '',
      status: item?.status ?? 1,
    })
  if (type === 'chapter')
    Object.assign(form, {
      chapterName: item?.chapterName || '',
      chapterNo: item?.chapterNo || '',
      description: item?.description || '',
      sortOrder: item?.sortOrder ?? chapters.value.length + 1,
    })
  if (type === 'point')
    Object.assign(form, {
      pointName: item?.pointName || '',
      pointCode: item?.pointCode || '',
      description: item?.description || '',
      difficulty: item?.difficulty ?? 2,
      teachingHours: item?.teachingHours ?? 1,
      sortOrder: item?.sortOrder ?? 0,
      chapterId: item?.chapterId,
    })
}
function closeModal() {
  modal.value = null
  editing.value = null
}
async function openResources(point) {
  resourcePoint.value = point
  resourceError.value = ''
  selectedResources.value = []
  resourceSearch.value = ''
  resourceCategory.value = ''
  await refreshResources()
}
async function refreshResources() {
  if (!resourcePoint.value) return
  resourceBusy.value = true
  try {
    const [existing, available] = await Promise.all([
      getKnowledgePointResources(resourcePoint.value.id),
      canEdit.value
        ? getKnowledgePointResourceCandidates(resourcePoint.value.id)
        : Promise.resolve({ data: [] }),
    ])
    linked.value = existing.data || []
    candidates.value = available.data || []
  } catch (e) {
    resourceError.value = e?.message || '资源加载失败'
  } finally {
    resourceBusy.value = false
  }
}
async function addResources() {
  if (!selectedResources.value.length) return
  resourceBusy.value = true
  resourceError.value = ''
  try {
    await linkKnowledgePointResources(resourcePoint.value.id, selectedResources.value)
    selectedResources.value = []
    await refreshResources()
    await loadTree()
    notice.value = '资源关联成功'
  } catch (e) {
    resourceError.value = e?.message || '关联失败'
  } finally {
    resourceBusy.value = false
  }
}
async function removeResource(resourceId) {
  resourceBusy.value = true
  resourceError.value = ''
  try {
    await unlinkKnowledgePointResource(resourcePoint.value.id, resourceId)
    await refreshResources()
    await loadTree()
  } catch (e) {
    resourceError.value = e?.message || '取消关联失败'
  } finally {
    resourceBusy.value = false
  }
}
async function preview(item) {
  if (!item.available) return
  resourceError.value = ''
  try {
    const download = item.resourceType === 'DOCUMENT'
    const result = await getResourceFile(item.id, download)
    const url = URL.createObjectURL(result.data)
    if (download) {
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = item.resourceName
      anchor.click()
      setTimeout(() => URL.revokeObjectURL(url), 60000)
    } else {
      previewResource.value = item
      previewUrl.value = url
    }
  } catch (e) {
    resourceError.value = e?.message || '预览失败'
  }
}
function closePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  previewResource.value = null
}
async function save() {
  saving.value = true
  resetMessages()
  try {
    if (modal.value === 'course') {
      const result = editing.value?.id ? await updateCourse(editing.value.id, form) : await createCourse(form)
      closeModal()
      await loadCourses(result.data.id)
    } else if (modal.value === 'chapter') {
      editing.value?.id
        ? await updateChapter(editing.value.id, form)
        : await createChapter(selectedId.value, form)
      closeModal()
      await loadCourses(selectedId.value)
    } else {
      editing.value?.id
        ? await updateKnowledgePoint(editing.value.id, form)
        : await createKnowledgePoint(form.chapterId, form)
      closeModal()
      await loadCourses(selectedId.value)
    }
    notice.value = '保存成功'
  } catch (e) {
    showError(e)
  } finally {
    saving.value = false
  }
}
async function remove(type, item) {
  const labels = { course: '课程及其全部章节、知识点', chapter: '章节及其全部知识点', point: '知识点' }
  if (!window.confirm(`确定删除该${labels[type]}吗？`)) return
  resetMessages()
  try {
    if (type === 'course') await deleteCourse(item.id)
    if (type === 'chapter') await deleteChapter(item.id)
    if (type === 'point') await deleteKnowledgePoint(item.id)
    await loadCourses(type === 'course' ? null : selectedId.value)
    notice.value = '删除成功'
  } catch (e) {
    showError(e)
  }
}
onMounted(() => loadCourses(route.query.courseId || undefined))
</script>

<template>
  <div class="page course-page">
    <header class="page-title-row">
      <div>
        <h1>课程与知识点管理</h1>
        <p>构建课程知识体系，为资源关联和智能推荐提供基础。</p>
      </div>
      <div class="title-actions">
        <label class="course-selector"
          ><BookOpen :size="17" /><select v-model="selectedId" @change="loadTree">
            <option value="" disabled>请选择课程</option>
            <option v-for="course in courses" :key="course.id" :value="String(course.id)">
              {{ course.courseName }}
            </option></select
          ><ChevronDown :size="15" /></label
        ><button v-if="canEdit" class="button primary" @click="openModal('course')">
          <Plus :size="17" />新建课程
        </button>
      </div>
    </header>
    <p v-if="error" class="form-error">{{ error }}</p>
    <p v-if="notice" class="form-success">{{ notice }}</p>
    <section v-if="currentCourse" class="course-summary panel">
      <div>
        <strong>{{ currentCourse.courseName }}</strong
        ><span
          >{{ currentCourse.courseCode }} · {{ currentCourse.college || '未设置学院' }} ·
          {{ currentCourse.semester || '未设置学期' }}</span
        >
      </div>
      <div class="summary-actions">
        <span>{{ currentCourse.chapterCount }} 章节 · {{ currentCourse.knowledgePointCount }} 知识点</span
        ><button v-if="canEdit" @click="openModal('course', currentCourse)"><Pencil /></button
        ><button v-if="canEdit" class="danger-action" @click="remove('course', currentCourse)">
          <Trash2 /></button
        ><button v-if="canEdit" class="button primary small" @click="openModal('chapter')">
          <Plus />添加章节
        </button>
      </div>
    </section>
    <section v-if="currentCourse" class="course-objectives panel">
      <header>
        <div>
          <span>COURSE OBJECTIVES</span>
          <h2>课程教学目标</h2>
        </div>
        <small>从知识、能力与素质三个维度描述课程预期学习成果</small>
      </header>
      <div class="course-objective-grid">
        <article class="knowledge">
          <b>01</b>
          <div>
            <h3>知识目标</h3>
            <p>{{ currentCourse.knowledgeObjective || '尚未填写知识目标' }}</p>
          </div>
        </article>
        <article class="ability">
          <b>02</b>
          <div>
            <h3>能力目标</h3>
            <p>{{ currentCourse.abilityObjective || '尚未填写能力目标' }}</p>
          </div>
        </article>
        <article class="quality">
          <b>03</b>
          <div>
            <h3>素质目标</h3>
            <p>{{ currentCourse.qualityObjective || '尚未填写素质目标' }}</p>
          </div>
        </article>
      </div>
    </section>
    <section class="panel tree-panel">
      <div class="tree-head">
        <span>章节 / 知识点名称</span><span>关联内容</span><span>最近更新</span><span>操作</span>
      </div>
      <div v-if="loading" class="tree-empty">正在加载课程体系...</div>
      <div v-else-if="!selectedId" class="tree-empty">暂无课程，请先新建课程。</div>
      <div v-else-if="!chapters.length" class="tree-empty">该课程暂无章节，请先添加章节。</div>
      <div v-for="chapter in chapters" :key="chapter.id" class="chapter">
        <div class="chapter-row" @click="toggle(chapter.id)">
          <span class="tree-name"
            ><button class="chevron">
              <ChevronDown v-if="openIds.has(chapter.id)" :size="16" /><ChevronRight
                v-else
                :size="16"
              /></button
            ><span class="folder-icon"><BookOpen :size="18" /></span
            ><strong
              >{{ chapter.chapterNo ? chapter.chapterNo + ' ' : '' }}{{ chapter.chapterName }}</strong
            ></span
          ><span>{{ chapter.knowledgePoints.length }} 个知识点</span
          ><span>{{ formatDate(chapter.updateTime) }}</span
          ><span class="row-actions"
            ><button
              v-if="canEdit"
              title="添加知识点"
              @click.stop="openModal('point', { chapterId: chapter.id })"
            >
              <Plus /></button
            ><button v-if="canEdit" title="编辑章节" @click.stop="openModal('chapter', chapter)">
              <Pencil /></button
            ><button
              v-if="canEdit"
              class="danger-action"
              title="删除章节"
              @click.stop="remove('chapter', chapter)"
            >
              <Trash2 /></button
          ></span>
        </div>
        <div v-if="openIds.has(chapter.id)" class="knowledge-list">
          <div v-for="point in chapter.knowledgePoints" :key="point.id" class="knowledge-row">
            <span class="tree-name"
              ><i class="tree-line" /><span class="point-icon"><Lightbulb :size="16" /></span
              ><span>{{ point.pointName }}</span></span
            ><span class="relation"
              ><b>{{ point.resourceCount }}</b> 份资源 · <b>{{ point.caseCount }}</b> 个案例</span
            ><span>{{ formatDate(point.updateTime) }}</span
            ><span class="row-actions"
              ><button class="link-resource" @click="openResources(point)">
                <Link2 />{{ canEdit ? '关联资源' : '查看资源' }}</button
              ><button v-if="canEdit" title="编辑" @click="openModal('point', point)"><Pencil /></button
              ><button v-if="canEdit" class="danger-action" title="删除" @click="remove('point', point)">
                <Trash2 /></button
            ></span>
          </div>
          <div v-if="!chapter.knowledgePoints.length" class="chapter-empty">暂无知识点</div>
        </div>
      </div>
    </section>
    <div v-if="modal" class="modal-mask" @click.self="closeModal">
      <form class="modal-card" :class="{ 'course-form-modal': modal === 'course' }" @submit.prevent="save">
        <header>
          <h2>
            {{ editing?.id ? '编辑' : '新建'
            }}{{ modal === 'course' ? '课程' : modal === 'chapter' ? '章节' : '知识点' }}
          </h2>
          <button type="button" @click="closeModal"><X /></button>
        </header>
        <div class="modal-fields">
          <template v-if="modal === 'course'"
            ><label>课程名称<input v-model="form.courseName" required maxlength="150" /></label
            ><label
              >课程编码<input v-model="form.courseCode" maxlength="50" placeholder="留空自动生成" /></label
            ><label>学院<input v-model="form.college" maxlength="100" /></label
            ><label>学期<input v-model="form.semester" maxlength="50" /></label
            ><label
              >状态<select v-model.number="form.status">
                <option :value="0">草稿</option>
                <option :value="1">启用</option>
                <option :value="2">归档</option>
              </select></label
            ><label class="wide"
              >课程简介<textarea
                v-model="form.description"
                maxlength="1000"
                placeholder="简要介绍课程定位、主要内容和适用对象"
              />
            </label>
            <div class="wide course-objective-heading">
              <strong>课程教学目标</strong><span>可在创建时暂不填写，后续编辑课程时继续完善</span>
            </div>
            <label class="wide objective-field knowledge"
              ><span><b>知识目标</b><small>学生完成课程后需要掌握什么</small></span
              ><textarea
                v-model="form.knowledgeObjective"
                maxlength="2000"
                placeholder="例如：掌握面向对象、集合、异常处理和并发编程等核心知识。"
              /></label
            ><label class="wide objective-field ability"
              ><span><b>能力目标</b><small>学生能够运用知识完成什么任务</small></span
              ><textarea
                v-model="form.abilityObjective"
                maxlength="2000"
                placeholder="例如：能够运用 Java 分析实际问题，完成程序设计、编码和调试。"
              /></label
            ><label class="wide objective-field quality"
              ><span><b>素质目标</b><small>课程重点培养哪些职业与综合素养</small></span
              ><textarea
                v-model="form.qualityObjective"
                maxlength="2000"
                placeholder="例如：培养规范编码、团队协作、持续学习和工程责任意识。"
              /></label
          ></template>
          <template v-else-if="modal === 'chapter'"
            ><label>章节名称<input v-model="form.chapterName" required maxlength="200" /></label
            ><label
              >章节编号<input v-model="form.chapterNo" maxlength="30" placeholder="例如：第一章" /></label
            ><label
              >排序<input v-model.number="form.sortOrder" type="number" min="0" step="1" required /></label
            ><label class="wide">章节简介<textarea v-model="form.description" maxlength="1000" /></label
          ></template>
          <template v-else
            ><label>知识点名称<input v-model="form.pointName" required maxlength="200" /></label
            ><label>知识点编码<input v-model="form.pointCode" maxlength="50" /></label
            ><label
              >难度<select v-model.number="form.difficulty">
                <option :value="1">简单</option>
                <option :value="2">中等</option>
                <option :value="3">困难</option>
              </select></label
            ><label
              >建议课时（整数）<input
                v-model.number="form.teachingHours"
                type="number"
                min="1"
                step="1"
                required /></label
            ><label
              >排序<input v-model.number="form.sortOrder" type="number" min="0" step="1" required /></label
            ><label class="wide">知识点说明<textarea v-model="form.description" /></label
          ></template>
        </div>
        <p v-if="error" class="form-error">{{ error }}</p>
        <footer>
          <button type="button" class="button secondary" @click="closeModal">取消</button
          ><button class="button primary" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        </footer>
      </form>
    </div>
    <div v-if="resourcePoint" class="modal-mask" @click.self="resourcePoint = null">
      <section class="modal-card course-resource-dialog">
        <header>
          <div>
            <h2>{{ resourcePoint.pointName }} · 关联资源</h2>
            <small>从个人收藏中选择已审核通过、当前可访问的资源</small>
          </div>
          <button @click="resourcePoint = null"><X /></button>
        </header>
        <div class="course-resource-body">
          <p v-if="resourceError" class="form-error">{{ resourceError }}</p>
          <h3>已关联（{{ linked.length }}）</h3>
          <p v-if="!linked.length" class="course-resource-empty">尚未关联资源</p>
          <div v-for="item in linked" :key="item.id" class="course-resource-item">
            <span
              ><strong>{{ item.resourceName }}</strong
              ><small>{{
                item.available
                  ? (item.categoryName || '未分类') + ' · ' + item.resourceType
                  : '资源已失效或当前无权查看'
              }}</small></span
            >
            <div>
              <button v-if="item.available" @click="preview(item)">
                {{ item.resourceType === 'DOCUMENT' ? '下载' : '预览' }}</button
              ><button
                v-if="canEdit"
                class="danger-action"
                :disabled="resourceBusy"
                @click="removeResource(item.id)"
              >
                取消关联
              </button>
            </div>
          </div>
          <template v-if="canEdit"
            ><h3>从我的收藏中添加</h3>
            <div class="course-resource-filters">
              <input v-model="resourceSearch" placeholder="搜索收藏资源名称或简介" /><select
                v-model="resourceCategory"
              >
                <option value="">全部分类</option>
                <option v-for="name in categories" :key="name" :value="name">{{ name }}</option>
              </select>
            </div>
            <p v-if="!filteredCandidates.length" class="course-resource-empty">
              暂无可选的已收藏、已审核通过资源
            </p>
            <div class="course-resource-candidates">
              <label v-for="item in filteredCandidates" :key="item.id" class="course-resource-item"
                ><input v-model="selectedResources" type="checkbox" :value="item.id" /><span
                  ><strong>{{ item.resourceName }}</strong
                  ><small>{{ item.categoryName || '未分类' }} · {{ item.resourceType }}</small></span
                ></label
              >
            </div></template
          >
        </div>
        <footer>
          <span v-if="canEdit">已选择 {{ selectedResources.length }} 项</span
          ><button class="button secondary" @click="resourcePoint = null">关闭</button
          ><button
            v-if="canEdit"
            class="button primary"
            :disabled="resourceBusy || !selectedResources.length"
            @click="addResources"
          >
            {{ resourceBusy ? '处理中...' : '确认关联' }}
          </button>
        </footer>
      </section>
    </div>
    <div v-if="previewResource" class="modal-mask" @click.self="closePreview">
      <section class="modal-card preview-card">
        <header>
          <h2>{{ previewResource.resourceName }}</h2>
          <button @click="closePreview"><X /></button>
        </header>
        <img v-if="previewResource.resourceType === 'IMAGE'" :src="previewUrl" /><video
          v-else-if="previewResource.resourceType === 'VIDEO'"
          :src="previewUrl"
          controls
        /><iframe v-else :src="previewUrl" />
      </section>
    </div>
  </div>
</template>
