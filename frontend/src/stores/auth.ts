import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

import {
  fetchMe,
  login as loginApi,
  sendSmsCode,
  type UserProfile,
  type UserRole,
} from '@/api/auth'

const TOKEN_KEY = 'youhu_token'

const ROLE_LABELS: Record<UserRole, string> = {
  USER: '普通用户',
  CAREGIVER: '护工',
  ADMIN: '管理员',
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<UserProfile | null>(null)

  const isLoggedIn = computed(() => token.value.length > 0)
  const roleLabel = computed(() => (user.value ? ROLE_LABELS[user.value.role] : ''))
  const displayName = computed(() => {
    if (!user.value) return ''
    return user.value.name || user.value.phone
  })

  function setSession(value: string, profile: UserProfile) {
    token.value = value
    user.value = profile
    localStorage.setItem(TOKEN_KEY, value)
  }

  function clear() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  async function sendCode(phone: string) {
    await sendSmsCode(phone)
  }

  async function login(phone: string, code: string) {
    const result = await loginApi(phone, code)
    setSession(result.token, result.user)
    return result.user
  }

  /** 刷新页面后恢复用户信息；令牌失效则自动清理 */
  async function ensureLoaded() {
    if (!token.value || user.value) return
    try {
      user.value = await fetchMe()
    } catch {
      clear()
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    roleLabel,
    displayName,
    setSession,
    sendCode,
    login,
    ensureLoaded,
    clear,
  }
})
