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
/* ====== ReLake CSS 变量 — 亮色/暗色双主题 ====== */

:root {
  --rl-bg-primary: #f5f7fa;
  --rl-bg-secondary: #eef0f4;
  --rl-bg-card: #ffffff;
  --rl-bg-input: #ffffff;
  --rl-bg-sidebar: #ebedf0;
  --rl-border-color: #e4e7ed;
  --rl-border-light: #ebeef5;
  --rl-text-primary: #303133;
  --rl-text-regular: #606266;
  --rl-text-secondary: #909399;
  --rl-text-placeholder: #c0c4cc;
}

html.dark {
  --rl-bg-primary: #0c1021;
  --rl-bg-secondary: #0a0e1a;
  --rl-bg-card: #111627;
  --rl-bg-input: #0c1021;
  --rl-bg-sidebar: #0a0e1a;
  --rl-border-color: #1a1f35;
  --rl-border-light: #1a1f35;
  --rl-text-primary: #e2e8f0;
  --rl-text-regular: #c0c8d8;
  --rl-text-secondary: #8492a6;
  --rl-text-placeholder: #4b5563;
}

/* ====== Element Plus 暗色主题覆盖 ====== */

/* 页头 */
html.dark .el-header { background: #0c1021 !important; border-bottom: 1px solid #1a1f35 !important; }

/* 卡片 */
html.dark .el-card {
  background: #111627 !important;
  border: 1px solid #1a1f35 !important;
  color: #c0c8d8 !important;
  box-shadow: none !important;
}
html.dark .el-card__header {
  border-bottom: 1px solid #1a1f35 !important;
  color: #e2e8f0 !important;
  font-weight: 500;
}
html.dark .el-card__body { color: #c0c8d8 !important; }

/* 表格 */
html.dark .el-table {
  --el-table-bg-color: #111627 !important;
  --el-table-tr-bg-color: #111627 !important;
  --el-table-header-bg-color: #0c1021 !important;
  --el-table-row-hover-bg-color: #161b2e !important;
  --el-table-border-color: #1a1f35 !important;
  --el-table-text-color: #c0c8d8 !important;
  --el-table-header-text-color: #8492a6 !important;
  background: #111627 !important;
}
html.dark .el-table th.el-table__cell { background: #0c1021 !important; border-color: #1a1f35 !important; }
html.dark .el-table td.el-table__cell { border-color: #1a1f35 !important; }
html.dark .el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell { background: #0f1324 !important; }

/* 表格展开行 */
html.dark .el-table__expanded-cell { background: #0a0e1a !important; }

/* 分页 */
html.dark .el-pagination { --el-pagination-bg-color: transparent !important; }
html.dark .el-pagination button { background: #111627 !important; border-color: #1a1f35 !important; color: #8492a6 !important; }
html.dark .el-pagination button:hover { color: #60a5fa !important; }
html.dark .el-pager li { background: #111627 !important; color: #8492a6 !important; }
html.dark .el-pager li.is-active { background: #2563eb !important; color: #fff !important; }
html.dark .el-pager li:hover { color: #60a5fa !important; }
html.dark .el-pagination .el-input__wrapper { background: #111627 !important; border-color: #1a1f35 !important; }

/* 对话框 */
html.dark .el-dialog { background: #111627 !important; border: 1px solid #1a1f35 !important; }
html.dark .el-dialog__header { border-bottom: 1px solid #1a1f35 !important; }
html.dark .el-dialog__title { color: #e2e8f0 !important; }
html.dark .el-dialog__body { color: #c0c8d8 !important; }

/* 表单 */
html.dark .el-form-item__label { color: #8492a6 !important; }
html.dark .el-input__wrapper { background: #0c1021 !important; border: 1px solid #1a1f35 !important; box-shadow: none !important; }
html.dark .el-input__wrapper:hover { border-color: #374151 !important; }
html.dark .el-input__wrapper.is-focus { border-color: #2563eb !important; }
html.dark .el-input__inner { color: #e2e8f0 !important; }
html.dark .el-input__inner::placeholder { color: #4b5563 !important; }
html.dark .el-input-number .el-input__wrapper { background: #0c1021 !important; }
html.dark .el-textarea__inner { background: #0c1021 !important; border-color: #1a1f35 !important; color: #e2e8f0 !important; }
html.dark .el-textarea__inner::placeholder { color: #4b5563 !important; }
/* Select — 与 input 保持一致的暗色风格 */
html.dark .el-select .el-input__wrapper {
  background: #0c1021 !important;
  border: 1px solid #1a1f35 !important;
  box-shadow: none !important;
}
html.dark .el-select .el-input__wrapper:hover { border-color: #374151 !important; }
html.dark .el-select .el-input__wrapper.is-focus { border-color: #2563eb !important; }
html.dark .el-select .el-input__inner { color: #e2e8f0 !important; }
html.dark .el-select .el-input__inner::placeholder { color: #4b5563 !important; }
html.dark .el-select-dropdown { background: #111627 !important; border-color: #1a1f35 !important; }
html.dark .el-select-dropdown__item { color: #8492a6 !important; }
html.dark .el-select-dropdown__item.hover { background: #161b2e !important; color: #c0c8d8 !important; }
html.dark .el-select-dropdown__item.selected { color: #60a5fa !important; }

/* 消息提示 */
html.dark .el-message { border: none !important; }

/* 确认框 */
html.dark .el-message-box { background: #111627 !important; border: 1px solid #1a1f35 !important; }
html.dark .el-message-box__title { color: #e2e8f0 !important; }
html.dark .el-message-box__message { color: #8492a6 !important; }
html.dark .el-message-box__headerbtn .el-icon { color: #8492a6 !important; }

/* 按钮 */
html.dark .el-button--default { --el-button-bg-color: #161b2e !important; --el-button-border-color: #1a1f35 !important; --el-button-text-color: #c0c8d8 !important; }
html.dark .el-button--default:hover { --el-button-border-color: #2563eb !important; --el-button-text-color: #60a5fa !important; }

/* 分割线 */
html.dark .el-divider--horizontal { border-color: #1a1f35 !important; }
html.dark .el-divider__text { background: #111627 !important; color: #8492a6 !important; }

/* 空状态 */
html.dark .el-empty__description p { color: #4b5563 !important; }

/* 页标题全局覆盖 */
html.dark .page-title { color: #e2e8f0 !important; }
</style>
