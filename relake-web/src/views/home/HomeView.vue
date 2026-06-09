<script setup lang="ts">
</script>

<template>
  <div class="home-page">
    <div class="dot-bg"></div>
    <div class="welcome">
      <img src="/relake.svg" class="welcome-logo" alt="ReLake" />
      <h1>ReLake</h1>
      <p class="subtitle">实时数据湖平台</p>
      <div class="divider"></div>
      <p class="desc">CDC 采集 · 批量同步 · 湖仓一体</p>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.dot-bg {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(var(--rl-accent) 1px, transparent 1px);
  background-size: 48px 48px;
  opacity: 0.04;
  pointer-events: none;
}

.welcome {
  text-align: center;
  z-index: 1;
}

.welcome-logo {
  width: 56px;
  height: 56px;
  margin-bottom: 24px;
  filter: drop-shadow(0 0 20px rgba(6, 182, 212, 0.3));
}

.welcome h1 {
  font-family: 'Share Tech Mono', monospace;
  font-size: 32px;
  font-weight: 400;
  color: var(--rl-text-primary);
  margin: 0 0 8px;
  letter-spacing: 0.15em;
}

.subtitle {
  font-size: 14px;
  color: var(--rl-text-secondary);
  margin: 0 0 24px;
  letter-spacing: 0.3em;
}

.divider {
  width: 40px;
  height: 2px;
  background: var(--rl-accent);
  margin: 0 auto 20px;
  opacity: 0.6;
}

.desc {
  font-size: 12px;
  color: var(--rl-text-placeholder);
  letter-spacing: 0.2em;
}
</style>
