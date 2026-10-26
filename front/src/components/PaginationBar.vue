<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

const props = defineProps({
  page: { type: Number, default: 1 },
  pages: { type: Number, default: 0 },
  total: { type: Number, default: 0 },
})
const emit = defineEmits(['change'])

function go(target) {
  if (target >= 1 && target <= props.pages && target !== props.page) emit('change', target)
}
</script>

<template>
  <nav v-if="total > 0" class="pagination-bar" aria-label="分页导航">
    <span>共 {{ total }} 条</span>
    <button :disabled="page <= 1" title="上一页" @click="go(page - 1)">
      <ChevronLeft :size="17" />上一页
    </button>
    <strong>第 {{ page }} / {{ pages }} 页</strong>
    <button :disabled="page >= pages" title="下一页" @click="go(page + 1)">
      下一页<ChevronRight :size="17" />
    </button>
  </nav>
</template>
