import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const caregiverMeta = { requiresAuth: true, roles: ['CAREGIVER'] }
const adminMeta = { requiresAuth: true, roles: ['ADMIN'] }

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/booking',
    name: 'booking',
    component: () => import('@/views/BookingView.vue'),
    meta: { title: '预约护工', requiresAuth: true, roles: ['USER'] },
  },
  {
    path: '/orders/:id',
    name: 'order-detail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单详情', requiresAuth: true, roles: ['USER'] },
  },
  {
    path: '/caregiver',
    component: () => import('@/layouts/CaregiverLayout.vue'),
    meta: { ...caregiverMeta, title: '护工工作台' },
    children: [
      {
        path: '',
        name: 'caregiver-home',
        component: () => import('@/views/caregiver/CaregiverHomeView.vue'),
        meta: { ...caregiverMeta, title: '护工工作台' },
      },
      {
        path: 'orders',
        name: 'caregiver-orders',
        component: () => import('@/views/caregiver/CaregiverOrdersView.vue'),
        meta: { ...caregiverMeta, title: '护工订单' },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { ...adminMeta, title: '管理后台' },
    children: [
      { path: '', redirect: '/admin/categories' },
      {
        path: 'categories',
        name: 'admin-categories',
        component: () => import('@/views/admin/AdminCategoriesView.vue'),
        meta: { ...adminMeta, title: '服务类别' },
      },
      {
        path: 'caregivers',
        name: 'admin-caregivers',
        component: () => import('@/views/admin/AdminCaregiversView.vue'),
        meta: { ...adminMeta, title: '护工管理' },
      },
      {
        path: 'schedules',
        name: 'admin-schedules',
        component: () => import('@/views/admin/AdminSchedulesView.vue'),
        meta: { ...adminMeta, title: '排班与名额' },
      },
    ],
  },
  {
    path: '/',
    component: () => import('@/layouts/MobileLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页', public: true },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/OrdersView.vue'),
        meta: { title: '我的订单', requiresAuth: true, roles: ['USER'] },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '我的', requiresAuth: true },
      },
      {
        path: 'receivers',
        name: 'receivers',
        component: () => import('@/views/ReceiversView.vue'),
        meta: { title: '被照顾人', requiresAuth: true, roles: ['USER'] },
      },
      {
        path: 'messages',
        name: 'messages',
        component: () => import('@/views/MessagesView.vue'),
        meta: { title: '消息通知', requiresAuth: true },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { title: '页面不存在', public: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (auth.isLoggedIn && !auth.user) {
    await auth.ensureLoaded()
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const roles = to.meta.roles as string[] | undefined
  if (roles && (!auth.user || !roles.includes(auth.user.role))) {
    return { path: auth.homePath }
  }

  if (to.name === 'login' && auth.isLoggedIn) {
    return { path: auth.homePath }
  }

  return true
})

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? ''
  document.title = title ? `${title} · 优护` : '优护 · 社区护工预约平台'
})

export default router
