import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<'light' | 'dark'>('dark')

  const isDark = computed(() => theme.value === 'dark')

  function initTheme() {
    const saved = localStorage.getItem('relake-theme')
    if (saved === 'light' || saved === 'dark') {
      theme.value = saved
    }
    applyTheme()
  }

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    localStorage.setItem('relake-theme', theme.value)
    applyTheme()
  }

  function applyTheme() {
    if (theme.value === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  return { theme, isDark, initTheme, toggleTheme }
})
