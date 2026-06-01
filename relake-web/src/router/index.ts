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
      redirect: '/home',
      children: [
        {
          path: 'home',
          name: 'home',
          component: () => import('@/views/home/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'kafka',
          component: () => import('@/layout/KafkaLayout.vue'),
          redirect: '/kafka/topics',
          children: [
            {
              path: 'topics',
              name: 'topic-manager',
              component: () => import('@/views/kafka/TopicList.vue'),
              meta: { title: '主题管理' },
            },
            {
              path: 'acls',
              name: 'acl-manager',
              component: () => import('@/views/kafka/AclList.vue'),
              meta: { title: '权限管理' },
            },
            {
              path: 'consumers',
              name: 'consumer-manager',
              component: () => import('@/views/kafka/ConsumerGroupList.vue'),
              meta: { title: '消费者组' },
            },
          ],
        },
        {
          path: 'integration',
          component: () => import('@/layout/IntegrationLayout.vue'),
          redirect: '/integration/dashboard',
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
    },
  ],
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || 'ReLake'
  const authStore = useAuthStore()

  if (to.path === '/login') {
    if (authStore.isLoggedIn) {
      next('/home')
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
