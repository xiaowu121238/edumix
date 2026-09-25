<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, BookOpen, MessageCircle, UserRound } from 'lucide-vue-next'
import { getTeacher } from '../api/chat'

const route = useRoute(),
  router = useRouter(),
  profile = ref(null),
  loading = ref(true),
  error = ref('')
const me = JSON.parse(localStorage.getItem('edumix-user') || '{}')
async function load() {
  loading.value = true
  error.value = ''
  try {
    profile.value = (await getTeacher(route.params.id)).data
  } catch (e) {
    error.value = e?.message || '教师主页加载失败'
  } finally {
    loading.value = false
  }
}
watch(() => route.params.id, load, { immediate: true })
</script>

<template>
  <div class="page teacher-page">
    <button class="reader-back" @click="router.back()"><ArrowLeft />返回</button>
    <div v-if="loading" class="panel resource-empty">正在加载教师主页...</div>
    <p v-else-if="error" class="form-error">{{ error }}</p>
    <template v-else
      ><header class="panel teacher-hero">
        <span class="teacher-avatar"
          ><img v-if="profile.teacher.avatarUrl" :src="profile.teacher.avatarUrl" alt="教师头像" /><UserRound
            v-else
        /></span>
        <div>
          <small>教师公开主页</small>
          <h1>{{ profile.teacher.realName }}</h1>
          <p>{{ profile.teacher.college || '未注明学院' }} · {{ profile.teacher.title || '教师' }}</p>
        </div>
        <button
          v-if="me.id !== profile.teacher.id && me.roles?.includes('TEACHER')"
          class="button primary"
          @click="router.push({ name: 'message', query: { tab: 'PRIVATE', teacherId: profile.teacher.id } })"
        >
          <MessageCircle />发私信
        </button>
      </header>
      <section class="teacher-cases">
        <h2>
          公开教学案例 <span>{{ profile.cases.length }}</span>
        </h2>
        <div v-if="!profile.cases.length" class="panel resource-empty">该教师暂未发布公开案例。</div>
        <article
          v-for="item in profile.cases"
          :key="item.id"
          class="panel teacher-case"
          @click="router.push({ name: 'case-detail', params: { id: item.id } })"
        >
          <div>
            <BookOpen /><span>{{ item.courseName || '未指定课程' }}</span>
          </div>
          <h3>{{ item.caseName }}</h3>
          <p>{{ item.summary || '暂无摘要' }}</p>
          <small>{{ item.knowledgePointNames.join(' · ') }}</small>
        </article>
      </section></template
    >
  </div>
</template>
