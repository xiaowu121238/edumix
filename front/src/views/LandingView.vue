<script setup>
import { useRouter } from 'vue-router'
import { Search, ArrowRight, PlayCircle, BookOpen, Users, Sparkles, Plus } from 'lucide-vue-next'
import BrandLogo from '../components/BrandLogo.vue'
const router = useRouter()
const cases = [
  { title: 'Spring Boot 微服务实战案例', type: '软件工程', icon: BookOpen, color: 'blue' },
  { title: 'AI辅助的混合式教学设计', type: '教学创新', icon: Sparkles, color: 'purple' },
  { title: '设计模式协作课堂实践', type: '计算机科学', icon: Users, color: 'green' },
]
</script>

<template>
  <div class="landing-page">
    <header class="public-header container-wide">
      <BrandLogo />
      <nav><a href="#features">平台能力</a><a href="#cases">公开资源</a><a href="#about">关于我们</a></nav>
      <label class="public-search"><Search :size="16" /><input placeholder="搜索公开案例或资源..." /></label>
      <button class="text-button" @click="router.push('/login')">登录</button>
      <button class="button primary pill" @click="router.push('/login')">进入平台</button>
    </header>

    <main>
      <section class="hero">
        <div class="hero-glow glow-one" />
        <div class="hero-glow glow-two" />
        <div class="hero-badge"><Sparkles :size="14" /> AI 驱动的新一代教学资源平台</div>
        <h1>重塑备课体验，<br /><span>让优质资源触手可及</span></h1>
        <p>汇聚课程、知识点、教学案例与多媒体资源，借助智能技术快速生成高质量授课方案。</p>
        <div class="hero-actions">
          <button class="button primary large" @click="router.push('/login')">
            立即开始备课 <ArrowRight :size="18" />
          </button>
          <button class="button secondary large"><PlayCircle :size="19" />观看平台介绍</button>
        </div>
        <div class="hero-proof">
          <span>已服务</span><strong>12,600+</strong><span>名高校教师</span><i /><strong>86,000+</strong
          ><span>份优质资源</span>
        </div>
      </section>

      <section id="cases" class="case-section container">
        <div class="section-heading">
          <div>
            <small>POPULAR RESOURCES</small>
            <h2>热门教学案例</h2>
            <p>来自优秀教师的真实课堂实践，开放共享，共同成长。</p>
          </div>
          <a href="#">查看全部 <ArrowRight :size="16" /></a>
        </div>
        <div class="case-grid">
          <article v-for="item in cases" :key="item.title" class="case-card">
            <div class="case-cover" :class="item.color">
              <component :is="item.icon" :size="52" /><span>{{ item.type }}</span>
            </div>
            <div class="case-content">
              <small>{{ item.type }} · 教学案例</small>
              <h3>{{ item.title }}</h3>
              <p>结合真实教学情境，提供完整的教学目标、课堂活动与配套资源。</p>
              <button @click="router.push('/login')"><Plus :size="15" />加入资源包</button>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>
