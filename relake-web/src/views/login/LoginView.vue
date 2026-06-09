<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Sunny, Moon } from '@element-plus/icons-vue'
import { loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()

const isDark = computed(() => themeStore.isDark)

const form = ref({
  username: 'admin',
  password: '',
})

const loading = ref(false)

async function handleLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await loginApi(form.value)
    const { token, username, displayName } = res.data.data
    authStore.setAuth(token, username, displayName)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch {
    // error handled in interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page" :class="{ dark: isDark }">
    <!-- 背景装饰 -->
    <div class="bg-grid"></div>
    <div class="bg-glow"></div>

    <!-- 左侧：品牌 + 表单 -->
    <div class="login-left">
      <!-- 主题切换 -->
      <div class="theme-switch">
        <el-button text @click="themeStore.toggleTheme()">
          <el-icon :size="20"><Sunny v-if="isDark" /><Moon v-else /></el-icon>
        </el-button>
      </div>

      <div class="brand">
        <img src="/relake.svg" class="logo" alt="ReLake" />
        <h1 class="brand-name">ReLake</h1>
        <p class="brand-desc">实时数据湖平台</p>
      </div>

      <div class="form-area">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">登录以继续管理您的数据湖</p>

        <el-form @submit.prevent="handleLogin" label-width="0">
          <div class="field">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="User"
              size="large"
            />
          </div>
          <div class="field">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </div>
          <el-button
            class="submit-btn"
            :loading="loading"
            size="large"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>

        <p class="hint">默认账号 admin / admin</p>
      </div>
    </div>

    <!-- 右侧：数据湖装饰图 -->
    <div class="login-right">
      <div class="visual">
        <svg viewBox="0 0 400 400" class="hero-svg">
          <defs>
            <radialGradient id="glow" cx="50%" cy="45%" r="50%">
              <stop offset="0%" stop-color="#06b6d4" stop-opacity="0.15" />
              <stop offset="100%" stop-color="#0a0e1a" stop-opacity="0" />
            </radialGradient>
            <linearGradient id="lineGrad" x1="0" y1="0" x2="1" y2="0">
              <stop offset="0%" stop-color="#06b6d4" stop-opacity="0" />
              <stop offset="50%" stop-color="#22d3ee" stop-opacity="0.4" />
              <stop offset="100%" stop-color="#06b6d4" stop-opacity="0" />
            </linearGradient>
          </defs>

          <!-- 背景光晕 -->
          <circle cx="200" cy="180" r="160" fill="url(#glow)" />

          <!-- 外环数据轨道 -->
          <ellipse cx="200" cy="180" rx="150" ry="45" fill="none" stroke="#1f2937" stroke-width="0.5" opacity="0.6"/>
          <ellipse cx="200" cy="180" rx="110" ry="32" fill="none" stroke="#374151" stroke-width="0.5" opacity="0.4"/>
          <ellipse cx="200" cy="180" rx="70" ry="20" fill="none" stroke="#4b5563" stroke-width="0.5" opacity="0.3"/>

          <!-- 中心湖面 -->
          <ellipse cx="200" cy="178" rx="140" ry="42" fill="#1e3a5f" opacity="0.12"/>
          <ellipse cx="200" cy="178" rx="100" ry="30" fill="#1e40af" opacity="0.15"/>
          <ellipse cx="200" cy="178" rx="60" ry="18" fill="#0891b2" opacity="0.2"/>

          <!-- 波纹 -->
          <ellipse cx="200" cy="176" rx="30" ry="9" fill="none" stroke="#22d3ee" stroke-width="1.2" class="ripple r1"/>
          <ellipse cx="200" cy="180" rx="50" ry="15" fill="none" stroke="#22d3ee" stroke-width="0.8" class="ripple r2"/>
          <ellipse cx="200" cy="174" rx="75" ry="23" fill="none" stroke="#22d3ee" stroke-width="0.5" class="ripple r3"/>

          <!-- 数据节点 -->
          <g class="node-group">
            <circle cx="80" cy="120" r="3.5" fill="#22d3ee" opacity="0.7" class="node n1"/>
            <circle cx="320" cy="120" r="3" fill="#22d3ee" opacity="0.6" class="node n2"/>
            <circle cx="110" cy="240" r="2.5" fill="#67e8f9" opacity="0.5" class="node n3"/>
            <circle cx="290" cy="240" r="3" fill="#67e8f9" opacity="0.6" class="node n4"/>
            <circle cx="200" cy="100" r="3" fill="#06b6d4" opacity="0.7" class="node n5"/>
            <circle cx="60" cy="180" r="2" fill="#06b6d4" opacity="0.4" class="node n6"/>
            <circle cx="340" cy="180" r="2" fill="#06b6d4" opacity="0.4" class="node n7"/>
          </g>

          <!-- 数据连线 -->
          <g opacity="0.15">
            <line x1="80" y1="120" x2="200" y2="178" stroke="#06b6d4" stroke-width="0.8" class="dline"/>
            <line x1="320" y1="120" x2="200" y2="178" stroke="#06b6d4" stroke-width="0.8" class="dline"/>
            <line x1="110" y1="240" x2="200" y2="178" stroke="#22d3ee" stroke-width="0.6" class="dline"/>
            <line x1="290" y1="240" x2="200" y2="178" stroke="#22d3ee" stroke-width="0.6" class="dline"/>
            <line x1="200" y1="100" x2="200" y2="178" stroke="#06b6d4" stroke-width="0.8" class="dline flex-line"/>
            <line x1="60" y1="180" x2="80" y2="120" stroke="#67e8f9" stroke-width="0.5" class="dline"/>
            <line x1="340" y1="180" x2="320" y2="120" stroke="#67e8f9" stroke-width="0.5" class="dline"/>
          </g>

          <!-- 粒子流动点 -->
          <circle r="2" fill="#67e8f9" opacity="0.8" class="flow-dot fd1"/>
          <circle r="2" fill="#22d3ee" opacity="0.7" class="flow-dot fd2"/>
          <circle r="1.5" fill="#67e8f9" opacity="0.6" class="flow-dot fd3"/>
        </svg>

        <p class="tagline">CDC 采集 · 批量同步 · 湖仓一体</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 全局 ========== */
.login-page {
  display: flex;
  height: 100vh;
  background: var(--rl-bg-secondary);
  position: relative;
  overflow: hidden;
  font-family: 'DM Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0,0,0,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,0,0,0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  pointer-events: none;
}
.login-page.dark .bg-grid {
  background-image:
    linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
}

.bg-glow {
  position: absolute;
  top: -200px;
  right: 0;
  width: 800px;
  height: 800px;
  background: radial-gradient(circle, rgba(37,99,235,0.04) 0%, transparent 70%);
  pointer-events: none;
}
.login-page.dark .bg-glow {
  background: radial-gradient(circle, rgba(37,99,235,0.06) 0%, transparent 70%);
}

/* ========== 主题切换按钮 ========== */
.theme-switch {
  position: absolute;
  top: 20px;
  right: 24px;
  color: var(--rl-text-secondary);
}
.theme-switch .el-button:hover { color: #f59e0b; }

/* ========== 左侧 ========== */
.login-left {
  flex: 0 0 520px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 72px;
  z-index: 2;
  position: relative;
}

.brand {
  text-align: center;
  margin-bottom: 48px;
}

.logo {
  width: 40px;
  height: 40px;
  margin: 0 auto 12px;
}

.form-area {
  width: 320px;
}

.logo {
  width: 40px;
  height: 40px;
  margin-bottom: 12px;
}

.brand-name {
  font-size: 34px;
  font-weight: 700;
  color: var(--rl-text-primary);
  margin: 0 0 4px;
  letter-spacing: 2px;
  font-family: 'Share Tech Mono', monospace;
}

.brand-desc {
  font-size: 13px;
  color: var(--rl-text-placeholder);
  letter-spacing: 3px;
}

.form-area {
  max-width: 320px;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--rl-text-primary);
  margin: 0 0 6px;
}

.form-sub {
  font-size: 13px;
  color: var(--rl-text-secondary);
  margin: 0 0 36px;
}

.field {
  margin-bottom: 18px;
}

/* 输入框 — 暗色 */
.login-page.dark :deep(.el-input__wrapper) {
  background: #111827;
  border: 1px solid #1f2937;
  border-radius: 8px;
  box-shadow: none;
  padding: 3px 12px;
  transition: border-color 0.2s;
}
.login-page.dark :deep(.el-input__wrapper:hover) { border-color: #374151; }
.login-page.dark :deep(.el-input__wrapper.is-focus) { border-color: #0891b2; }
.login-page.dark :deep(.el-input__inner) { color: #e2e8f0; font-size: 14px; }
.login-page.dark :deep(.el-input__inner::placeholder) { color: #4b5563; }
.login-page.dark :deep(.el-input__prefix .el-icon) { color: #4b5563; }

.submit-btn {
  width: 100%;
  height: 44px;
  margin-top: 4px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #0891b2, #0e7490);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 3px;
  transition: all 0.25s;
}
.submit-btn:hover {
  background: linear-gradient(135deg, #06b6d4, #0891b2);
  transform: translateY(-1px);
  box-shadow: 0 6px 24px rgba(8, 145, 178, 0.35);
}

.hint {
  text-align: center;
  margin-top: 28px;
  font-size: 12px;
  color: var(--rl-text-placeholder);
}

/* ========== 右侧 ========== */
.login-right {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 1;
  /* 暗色模式背景 */
  background: transparent;
}
.login-page.dark .login-right {
  background: transparent;
}

.visual {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-svg {
  width: 380px;
  height: 380px;
}

/* SVG 图形在暗色模式下可见，亮色模式下降低不透明度 */
.login-page:not(.dark) .hero-svg circle[fill]:not([fill^="url"]) { opacity: 0.06; }
.login-page:not(.dark) .hero-svg ellipse:not(.ripple) { opacity: 0.08; }
.login-page:not(.dark) .node-group { opacity: 0.25; }

/* 波纹动画 */
.ripple { animation: rippleExpand 3s ease-in-out infinite; }
.r2 { animation-delay: 1s; }
.r3 { animation-delay: 2s; }

@keyframes rippleExpand {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.65; }
}

/* 节点脉冲 */
.node { animation: nodePulse 2.8s ease-in-out infinite; }
.n1 { animation-delay: 0s; }
.n2 { animation-delay: 0.6s; }
.n3 { animation-delay: 1.2s; }
.n4 { animation-delay: 1.8s; }
.n5 { animation-delay: 0.3s; }
.n6 { animation-delay: 2.1s; }
.n7 { animation-delay: 2.4s; }

@keyframes nodePulse {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 0.85; }
}

/* 连线闪烁 */
.dline { animation: lineShimmer 4s ease-in-out infinite; }
.flex-line { animation-delay: 2s; }

@keyframes lineShimmer {
  0%, 100% { opacity: 0.1; }
  50% { opacity: 0.25; }
}

/* 流动粒子 */
.fd1 { animation: dotFlow1 3.5s linear infinite; }
.fd2 { animation: dotFlow2 4s linear infinite; }
.fd3 { animation: dotFlow3 4.5s linear infinite; }

@keyframes dotFlow1 {
  0%   { transform: translate(200px, 100px); opacity: 0; }
  50%  { transform: translate(200px, 178px); opacity: 0.9; }
  100% { transform: translate(200px, 256px); opacity: 0; }
}
@keyframes dotFlow2 {
  0%   { transform: translate(80px, 120px); opacity: 0; }
  50%  { transform: translate(200px, 178px); opacity: 0.8; }
  100% { transform: translate(320px, 120px); opacity: 0; }
}
@keyframes dotFlow3 {
  0%   { transform: translate(110px, 240px); opacity: 0; }
  50%  { transform: translate(200px, 178px); opacity: 0.7; }
  100% { transform: translate(290px, 240px); opacity: 0; }
}

.tagline {
  color: var(--rl-text-placeholder);
  font-size: 12px;
  letter-spacing: 4px;
  margin-top: 40px;
}
</style>
