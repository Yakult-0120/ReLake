import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '仪表盘' },
        },
        {
          path: 'datasources',
          name: 'datasources',
          component: () => import('@/views/datasource/DatasourceListView.vue'),
          meta: { title: '数据源管理' },
        },
        {
          path: 'targets',
          name: 'targets',
          component: () => import('@/views/target/TargetListView.vue'),
          meta: { title: '目标存储管理' },
        },
        {
          path: 'schemas',
          name: 'schemas',
          component: () => import('@/views/schema/SchemaView.vue'),
          meta: { title: 'Schema 浏览' },
        },
        {
          path: 'tasks',
          name: 'tasks',
          component: () => import('@/views/task/TaskListView.vue'),
          meta: { title: '同步任务管理' },
        },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'ReLake'
  const authStore = useAuthStore()

  if (to.path === '/login') {
    if (authStore.isLoggedIn) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }

  if (!authStore.isLoggedIn) {
    next('/login')
    return
  }

  next()
})

export default router
