import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '../views/LandingView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import AdminAccountsView from '../views/AdminAccountsView.vue'
import ReviewCenterView from '../views/ReviewCenterView.vue'
import AppLayout from '../layouts/AppLayout.vue'
import DashboardView from '../views/DashboardView.vue'
import CourseView from '../views/CourseView.vue'
import MessageView from '../views/MessageView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import ProfileView from '../views/ProfileView.vue'
import ProfileEditView from '../views/ProfileEditView.vue'
import ResourceView from '../views/ResourceView.vue'
import CaseView from '../views/CaseView.vue'
import CaseDetailView from '../views/CaseDetailView.vue'
import CaseEditorView from '../views/CaseEditorView.vue'
import MyCasesView from '../views/MyCasesView.vue'
import PointsView from '../views/PointsView.vue'
import AdminPointGrantView from '../views/AdminPointGrantView.vue'
import AiView from '../views/AiView.vue'
import FavoritesView from '../views/FavoritesView.vue'
import TeacherProfileView from '../views/TeacherProfileView.vue'

const routes = [
  { path: '/', name: 'landing', component: LandingView },
  { path: '/login', name: 'login', component: LoginView },
  { path: '/register', name: 'register', component: RegisterView },
  {
    path: '/app',
    component: AppLayout,
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/app/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '教师工作台' } },
      { path: 'course', name: 'course', component: CourseView, meta: { title: '课程与知识点管理' } },
      { path: 'resource', name: 'resource', component: ResourceView, meta: { title: '教学资源库' } },
      { path: 'case', name: 'case', component: CaseView, meta: { title: '教学案例库' } },
      { path: 'my-cases', name: 'my-cases', component: MyCasesView, meta: { title: '我的案例' } },
      { path: 'case/new', name: 'case-new', component: CaseEditorView, meta: { title: '发布案例' } },
      { path: 'case/:id/edit', name: 'case-edit', component: CaseEditorView, meta: { title: '编辑案例' } },
      { path: 'case/:id', name: 'case-detail', component: CaseDetailView, meta: { title: '案例详情' } },
      {
        path: 'teachers/:id',
        name: 'teacher-profile',
        component: TeacherProfileView,
        meta: { title: '教师主页' },
      },
      { path: 'chats', redirect: (to) => ({ name: 'message', query: { ...to.query, tab: 'PRIVATE' } }) },
      { path: 'ai', name: 'ai', component: AiView, meta: { title: 'AI资源整合' } },
      { path: 'favorites', name: 'favorites', component: FavoritesView, meta: { title: '个人收藏' } },
      { path: 'points', name: 'points', component: PointsView, meta: { title: '我的积分' } },
      { path: 'search', redirect: '/app/resource' },
      { path: 'message', name: 'message', component: MessageView, meta: { title: '消息中心' } },
      { path: 'profile', name: 'profile', component: ProfileView, meta: { title: '个人主页' } },
      { path: 'profile/edit', name: 'profile-edit', component: ProfileEditView, meta: { title: '编辑资料' } },
      {
        path: 'admin/accounts',
        name: 'admin-accounts',
        component: AdminAccountsView,
        meta: { title: '账号管理', adminOnly: true },
      },
      {
        path: 'admin/points',
        name: 'admin-points',
        component: AdminPointGrantView,
        meta: { title: '积分发放', adminOnly: true },
      },
      {
        path: 'reviews',
        name: 'reviews',
        component: ReviewCenterView,
        meta: { title: '审核中心', reviewOnly: true },
      },
      { path: ':module', name: 'module', component: PlaceholderView },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const token = localStorage.getItem('edumix-token')
  if (to.meta.requiresAuth && !token) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && token) return { name: 'dashboard' }
  if (to.meta.adminOnly && !JSON.parse(localStorage.getItem('edumix-user') || '{}').roles?.includes('ADMIN'))
    return { name: 'dashboard' }
  if (
    to.meta.reviewOnly &&
    !JSON.parse(localStorage.getItem('edumix-user') || '{}').roles?.some((role) =>
      ['RESOURCE_MANAGER', 'ADMIN'].includes(role),
    )
  )
    return { name: 'dashboard' }
})

export default router
