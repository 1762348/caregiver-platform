import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import HomeView from '@/views/HomeView.vue'

vi.mock('@/api/catalog', () => ({
  fetchCategories: vi.fn().mockResolvedValue([
    {
      id: 1,
      code: 'DAILY_CARE',
      name: '日常照料',
      description: '饮食起居',
      basePrice: 200,
    },
  ]),
}))

vi.mock('@/api/meta', () => ({
  fetchServerMeta: vi.fn().mockResolvedValue({
    name: 'caregiver-platform',
    displayName: '优护',
    version: '0.1.0',
    profile: 'test',
    serverTime: '2026-01-01T00:00:00',
  }),
}))

vi.mock('@/api/messages', () => ({
  fetchUnreadCount: vi.fn().mockResolvedValue(0),
}))

vi.mock('@/api/orders', () => ({
  fetchOrders: vi.fn().mockResolvedValue({ items: [], total: 0, page: 0, size: 10 }),
}))

describe('HomeView', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('渲染接口返回的服务类别与后端连接状态', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: { template: '<div />' } },
        { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
      ],
    })
    await router.push('/')
    await router.isReady()

    const wrapper = mount(HomeView, {
      global: {
        plugins: [createPinia(), router],
        stubs: { 'van-icon': true, 'van-button': true, 'van-loading': true },
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('日常照料')
    expect(wrapper.text()).toContain('后端已连接')
    expect(wrapper.text()).toContain('暂无进行中的服务')
  })
})
