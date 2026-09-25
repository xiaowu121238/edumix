<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  BookOpenText,
  Bot,
  Check,
  FileText,
  LibraryBig,
  LoaderCircle,
  MessageSquareText,
  Search,
  Send,
  Sparkles,
  WandSparkles,
  X,
} from 'lucide-vue-next'
import { chatWithAi, getAiRecommendations, getAiStatus } from '../api/ai'
import { getCases, getPublishedCases } from '../api/case'
import { getCourses } from '../api/course'
import { getResourceFile, getResources } from '../api/resource'
import '../styles/ai-draft-preview.css'

const router = useRouter()
const active = ref('recommend')
const resources = ref([]),
  cases = ref([]),
  courses = ref([])
const libraryResources = ref([]),
  libraryCases = ref([])
const resourceIds = ref([]),
  caseIds = ref([]),
  courseId = ref('')
const recommendResourceIds = ref([]),
  recommendCaseIds = ref([])
const contextTab = ref('resource'),
  keyword = ref('')
const activeTag = ref('全部')
const status = ref({ configured: false, model: '', message: '正在检查AI服务…' })
const loading = ref(true),
  working = ref(false),
  error = ref('')
const requirement = ref(''),
  recommendations = ref(null)
const message = ref(''),
  messages = ref([]),
  latestDraft = ref(null)
const draftPreviewOpen = ref(false),
  draftVersion = ref(0),
  composerInput = ref(null)
const draftPreviewBody = ref(null),
  draftBlobUrls = new Map()

const displayedResources = computed(() =>
  active.value === 'recommend' ? libraryResources.value : resources.value,
)
const displayedCases = computed(() => (active.value === 'recommend' ? libraryCases.value : cases.value))
const resourceTag = (item) => item.categoryName || item.resourceType || '未分类'
const resourceTags = computed(() => ['全部', ...new Set(displayedResources.value.map(resourceTag))])
const caseTags = computed(() => [
  '全部',
  ...new Set(
    displayedCases.value.flatMap((item) => [
      item.courseName || '未指定课程',
      ...(item.knowledgePointNames || []),
    ]),
  ),
])
const tags = computed(() => (contextTab.value === 'resource' ? resourceTags.value : caseTags.value))
const filteredResources = computed(() =>
  displayedResources.value.filter(
    (item) =>
      (activeTag.value === '全部' || resourceTag(item) === activeTag.value) &&
      (!keyword.value ||
        `${item.resourceName} ${item.description || ''}`.toLowerCase().includes(keyword.value.toLowerCase())),
  ),
)
const filteredCases = computed(() =>
  displayedCases.value.filter(
    (item) =>
      (!courseId.value || item.courseId === Number(courseId.value)) &&
      (activeTag.value === '全部' ||
        item.courseName === activeTag.value ||
        (item.knowledgePointNames || []).includes(activeTag.value)) &&
      (!keyword.value ||
        `${item.caseName} ${item.summary || ''}`.toLowerCase().includes(keyword.value.toLowerCase())),
  ),
)
const selectedResources = computed(() =>
  resources.value.filter((item) => resourceIds.value.includes(item.id)),
)
const selectedCases = computed(() => cases.value.filter((item) => caseIds.value.includes(item.id)))
const currentResourceIds = computed(() =>
  active.value === 'recommend' ? recommendResourceIds.value : resourceIds.value,
)
const currentCaseIds = computed(() => (active.value === 'recommend' ? recommendCaseIds.value : caseIds.value))
const draftPreviewHtml = computed(() => {
  if (!latestDraft.value?.content) return ''
  const map = new Map(resources.value.map((item) => [item.id, item]))
  return latestDraft.value.content.replace(/\[\[resource:(\d+)\|([^\]]+)]]/g, (_, id, rawName) => {
    const item = map.get(Number(id))
    if (!item) return ''
    const name = escapeHtml(item.resourceName || rawName)
    if (item.resourceType === 'IMAGE')
      return `<figure class="embedded-image"><img data-resource-id="${id}" alt="${name}"><figcaption>${name}</figcaption></figure>`
    if (item.resourceType === 'VIDEO')
      return `<figure class="embedded-video"><video data-resource-id="${id}" controls preload="metadata"></video><figcaption>${name}</figcaption></figure>`
    return `<p><span class="embedded-resource" data-resource-id="${id}">📎 ${name}</span></p>`
  })
})

