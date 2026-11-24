import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { fetchMe, login as loginApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/api/auth', () => ({
  sendSmsCode: vi.fn(),
  login: vi.fn(),
  fetchMe: vi.fn(),
  refreshToken: vi.fn(),
}))

const user = {
  id: 3,
  phone: '13800000002',
  name: '张三',
  role: 'USER' as const,
  avatar: null,
  permissions: ['caregiver:view', 'order:view'],
}

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('登录后保存令牌与用户信息', async () => {
    vi.mocked(loginApi).mockResolvedValue({
      token: 'token-abc',
      tokenType: 'Bearer',
      expiresIn: 604800,
      user,
    })

    const auth = useAuthStore()
    await auth.login('13800000002', '123456')

    expect(auth.token).toBe('token-abc')
    expect(auth.user?.name).toBe('张三')
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.roleLabel).toBe('普通用户')
    expect(localStorage.getItem('youhu_token')).toBe('token-abc')
  })

  it('退出登录会清理状态与本地令牌', async () => {
    vi.mocked(loginApi).mockResolvedValue({
      token: 'token-abc',
      tokenType: 'Bearer',
      expiresIn: 604800,
      user,
    })

    const auth = useAuthStore()
    await auth.login('13800000002', '123456')
    auth.clear()

    expect(auth.token).toBe('')
    expect(auth.user).toBeNull()
    expect(localStorage.getItem('youhu_token')).toBeNull()
  })

  it('刷新后令牌失效时自动清理', async () => {
    localStorage.setItem('youhu_token', 'expired-token')
    vi.mocked(fetchMe).mockRejectedValue(new Error('401'))

    const auth = useAuthStore()
    await auth.ensureLoaded()

    expect(auth.token).toBe('')
    expect(localStorage.getItem('youhu_token')).toBeNull()
  })
})
