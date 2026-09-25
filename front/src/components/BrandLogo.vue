<script setup>
import { GraduationCap } from 'lucide-vue-next'
defineProps({ light: Boolean })
</script>

<template>
  <div class="brand">
    <span class="brand-mark"><GraduationCap :size="21" stroke-width="2.4" /></span>
    <span>
      <strong :class="{ light }">EduMix</strong>
      <small :class="{ light }">混合式教学资源平台</small>
    </span>
  </div>
</template>
