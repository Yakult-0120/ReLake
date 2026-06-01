<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { Document, Lock, User } from '@element-plus/icons-vue'
import { Fold, Expand } from '@element-plus/icons-vue'

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
  <div class="kafka-root">
    <!-- 侧边栏 -->
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

    <!-- 右侧主体 -->
    <div class="layout-body">
      <header class="kafka-topbar">
        <div class="topbar-left">
          <el-button text class="topbar-btn" @click="appStore.toggleSidebar()">
            <el-icon :size="18"><Fold v-if="!appStore.sidebarCollapsed" /><Expand v-else /></el-icon>
          </el-button>
        </div>
      </header>

      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.kafka-root {
  display: flex;
  height: 100%;
}

/* ====== 侧边栏 ====== */
.layout-aside {
  width: 220px;
  display: flex;
  flex-direction: column;
  background: var(--rl-bg-sidebar);
  border-right: 1px solid var(--rl-border-color);
  transition: width 0.2s;
  overflow: hidden;
  flex-shrink: 0;
}
.layout-aside.collapsed { width: 64px; }

.aside-nav { flex: 1; padding: 8px; overflow-y: auto; }

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border-radius: 8px;
  color: var(--rl-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  margin-bottom: 2px;
  white-space: nowrap;
  font-size: 14px;
}
.nav-item:hover { background: var(--rl-bg-card); color: var(--rl-text-regular); }
.nav-item.active { background: rgba(37, 99, 235, 0.12); color: #60a5fa; }
.collapsed .nav-item { justify-content: center; padding: 0; }

/* ====== 顶栏（仅折叠按钮） ====== */
.layout-body { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.kafka-topbar {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 20px;
  background: var(--rl-bg-primary);
  border-bottom: 1px solid var(--rl-border-color);
  flex-shrink: 0;
}

.topbar-btn { color: var(--rl-text-secondary); }
.topbar-btn:hover { color: var(--rl-text-regular); }

/* ====== 内容区 ====== */
.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: var(--rl-bg-primary);
}
</style>
