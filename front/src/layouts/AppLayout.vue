<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'
import AppHeader from '../components/AppHeader.vue'

const sidebarOpen = ref(window.innerWidth >= 1024)
const onResize = () => {
  sidebarOpen.value = window.innerWidth >= 1024
}
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => window.removeEventListener('resize', onResize))
</script>

<template>
  <div class="app-shell">
    <AppSidebar :open="sidebarOpen" @close="sidebarOpen = false" />
    <div class="app-stage" :class="{ shifted: sidebarOpen }">
      <AppHeader :sidebar-open="sidebarOpen" @toggle="sidebarOpen = !sidebarOpen" />
      <main class="app-main"><RouterView /></main>
    </div>
  </div>
</template>
