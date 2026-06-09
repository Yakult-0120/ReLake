<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { SwitchButton, Sunny, Moon } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const tabs = [
  { key: 'home', path: '/home', label: '首页' },
  { key: 'integration', path: '/integration', label: '数据集成' },
  { key: 'kafka', path: '/kafka', label: 'Kafka 中心' },
]

const activeModule = computed(() => {
  if (route.path.startsWith('/integration')) return 'integration'
  if (route.path.startsWith('/kafka')) return 'kafka'
  return 'home'
})

function goTo(path: string) {
  router.push(path)
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <!-- 顶部导航栏 -->
    <header class="top-navbar">
      <div class="nav-left">
        <img src="/relake.svg" class="nav-logo" alt="ReLake" />
        <span class="nav-brand">ReLake</span>
        <div class="nav-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="nav-tab"
            :class="{ active: activeModule === tab.key }"
            @click="goTo(tab.path)"
          >
            {{ tab.label }}
          </div>
        </div>
      </div>

      <div class="nav-right">
        <el-button text class="nav-btn theme-btn" @click="themeStore.toggleTheme()">
          <el-icon :size="18"><Sunny v-if="themeStore.isDark" /><Moon v-else /></el-icon>
        </el-button>
        <span class="nav-user">{{ authStore.displayName || authStore.username }}</span>
        <el-button text class="nav-logout" @click="handleLogout">
          <el-icon :size="15"><SwitchButton /></el-icon>
          <span>退出</span>
        </el-button>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="app-content">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--rl-bg-primary);
}

/* ====== 顶部导航栏 ====== */
.top-navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 52px;
  padding: 0 20px;
  background: var(--rl-bg-sidebar);
  border-bottom: 1px solid var(--rl-border-color);
  flex-shrink: 0;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-logo {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.nav-tabs {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-tab {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 14px;
  color: var(--rl-text-secondary);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.nav-tab:hover {
  background: var(--rl-bg-card);
  color: var(--rl-text-regular);
}
.nav-tab.active {
  background: rgba(6, 182, 212, 0.10);
  color: var(--rl-accent-light);
}

.nav-brand {
  font-size: 17px;
  font-weight: 700;
  color: var(--rl-text-primary);
  letter-spacing: 1px;
  margin-right: 16px;
  font-family: 'Share Tech Mono', monospace;
}

/* ====== 右侧控件 ====== */
.nav-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-btn {
  color: var(--rl-text-secondary);
}
.nav-btn:hover {
  color: var(--rl-text-regular);
}

.theme-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
}
.theme-btn:hover {
  background: var(--rl-bg-card);
  color: #f59e0b;
}

.nav-user {
  font-size: 13px;
  color: var(--rl-text-secondary);
}

.nav-logout {
  color: var(--rl-text-secondary);
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
}
.nav-logout:hover {
  color: #f87171;
}

/* ====== 内容区 ====== */
.app-content {
  flex: 1;
  overflow: hidden;
}
</style>
