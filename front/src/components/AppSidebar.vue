<script setup>
import { computed } from 'vue'
import {
  LayoutGrid,
  BookOpenText,
  LibraryBig,
  BriefcaseBusiness,
  Files,
  WandSparkles,
  Heart,
  Bell,
  Settings,
  BadgeCheck,
  Coins,
  X,
} from 'lucide-vue-next'
import BrandLogo from './BrandLogo.vue'

defineProps({ open: Boolean })
const emit = defineEmits(['close'])

const modules = [
  {
    name: '教师工作台',
    path: '/app/dashboard',
    icon: LayoutGrid,
    roles: ['TEACHER', 'RESOURCE_MANAGER', 'ADMIN'],
  },
  { name: '课程与知识点管理', path: '/app/course', icon: BookOpenText, roles: ['TEACHER'] },
  { name: '教学资源库', path: '/app/resource', icon: LibraryBig, roles: ['TEACHER', 'RESOURCE_MANAGER'] },
  { name: '教学案例库', path: '/app/case', icon: BriefcaseBusiness, roles: ['TEACHER', 'RESOURCE_MANAGER'] },
  { name: '我的案例', path: '/app/my-cases', icon: Files, roles: ['TEACHER', 'ADMIN'] },
  { name: 'AI资源整合', path: '/app/ai', icon: WandSparkles, featured: true, roles: ['TEACHER'] },
  { name: '个人收藏', path: '/app/favorites', icon: Heart, roles: ['TEACHER', 'RESOURCE_MANAGER', 'ADMIN'] },
  { name: '我的积分', path: '/app/points', icon: Coins, roles: ['TEACHER', 'RESOURCE_MANAGER', 'ADMIN'] },
  { name: '审核中心', path: '/app/reviews', icon: BadgeCheck, roles: ['RESOURCE_MANAGER', 'ADMIN'] },
  { name: '消息中心', path: '/app/message', icon: Bell, roles: ['TEACHER', 'RESOURCE_MANAGER', 'ADMIN'] },
]
const userRoles = JSON.parse(localStorage.getItem('edumix-user') || '{}').roles || []
const visibleModules = computed(() =>
  modules.filter((item) => item.roles.some((role) => userRoles.includes(role))),
)
const isAdmin = computed(() => userRoles.includes('ADMIN'))
</script>

<template>
  <div v-if="open" class="sidebar-mask" @click="emit('close')" />
  <aside class="sidebar" :class="{ open }">
    <div class="sidebar-brand">
      <BrandLogo />
      <button class="icon-button sidebar-close" aria-label="关闭侧边栏" @click="emit('close')">
        <X :size="19" />
      </button>
    </div>
    <nav class="sidebar-nav">
      <p class="nav-caption">备课与资源 / RESOURCES</p>
      <RouterLink
        v-for="item in visibleModules"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        @click="emit('close')"
      >
        <component :is="item.icon" :size="19" />
        <span>{{ item.name }}</span>
        <i v-if="item.featured" class="featured-dot" />
      </RouterLink>
      <p v-if="isAdmin" class="nav-caption system-caption">系统 / SYSTEM</p>
      <RouterLink v-if="isAdmin" to="/app/admin/accounts" class="nav-item" @click="emit('close')">
        <Settings :size="19" /><span>账号管理</span>
      </RouterLink>
      <RouterLink v-if="isAdmin" to="/app/admin/points" class="nav-item" @click="emit('close')">
        <Coins :size="19" /><span>积分发放</span>
      </RouterLink>
    </nav>
  </aside>
</template>
