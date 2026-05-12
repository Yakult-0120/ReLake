<script setup lang="ts">
</script>

<template>
  <div class="home-page">
    <div class="welcome">
      <img src="/relake.svg" class="welcome-logo" alt="ReLake" />
      <h1>欢迎使用 ReLake</h1>
      <p>实时数据湖平台</p>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
.welcome {
  text-align: center;
  color: var(--rl-text-primary);
}
.welcome-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
}
.welcome h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}
.welcome p {
  font-size: 14px;
  color: var(--rl-text-secondary);
}
</style>
