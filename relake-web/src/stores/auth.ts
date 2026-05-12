import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const username = ref<string>(localStorage.getItem('username') || '')
  const displayName = ref<string>(localStorage.getItem('displayName') || '')

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(newToken: string, newUsername: string, newDisplayName?: string) {
    token.value = newToken
    username.value = newUsername
    displayName.value = newDisplayName || newUsername
    localStorage.setItem('token', newToken)
    localStorage.setItem('username', newUsername)
    localStorage.setItem('displayName', newDisplayName || newUsername)
  }

  function logout() {
    token.value = ''
    username.value = ''
    displayName.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('displayName')
  }

  return { token, username, displayName, isLoggedIn, setAuth, logout }
})
