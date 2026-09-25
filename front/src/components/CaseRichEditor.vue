<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  Bold,
  Code2,
  Heading1,
  Image,
  Italic,
  Link,
  Link2,
  List,
  ListOrdered,
  Minus,
  Quote,
  Redo2,
  Underline,
  Undo2,
  Upload,
  Video,
  X,
} from 'lucide-vue-next'
import { getResourceCategories, getResourceFile, uploadResource } from '../api/resource'

const props = defineProps({
  modelValue: { type: String, default: '' },
  resources: { type: Array, default: () => [] },
  resourceIds: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue', 'update:resourceIds', 'resource-created'])
const editor = ref(null),
  dialog = ref(false),
  tab = ref('existing'),
  bindingMode = ref('ALL'),
  search = ref(''),
  categories = ref([]),
  uploading = ref(false),
  progress = ref(0),
  savedRange = ref(null)
const localResources = ref([]),
  blobUrls = new Map()
const uploadForm = reactive({
  file: null,
  resourceName: '',
  description: '',
  categoryId: '',
  visibility: 'PRIVATE',
})

function resourceHtml(r) {
  const name = escapeHtml(r.resourceName)
  if (r.resourceType === 'IMAGE')
    return `<figure class="embedded-image"><img data-resource-id="${r.id}" data-resource-name="${name}"><figcaption>${name}</figcaption></figure><p><br></p>`
  if (r.resourceType === 'VIDEO')
    return `<figure class="embedded-video"><video data-resource-id="${r.id}" data-resource-name="${name}" controls preload="metadata"></video><figcaption>${name}</figcaption></figure><p><br></p>`
  return `<p><a class="embedded-resource" data-resource-id="${r.id}" data-resource-name="${name}">📎 ${name}</a></p><p><br></p>`
}
function escapeHtml(v) {
  return String(v || '').replace(
    /[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c],
  )
}
function normalized(value) {
  return (value || '')
    .replace(/\[\[resource:(\d+)\|([^\]]+)]]/g, (_, id, name) => {
      const r = localResources.value.find((x) => x.id === Number(id))
      return resourceHtml(r || { id: Number(id), resourceName: name, resourceType: 'DOCUMENT' })
    })
    .replace(/\n/g, '<br>')
}
function serialized() {
  const clone = editor.value.cloneNode(true)
  clone
    .querySelectorAll('img[data-resource-id],video[data-resource-id]')
    .forEach((media) => media.removeAttribute('src'))
  return clone.innerHTML
}
function sync() {
  emit('update:modelValue', serialized())
}
function command(name, value = null) {
  editor.value.focus()
  document.execCommand(name, false, value)
  sync()
}
function heading() {
  command('formatBlock', 'h2')
}
function createLink() {
  const url = prompt('请输入链接地址')
  if (url) command('createLink', url)
}
function saveSelection(mode = 'ALL') {
  const sel = window.getSelection()
  savedRange.value =
    sel?.rangeCount && editor.value.contains(sel.anchorNode) ? sel.getRangeAt(0).cloneRange() : null
  bindingMode.value = mode
  search.value = ''
  tab.value = mode === 'ALL' ? 'existing' : 'upload'
  dialog.value = true
}
async function insertResource(r) {
  let html = resourceHtml(r)
  if (['IMAGE', 'VIDEO'].includes(r.resourceType)) {
    try {
      const blob = await getResourceFile(r.id, false),
        url = URL.createObjectURL(blob)
      blobUrls.set(r.id, url)
      html = html.replace(
        r.resourceType === 'IMAGE' ? '<img ' : '<video ',
        '<' + (r.resourceType === 'IMAGE' ? 'img' : 'video') + ' src="' + url + '" ',
      )
    } catch {}
  }
  editor.value.focus()
  const sel = window.getSelection()
  sel.removeAllRanges()
  if (savedRange.value) sel.addRange(savedRange.value)
  else {
    const range = document.createRange()
    range.selectNodeContents(editor.value)
    range.collapse(false)
    sel.addRange(range)
  }
  document.execCommand('insertHTML', false, html)
  const ids = [...new Set([...props.resourceIds, r.id])]
  emit('update:resourceIds', ids)
  sync()
  dialog.value = false
  await nextTick()
  hydrateImages()
}
async function hydrateImages() {
  if (!editor.value) return
  for (const media of editor.value.querySelectorAll('img[data-resource-id],video[data-resource-id]')) {
    const id = Number(media.dataset.resourceId)
    if (blobUrls.has(id)) {
      media.src = blobUrls.get(id)
      continue
    }
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
function chooseFile(e) {
  uploadForm.file = e.target.files[0]
  if (uploadForm.file && !uploadForm.resourceName) uploadForm.resourceName = uploadForm.file.name
}
async function doUpload() {
  if (!uploadForm.file) return
  uploading.value = true
  try {
    const data = new FormData()
    data.append('file', uploadForm.file)
    data.append('resourceName', uploadForm.resourceName)
    data.append('description', uploadForm.description)
    data.append('visibility', uploadForm.visibility)
    if (uploadForm.categoryId) data.append('categoryId', uploadForm.categoryId)
    const res = (
      await uploadResource(data, (e) => {
        if (e.total) progress.value = Math.round((e.loaded * 100) / e.total)
      })
    ).data
    localResources.value.unshift(res)
    emit('resource-created', res)
    await insertResource(res)
  } finally {
    uploading.value = false
  }
}
watch(
  () => props.resources,
  (v) => {
    localResources.value = [...v]
  },
  { immediate: true, deep: true },
)
watch(
  () => props.modelValue,
  async (v) => {
    if (editor.value && document.activeElement !== editor.value && serialized() !== v) {
      editor.value.innerHTML = normalized(v)
      await nextTick()
      hydrateImages()
    }
  },
  { immediate: true },
)
const visible = () =>
  localResources.value.filter(
    (r) =>
      (bindingMode.value === 'ALL' || r.resourceType === bindingMode.value) &&
      (!search.value || r.resourceName.toLowerCase().includes(search.value.toLowerCase())),
  )
const acceptFiles = () =>
  bindingMode.value === 'IMAGE'
    ? '.jpg,.jpeg,.png,.gif,.webp'
    : bindingMode.value === 'VIDEO'
      ? '.mp4'
      : '.jpg,.jpeg,.png,.gif,.webp,.pdf,.mp4,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,.7z'
const dialogTitle = () =>
  bindingMode.value === 'IMAGE' ? '插入图片' : bindingMode.value === 'VIDEO' ? '插入视频' : '绑定教学资源'
onMounted(async () => {
  editor.value.innerHTML = normalized(props.modelValue)
  categories.value = (await getResourceCategories()).data || []
  hydrateImages()
})
onBeforeUnmount(() => blobUrls.forEach(URL.revokeObjectURL))
</script>

<template>
  <div class="rich-editor">
    <div class="editor-toolbar">
      <button type="button" title="撤销" @click="command('undo')"><Undo2 /></button
      ><button type="button" title="重做" @click="command('redo')"><Redo2 /></button><i /><button
        type="button"
        title="标题"
        @click="heading"
      >
        <Heading1 /></button
      ><button type="button" title="加粗" @click="command('bold')"><Bold /></button
      ><button type="button" title="斜体" @click="command('italic')"><Italic /></button
      ><button type="button" title="下划线" @click="command('underline')"><Underline /></button><i /><button
        type="button"
        title="无序列表"
        @click="command('insertUnorderedList')"
      >
        <List /></button
      ><button type="button" title="有序列表" @click="command('insertOrderedList')"><ListOrdered /></button
      ><button type="button" title="引用" @click="command('formatBlock', 'blockquote')"><Quote /></button
      ><button type="button" title="代码" @click="command('formatBlock', 'pre')"><Code2 /></button
      ><button type="button" title="分隔线" @click="command('insertHorizontalRule')"><Minus /></button
      ><button type="button" title="链接" @click="createLink"><Link /></button><i /><button
        type="button"
        title="插入图片"
        @mousedown.prevent="saveSelection('IMAGE')"
      >
        <Image />图片</button
      ><button type="button" title="插入视频" @mousedown.prevent="saveSelection('VIDEO')">
        <Video />视频</button
      ><button type="button" class="bind-button" @mousedown.prevent="saveSelection('ALL')">
        <Link2 />资源绑定
      </button>
    </div>
    <div
      ref="editor"
      class="editable-area"
      contenteditable="true"
      data-placeholder="在这里自由编写案例内容，可插入标题、列表、引用、图片、视频和资源……"
      @input="sync"
    />
  </div>
  <div v-if="dialog" class="resource-dialog-mask" @click.self="dialog = false">
    <section class="resource-dialog">
      <header>
        <h2>{{ dialogTitle() }}</h2>
        <button type="button" @click="dialog = false"><X /></button>
      </header>
      <nav>
        <button type="button" :class="{ active: tab === 'upload' }" @click="tab = 'upload'">
          上传{{ bindingMode === 'IMAGE' ? '图片' : bindingMode === 'VIDEO' ? '视频' : '资源' }}</button
        ><button type="button" :class="{ active: tab === 'existing' }" @click="tab = 'existing'">
          选择已有{{ bindingMode === 'IMAGE' ? '图片' : bindingMode === 'VIDEO' ? '视频' : '资源' }}
        </button>
      </nav>
      <form v-if="tab === 'upload'" class="resource-upload-form" @submit.prevent="doUpload">
        <label class="upload-drop"
          ><input type="file" required :accept="acceptFiles()" @change="chooseFile" /><Upload />{{
            uploadForm.file?.name ||
            `点击上传${bindingMode === 'IMAGE' ? '图片' : bindingMode === 'VIDEO' ? 'MP4 视频' : '文件'}（最大200MB）`
          }}</label
        ><label>资源名称<input v-model="uploadForm.resourceName" required maxlength="200" /></label
        ><label>资源描述<textarea v-model="uploadForm.description" maxlength="1000" /></label
        ><label
          >所属分类<select v-model="uploadForm.categoryId">
            <option value="">未分类</option>
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.categoryName }}</option>
          </select></label
        ><label
          >可见范围<select v-model="uploadForm.visibility">
            <option value="PRIVATE">仅自己</option>
            <option value="PUBLIC">公开</option>
          </select></label
        >
        <div v-if="uploading" class="upload-progress">
          <i :style="{ width: progress + '%' }" /><span>{{ progress }}%</span>
        </div>
        <footer>
          <button type="button" class="button secondary" @click="dialog = false">取消</button
          ><button class="button primary" :disabled="uploading">
            {{ uploading ? '上传中...' : '上传并插入' }}
          </button>
        </footer>
      </form>
      <div v-else class="existing-resource">
        <label
          ><input
            v-model="search"
            :placeholder="`搜索已有${bindingMode === 'IMAGE' ? '图片' : bindingMode === 'VIDEO' ? '视频' : '资源'}`"
        /></label>
        <div>
          <button v-for="r in visible()" :key="r.id" type="button" @click="insertResource(r)">
            <component
              :is="r.resourceType === 'IMAGE' ? Image : r.resourceType === 'VIDEO' ? Video : FileText"
            /><span
              ><b>{{ r.resourceName }}</b
              ><small>{{ r.resourceType }} · {{ (r.fileSize / 1024 / 1024).toFixed(1) }} MB</small></span
            ><Plus />
          </button>
          <p v-if="!visible().length">没有找到可用资源</p>
        </div>
      </div>
    </section>
  </div>
</template>
