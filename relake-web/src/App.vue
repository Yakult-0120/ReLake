<script setup lang="ts">
import { onMounted } from 'vue'
import { RouterView } from 'vue-router'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()

onMounted(() => {
  themeStore.initTheme()
})
</script>

<template>
  <RouterView />
</template>

<style>
/* ====== ReLake Design System — Control Room ====== */

:root {
  --rl-bg-primary: #f8f9fb;
  --rl-bg-secondary: #eef1f5;
  --rl-bg-card: #ffffff;
  --rl-bg-input: #ffffff;
  --rl-bg-sidebar: #edf0f4;
  --rl-border-color: #e2e6ed;
  --rl-border-light: #eef1f5;
  --rl-text-primary: #1e293b;
  --rl-text-regular: #475569;
  --rl-text-secondary: #64748b;
  --rl-text-placeholder: #94a3b8;
  --rl-accent: #0891b2;
  --rl-accent-light: #06b6d4;
}

html.dark {
  --rl-bg-primary: #080c17;
  --rl-bg-secondary: #0a0f1d;
  --rl-bg-card: #111827;
  --rl-bg-input: #0d1321;
  --rl-bg-sidebar: #060a14;
  --rl-border-color: #1e293b;
  --rl-border-light: #1a2236;
  --rl-text-primary: #f1f5f9;
  --rl-text-regular: #cbd5e1;
  --rl-text-secondary: #64748b;
  --rl-text-placeholder: #475569;
  --rl-accent: #22d3ee;
  --rl-accent-light: #67e8f9;
}

/* ====== Element Plus 暗色覆盖 ====== */
html.dark .el-header { background: var(--rl-bg-sidebar); border-bottom: 1px solid var(--rl-border-color); }
html.dark .el-card {
  background: var(--rl-bg-card);
  border: 1px solid var(--rl-border-color);
  color: var(--rl-text-regular);
  box-shadow: none;
}
html.dark .el-card__header {
  border-bottom: 1px solid var(--rl-border-color);
  color: var(--rl-text-primary);
  font-weight: 500;
}
html.dark .el-card__body { color: var(--rl-text-regular); }

html.dark .el-table {
  --el-table-bg-color: var(--rl-bg-card);
  --el-table-tr-bg-color: var(--rl-bg-card);
  --el-table-header-bg-color: var(--rl-bg-secondary);
  --el-table-row-hover-bg-color: #161b2e;
  --el-table-border-color: var(--rl-border-color);
  --el-table-text-color: var(--rl-text-regular);
  --el-table-header-text-color: var(--rl-text-secondary);
}
html.dark .el-table th.el-table__cell { background: var(--rl-bg-secondary); border-color: var(--rl-border-color); }
html.dark .el-table td.el-table__cell { border-color: var(--rl-border-color); }
html.dark .el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell { background: #0d1324; }
html.dark .el-table__expanded-cell { background: var(--rl-bg-secondary); }

html.dark .el-pagination button { background: var(--rl-bg-card); border-color: var(--rl-border-color); color: var(--rl-text-secondary); }
html.dark .el-pagination button:hover { color: var(--rl-accent); }
html.dark .el-pager li { background: var(--rl-bg-card); color: var(--rl-text-secondary); }
html.dark .el-pager li.is-active { background: var(--rl-accent); color: #080c17; font-weight: 600; }
html.dark .el-pager li:hover { color: var(--rl-accent-light); }

html.dark .el-dialog { background: var(--rl-bg-card); border: 1px solid var(--rl-border-color); }
html.dark .el-dialog__header { border-bottom: 1px solid var(--rl-border-color); }
html.dark .el-dialog__title { color: var(--rl-text-primary); font-family: 'Share Tech Mono', monospace; letter-spacing: 0.05em; }
html.dark .el-dialog__body { color: var(--rl-text-regular); }

html.dark .el-form-item__label { color: var(--rl-text-secondary); }
html.dark .el-input__wrapper { background: var(--rl-bg-input); border: 1px solid var(--rl-border-color); box-shadow: none; }
html.dark .el-input__wrapper:hover { border-color: #334155; }
html.dark .el-input__wrapper.is-focus { border-color: var(--rl-accent); box-shadow: 0 0 0 1px var(--rl-accent); }
html.dark .el-input__inner { color: var(--rl-text-primary); }
html.dark .el-input__inner::placeholder { color: var(--rl-text-placeholder); }
html.dark .el-textarea__inner { background: var(--rl-bg-input); border-color: var(--rl-border-color); color: var(--rl-text-primary); }
html.dark .el-textarea__inner::placeholder { color: var(--rl-text-placeholder); }

html.dark .el-select .el-input__wrapper { background: var(--rl-bg-input); border: 1px solid var(--rl-border-color); box-shadow: none; }
html.dark .el-select .el-input__wrapper:hover { border-color: #334155; }
html.dark .el-select .el-input__wrapper.is-focus { border-color: var(--rl-accent); }
html.dark .el-select .el-input__inner { color: var(--rl-text-primary); }
html.dark .el-select .el-input__inner::placeholder { color: var(--rl-text-placeholder); }
html.dark .el-select-dropdown { background: var(--rl-bg-card); border-color: var(--rl-border-color); }
html.dark .el-select-dropdown__item { color: var(--rl-text-secondary); }
html.dark .el-select-dropdown__item.hover { background: var(--rl-bg-secondary); color: var(--rl-text-regular); }
html.dark .el-select-dropdown__item.selected { color: var(--rl-accent); font-weight: 500; }

html.dark .el-message { border: none; }
html.dark .el-message-box { background: var(--rl-bg-card); border: 1px solid var(--rl-border-color); }
html.dark .el-message-box__title { color: var(--rl-text-primary); }
html.dark .el-message-box__message { color: var(--rl-text-secondary); }
html.dark .el-message-box__headerbtn .el-icon { color: var(--rl-text-secondary); }

html.dark .el-button--default { --el-button-bg-color: #161b2e; --el-button-border-color: var(--rl-border-color); --el-button-text-color: var(--rl-text-regular); }
html.dark .el-button--default:hover { --el-button-border-color: var(--rl-accent); --el-button-text-color: var(--rl-accent-light); }

html.dark .el-divider--horizontal { border-color: var(--rl-border-color); }
html.dark .el-divider__text { background: var(--rl-bg-card); color: var(--rl-text-secondary); }

html.dark .el-empty__description p { color: var(--rl-text-placeholder); }

html.dark .page-title { color: var(--rl-text-primary); }

/* ====== 全局排版 ====== */
h1, h2, h3, h4, h5, h6 { font-family: 'Share Tech Mono', monospace; letter-spacing: 0.03em; }
.mono { font-family: 'Share Tech Mono', monospace; }
.stat-number { font-family: 'Share Tech Mono', monospace; font-variant-numeric: tabular-nums; }
</style>
