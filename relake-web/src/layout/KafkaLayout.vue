<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { Document, Lock, User, Expand, Fold } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()

const menuItems = [
  { path: '/kafka/topics', title: '主题管理', icon: Document },
  { path: '/kafka/acls', title: '权限管理', icon: Lock },
  { path: '/kafka/consumers', title: '消费者组', icon: User },
]

const activeMenu = computed(() => route.path)

function handleMenuSelect(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="layout-root">
    <aside class="layout-aside" :class="{ collapsed: appStore.sidebarCollapsed }">
      <nav class="aside-nav">
        <div
          v-for="item in menuItems"
          :key="item.path"
          class="nav-item" :class="{ active: activeMenu === item.path }"
          @click="handleMenuSelect(item.path)"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span v-show="!appStore.sidebarCollapsed" class="nav-label">{{ item.title }}</span>
        </div>
      </nav>
    </aside>

    <div class="layout-body">
      <header class="layout-topbar">
        <el-button text class="topbar-btn" @click="appStore.toggleSidebar()">
          <el-icon :size="18"><Fold v-if="!appStore.sidebarCollapsed" /><Expand v-else /></el-icon>
        </el-button>
      </header>
      <main class="layout-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
@import '@/styles/layout.css';
</style>
