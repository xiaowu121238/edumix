<script setup>
import { AlertCircle, X } from 'lucide-vue-next'
import { dismissNotice, noticeState } from '../services/notice'
</script>

<template>
  <Teleport to="body">
    <section class="global-notice-stack" aria-live="assertive" aria-atomic="true">
      <TransitionGroup name="notice">
        <article v-for="item in noticeState.items" :key="item.id" class="global-notice" role="alert">
          <AlertCircle class="global-notice-icon" :size="22" />
          <div>
            <header>
              <strong>{{ item.title }}</strong>
              <small v-if="item.status">HTTP {{ item.status }}</small>
            </header>
            <p>{{ item.message }}</p>
          </div>
          <button type="button" aria-label="关闭提示" @click="dismissNotice(item.id)">
            <X :size="18" />
          </button>
        </article>
      </TransitionGroup>
    </section>
  </Teleport>
</template>

<style scoped>
.global-notice-stack {
  position: fixed;
  z-index: 10000;
  top: 22px;
  right: 22px;
  display: grid;
  width: min(420px, calc(100vw - 32px));
  gap: 12px;
  pointer-events: none;
}
.global-notice {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: start;
  padding: 16px;
  border: 1px solid #fecaca;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 16px 42px rgb(15 23 42 / 18%);
  color: #334155;
  pointer-events: auto;
}
.global-notice-icon {
  margin-top: 1px;
  color: #dc2626;
}
.global-notice header {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #991b1b;
}
.global-notice small {
  padding: 2px 6px;
  border-radius: 999px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 11px;
}
.global-notice p {
  margin: 5px 0 0;
  line-height: 1.55;
  overflow-wrap: anywhere;
}
.global-notice button {
  display: grid;
  padding: 3px;
  border: 0;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
}
.notice-enter-active,
.notice-leave-active {
  transition: 0.22s ease;
}
.notice-enter-from,
.notice-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
@media (max-width: 640px) {
  .global-notice-stack {
    top: 12px;
    right: 16px;
  }
}
</style>
