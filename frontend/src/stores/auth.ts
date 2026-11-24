import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const TOKEN_KEY = 'youhu_token'

/**
 * 认证状态：P0 只落地令牌存取骨架，P2 接入登录接口后补充用户信息与角色。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')

  const isLoggedIn = computed(() => token.value.length > 0)

  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  function clear() {
    token.value = ''
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, isLoggedIn, setToken, clear }
})
