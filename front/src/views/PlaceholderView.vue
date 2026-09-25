<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Construction } from 'lucide-vue-next'
const route = useRoute()
const names = {
  resource: '教学资源库',
  case: '教学案例库',
  ai: 'AI资源整合',
  'package-plan': '资源包与授课方案',
  system: '系统管理',
}
const title = computed(() => names[route.params.module] || '功能模块')
</script>
<template>
  <div class="placeholder">
    <span><Construction :size="38" /></span>
    <h1>{{ title }}</h1>
    <p>页面结构已经预留，接入对应的 Spring Boot 接口后即可继续开发。</p>
    <RouterLink class="button primary" to="/app/dashboard">返回教师工作台</RouterLink>
  </div>
</template>