function escapeHtml(value) {
  return String(value || '').replace(
    /[&<>"']/g,
    (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char],
  )
}
function cleanupDraftMedia() {
  draftBlobUrls.forEach(URL.revokeObjectURL)
  draftBlobUrls.clear()
}
async function hydrateDraftMedia() {
  await nextTick()
  if (!draftPreviewBody.value) return
  cleanupDraftMedia()
  for (const media of draftPreviewBody.value.querySelectorAll(
    'img[data-resource-id],video[data-resource-id]',
  )) {
    try {
      const id = Number(media.dataset.resourceId),
        blob = await getResourceFile(id, false),
        url = URL.createObjectURL(blob)
      draftBlobUrls.set(id, url)
      media.src = url
    } catch {
      media.setAttribute('aria-label', '资源暂时无法预览')
    }
  }
}

function toggle(type, id) {
  const list = type === 'resource' ? currentResourceIds.value : currentCaseIds.value
  const index = list.indexOf(id)
  if (index >= 0) list.splice(index, 1)
  else list.push(id)
}
function clearContext() {
  if (active.value === 'recommend') {
    recommendResourceIds.value = []
    recommendCaseIds.value = []
  } else {
    resourceIds.value = []
    caseIds.value = []
  }
}
function changeContextTab(value) {
  contextTab.value = value
  activeTag.value = '全部'
  keyword.value = ''
}
async function refreshMaterials() {
  const [resourceResult, caseResult, publicResourceResult, publicCaseResult] = await Promise.all([
    getResources({ favoriteOnly: true }),
    getCases({ favoriteOnly: true }),
    getResources({}),
    getPublishedCases({}),
  ])
  resources.value = resourceResult.data || []
  cases.value = (caseResult.data || []).filter((item) => item.contentUnlocked !== false)
  libraryResources.value = (publicResourceResult.data || [])
    .filter((item) => item.visibility === 'PUBLIC' && item.auditStatus === 'APPROVED')
    .slice(0, 60)
  libraryCases.value = (publicCaseResult.data || [])
    .filter((item) => item.visibility === 'PUBLIC' && item.auditStatus === 'APPROVED')
    .slice(0, 60)
  resourceIds.value = resourceIds.value.filter((id) => resources.value.some((item) => item.id === id))
  caseIds.value = caseIds.value.filter((id) => cases.value.some((item) => item.id === id))
  recommendResourceIds.value = recommendResourceIds.value.filter((id) =>
    libraryResources.value.some((item) => item.id === id),
  )
  recommendCaseIds.value = recommendCaseIds.value.filter((id) =>
    libraryCases.value.some((item) => item.id === id),
  )
}
watch(courseId, (value) => {
  caseIds.value = caseIds.value.filter((id) =>
    cases.value.some((item) => item.id === id && (!value || item.courseId === Number(value))),
  )
  recommendCaseIds.value = recommendCaseIds.value.filter((id) =>
    libraryCases.value.some((item) => item.id === id && (!value || item.courseId === Number(value))),
  )
})
async function recommend() {
  working.value = true
  error.value = ''
  try {
    recommendations.value = (
      await getAiRecommendations({
        courseId: courseId.value || null,
        resourceIds: recommendResourceIds.value,
        caseIds: recommendCaseIds.value,
        requirement: requirement.value,
        limit: 6,
      })
    ).data
  } catch (e) {
    error.value = e?.message || '推荐失败'
  } finally {
    working.value = false
  }
}
async function sendMessage(preset) {
  // 后续追问保留同一轮草稿和上下文，便于教师逐步调整案例内容。
  const content = (preset || message.value).trim()
  if (!content || working.value) return
  const history = messages.value.slice(-10).map((item) => ({ role: item.role, content: item.content }))
  messages.value.push({ role: 'user', content })
  message.value = ''
  working.value = true
  error.value = ''
  try {
    const result = (
      await chatWithAi({
        message: content,
        resourceIds: resourceIds.value,
        caseIds: caseIds.value,
        history,
        currentDraft: latestDraft.value,
      })
    ).data
    messages.value.push({ role: 'assistant', content: result.answer })
    if (result.draft) {
      latestDraft.value = result.draft
      draftVersion.value += 1
      draftPreviewOpen.value = true
    }
  } catch (e) {
    error.value = e?.message || 'AI对话失败'
  } finally {
    working.value = false
  }
}
function newConversation() {
  messages.value = []
  latestDraft.value = null
  draftVersion.value = 0
  draftPreviewOpen.value = false
  message.value = ''
}
async function continueEditing() {
  draftPreviewOpen.value = false
  await nextTick()
  composerInput.value?.focus()
}
function useDraft() {
  if (!latestDraft.value) return
  localStorage.setItem('edumix-ai-draft', JSON.stringify(latestDraft.value))
  router.push({ name: 'case-new' })
}
function openItem(item) {
  router.push(
    item.type === 'CASE' ? `/app/case/${item.id}` : { path: '/app/resource', query: { id: item.id } },
  )
}
onMounted(async () => {
  try {
    const [statusResult, courseResult] = await Promise.all([getAiStatus(), getCourses(), refreshMaterials()])
    status.value = statusResult.data
    courses.value = courseResult.data || []
  } catch (e) {
    error.value = e?.message || 'AI工作台加载失败'
  } finally {
    loading.value = false
  }
})
watch(active, () => {
  activeTag.value = '全部'
  keyword.value = ''
})
watch([draftPreviewOpen, latestDraft], ([open]) => {
  if (open) hydrateDraftMedia()
  else cleanupDraftMedia()
})
onBeforeUnmount(cleanupDraftMedia)
</script>

<template>
  <div class="page ai-page">
    <div class="page-title-row ai-heading">
      <div>
        <span class="ai-eyebrow"><Sparkles /> EDU MIX AI</span>
        <h1>AI 备课助手</h1>
        <p>只专注于平台资源推荐与教学案例整合。</p>
      </div>
      <span class="ai-status" :class="{ ready: status.configured }"
        ><i />{{ status.configured ? `${status.model} 已连接` : 'AI 尚未配置' }}</span
      >
    </div>

    <div v-if="loading" class="panel ai-loading"><LoaderCircle />正在读取资源库与个人收藏…</div>
    <template v-else>
      <div v-if="!status.configured" class="ai-config-notice">
        <Bot />
        <div>
          <strong>完成后端配置后即可调用模型</strong><span>{{ status.message }}</span>
        </div>
      </div>
      <div class="ai-tabs">
        <button :class="{ active: active === 'recommend' }" @click="active = 'recommend'">
          <WandSparkles /><span><b>智能推荐</b><small>从全部公开资源与案例中推荐</small></span>
        </button>
        <button :class="{ active: active === 'chat' }" @click="active = 'chat'">
          <MessageSquareText /><span><b>对话整合</b><small>讨论并形成可编辑的案例草稿</small></span>
        </button>
      </div>

      <div class="ai-workspace">
        <aside class="panel ai-context">
          <header>
            <div>
              <h2>{{ active === 'recommend' ? '公开资源库' : '我的收藏' }}</h2>
              <p>
                {{
                  active === 'recommend'
                    ? '可选参考素材表达推荐方向；AI 仍会从完整公开库中扩展推荐。'
                    : '可不选；对话仅引用本轮主动选择的收藏资料。'
                }}
              </p>
            </div>
            <button v-if="currentResourceIds.length || currentCaseIds.length" @click="clearContext">
              清空
            </button>
          </header>
          <label v-if="contextTab === 'case'" class="ai-course-filter"
            >课程范围<select v-model="courseId">
              <option value="">全部课程</option>
              <option v-for="course in courses" :key="course.id" :value="course.id">
                {{ course.courseName }}
              </option>
            </select></label
          >
          <nav>
            <button :class="{ active: contextTab === 'resource' }" @click="changeContextTab('resource')">
              资源 {{ displayedResources.length }} · 已选 {{ currentResourceIds.length }}</button
            ><button :class="{ active: contextTab === 'case' }" @click="changeContextTab('case')">
              案例 {{ displayedCases.length }} · 已选 {{ currentCaseIds.length }}
            </button>
          </nav>
          <label class="ai-search"><Search /><input v-model="keyword" placeholder="搜索名称或摘要" /></label>
          <div class="ai-favorite-tags">
            <button
              v-for="tag in tags"
              :key="tag"
              :class="{ active: activeTag === tag }"
              @click="activeTag = tag"
            >
              {{ tag }}
            </button>
          </div>
          <div class="ai-context-list">
            <button
              v-for="item in contextTab === 'resource' ? filteredResources : filteredCases"
              :key="item.id"
              :class="{
                selected: (contextTab === 'resource' ? currentResourceIds : currentCaseIds).includes(item.id),
              }"
              @click="toggle(contextTab, item.id)"
            >
              <span class="context-icon"
                ><LibraryBig v-if="contextTab === 'resource'" /><BookOpenText v-else
              /></span>
              <span
                ><b>{{ contextTab === 'resource' ? item.resourceName : item.caseName }}</b
                ><small>{{
                  contextTab === 'resource'
                    ? `${item.resourceType} · ${item.categoryName || '未分类'}`
                    : `${item.courseName || '未指定课程'} · ${item.creatorName}`
                }}</small></span
              ><i><Check /></i>
            </button>
            <div
              v-if="!(contextTab === 'resource' ? filteredResources : filteredCases).length"
              class="ai-favorite-empty"
            >
              <p>
                {{
                  (contextTab === 'resource' ? displayedResources : displayedCases).length
                    ? '没有符合筛选条件的内容'
                    : active === 'recommend'
                      ? '当前没有审核通过且公开的内容'
                      : '还没有可用的收藏内容'
                }}
              </p>
              <button
                v-if="
                  active === 'chat' &&
                  !(contextTab === 'resource' ? displayedResources : displayedCases).length
                "
                @click="router.push(contextTab === 'resource' ? '/app/resource' : '/app/case')"
              >
                去{{ contextTab === 'resource' ? '资源库' : '案例库' }}收藏
              </button>
            </div>
          </div>
        </aside>

        <main v-if="active === 'recommend'" class="panel ai-main recommend-main">
          <div class="ai-main-intro">
            <span><WandSparkles /></span>
            <div>
              <h2>从全部公开内容中发现合适素材</h2>
              <p v-if="recommendResourceIds.length || recommendCaseIds.length">
                已选择 {{ recommendResourceIds.length }} 份资源、{{ recommendCaseIds.length }}
                个案例作为方向参考；推荐范围仍是完整公开库。
              </p>
              <p v-else>候选结果来自平台中审核通过且公开的资源和案例，也可以先选择参考素材。</p>
            </div>
          </div>
          <label class="recommend-input">
            <textarea
              v-model="requirement"
              maxlength="1000"
              placeholder="描述你的备课目标，例如：为 Java 异常处理设计一节包含导入案例、课堂练习和课后拓展的课程"
            ></textarea
            ><button
              :disabled="working || !status.configured || (!libraryResources.length && !libraryCases.length)"
              @click="recommend"
            >
              <LoaderCircle v-if="working" class="spin" /><Sparkles v-else />开始推荐
            </button></label
          >
          <p v-if="error" class="form-error">{{ error }}</p>
          <section v-if="recommendations" class="recommend-results">
            <header>
              <h3>推荐结果</h3>
              <p>{{ recommendations.summary }}</p>
            </header>
            <div class="recommend-grid">
              <article
                v-for="item in [...recommendations.resources, ...recommendations.cases]"
                :key="`${item.type}-${item.id}`"
              >
                <span>{{ item.type === 'RESOURCE' ? '教学资源' : '教学案例' }}</span>
                <h4>{{ item.name }}</h4>
                <small>{{ item.metadata }}</small>
                <p>{{ item.reason }}</p>
                <blockquote>{{ item.useSuggestion }}</blockquote>
                <button @click="openItem(item)">查看内容<ArrowRight /></button>
              </article>
            </div>
          </section>
          <div v-else class="ai-empty">
            <Sparkles />
            <h3>
              {{ libraryResources.length || libraryCases.length ? '等待你的推荐需求' : '公开资源库暂时为空' }}
            </h3>
            <p>
              {{
                libraryResources.length || libraryCases.length
                  ? '描述备课目标后，AI 会从全部公开资源和案例中筛选。'
                  : '请先等待资源或案例审核通过并公开。'
              }}
            </p>
          </div>
        </main>

        <main v-else class="panel ai-main chat-main">
          <header>
            <div>
              <h2>教学案例整合对话</h2>
              <p v-if="resourceIds.length || caseIds.length">
                本轮引用 {{ resourceIds.length }} 份收藏资源、{{ caseIds.length }} 个收藏案例
              </p>
              <p v-else>本轮未引用收藏资料；可以直接讨论教学思路</p>
            </div>
            <button v-if="messages.length" @click="newConversation">新对话</button>
          </header>
          <div class="chat-stream">
            <div v-if="!messages.length" class="chat-welcome">
              <span><Bot /></span>
              <h3>告诉我你想怎样整合</h3>
              <p>我可以梳理教学目标、重组过程、融合资料，并生成案例草稿。</p>
              <div>
                <button @click="sendMessage('请分析我选择的内容，并给出适合的教学案例整合思路。')">
                  分析所选内容</button
                ><button
                  @click="sendMessage('请将我选择的内容整合成一篇结构完整、可继续编辑的教学案例草稿。')"
                >
                  整合为案例草稿
                </button>
              </div>
            </div>
            <div v-for="(item, index) in messages" :key="index" class="chat-message" :class="item.role">
              <span>{{ item.role === 'assistant' ? 'AI' : '我' }}</span>
              <p>{{ item.content }}</p>
            </div>
            <div v-if="working" class="chat-message assistant">
              <span>AI</span>
              <p class="thinking"><i /><i /><i /></p>
            </div>
          </div>
          <div v-if="latestDraft" class="draft-ready">
            <FileText />
            <div>
              <strong>案例草稿 v{{ draftVersion }}</strong
              ><span>{{ latestDraft.title }}</span>
            </div>
            <div class="draft-ready-actions">
              <button class="draft-preview-button" @click="draftPreviewOpen = true">预览草稿</button
              ><button @click="useDraft">带入案例编辑器<ArrowRight /></button>
            </div>
          </div>
          <div v-if="latestDraft" class="draft-revision-presets">
            <span>继续修改：</span
            ><button
              :disabled="working"
              @click="sendMessage('请保留核心内容，进一步补充课堂互动环节，并返回完整修订稿。')"
            >
              增加课堂互动</button
            ><button
              :disabled="working"
              @click="
                sendMessage(
                  '请分析所选材料之间的共同结构和关键差异，用自然且准确的跨学科类比重新整合，并把适合预览的所选资源插入正文，返回完整修订稿。',
                )
              "
            >
              跨学科融合</button
            ><button
              :disabled="working"
              @click="sendMessage('请精简当前草稿，使结构更紧凑，并返回完整修订稿。')"
            >
              精简内容</button
            ><button
              :disabled="working"
              @click="sendMessage('请检查当前草稿的教学目标、教学过程与评价是否一致，修正后返回完整草稿。')"
            >
              检查教学闭环
            </button>
          </div>
          <p v-if="error" class="form-error">{{ error }}</p>
          <form class="chat-composer" @submit.prevent="sendMessage()">
            <textarea
              ref="composerInput"
              v-model="message"
              maxlength="4000"
              :placeholder="
                latestDraft
                  ? '继续提出修改要求，例如：把课堂活动改成小组协作；Shift+Enter 可换行'
                  : '围绕教学资源或案例提出问题；Shift+Enter 可换行'
              "
              @keydown.enter.exact.prevent="sendMessage()"
            ></textarea
            ><button :disabled="working || !message.trim() || !status.configured"><Send /></button>
          </form>
          <small class="ai-boundary"
            >AI 仅回答教学资源推荐与教学案例整合相关内容，请核对生成结果后再使用。</small
          >

          <div
            v-if="draftPreviewOpen && latestDraft"
            class="ai-draft-modal-mask"
            @click.self="draftPreviewOpen = false"
          >
            <section
              class="ai-draft-modal"
              role="dialog"
              aria-modal="true"
              aria-labelledby="draft-preview-title"
            >
              <header class="ai-draft-modal-header">
                <div>
                  <span>AI 案例草稿 · v{{ draftVersion }}</span>
                  <h2 id="draft-preview-title">草稿预览</h2>
                </div>
                <button aria-label="关闭预览" @click="draftPreviewOpen = false"><X /></button>
              </header>
              <div class="ai-draft-modal-body">
                <article class="ai-draft-paper">
                  <div class="ai-draft-paper-meta">
                    <span>生成草稿</span><small>确认前可返回对话继续修改</small>
                  </div>
                  <h1>{{ latestDraft.title }}</h1>
                  <p v-if="latestDraft.summary" class="ai-draft-summary">{{ latestDraft.summary }}</p>
                  <div ref="draftPreviewBody" class="ai-draft-content" v-html="draftPreviewHtml"></div>
                </article>
              </div>
              <footer class="ai-draft-modal-footer">
                <p>不满意可以继续告诉 AI 要增加、删减或改写什么。</p>
                <div>
                  <button class="secondary" @click="continueEditing">继续提出修改</button
                  ><button class="primary" @click="useDraft">确认并带入编辑器<ArrowRight /></button>
                </div>
              </footer>
            </section>
          </div>
        </main>
      </div>
    </template>
  </div>
</template>
