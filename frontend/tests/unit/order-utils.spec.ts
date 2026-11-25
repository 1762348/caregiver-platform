import { describe, expect, it } from 'vitest'

import {
  caregiverAction,
  formatDateLabel,
  formatDateTime,
  nextDays,
  progressSteps,
  statusTone,
} from '@/utils/order'

describe('progressSteps', () => {
  it('按时间戳标记完成节点与当前节点', () => {
    const steps = progressSteps({
      departedAt: '2026-12-01T08:00:00',
      arrivedAt: '2026-12-01T08:40:00',
      startedAt: null,
      completedAt: null,
    })

    expect(steps.map((step) => step.done)).toEqual([true, true, false, false])
    expect(steps[2].active).toBe(true)
    expect(steps[3].active).toBe(false)
  })

  it('全部完成后没有进行中节点', () => {
    const steps = progressSteps({
      departedAt: '2026-12-01T08:00:00',
      arrivedAt: '2026-12-01T08:40:00',
      startedAt: '2026-12-01T09:00:00',
      completedAt: '2026-12-01T12:00:00',
    })

    expect(steps.every((step) => step.done)).toBe(true)
    expect(steps.some((step) => step.active)).toBe(false)
  })
})

describe('date helpers', () => {
  it('生成未来 N 天，首项为今天', () => {
    const days = nextDays(3, new Date(2026, 10, 30))
    expect(days).toHaveLength(3)
    expect(days[0]).toEqual({ value: '2026-11-30', label: '今天' })
    expect(days[1].label).toBe('明天')
    expect(days[2].value).toBe('2026-12-02')
  })

  it('中文日期带星期', () => {
    expect(formatDateLabel('2026-11-30')).toBe('11月30日 周一')
  })

  it('时间格式化去掉秒', () => {
    expect(formatDateTime('2026-11-25T07:52:08.123')).toBe('2026-11-25 07:52')
    expect(formatDateTime(null)).toBe('-')
  })
})

describe('statusTone', () => {
  it('状态映射到展示色板', () => {
    expect(statusTone('PENDING_PAYMENT')).toBe('warning')
    expect(statusTone('PAID')).toBe('primary')
    expect(statusTone('IN_SERVICE')).toBe('success')
    expect(statusTone('COMPLETED')).toBe('default')
    expect(statusTone('CANCELLED')).toBe('danger')
  })
})

describe('caregiverAction', () => {
  it('按时间戳给出下一步动作', () => {
    expect(caregiverAction({ status: 'PAID', departedAt: null, arrivedAt: null })?.key).toBe(
      'depart',
    )
    expect(
      caregiverAction({
        status: 'PAID',
        departedAt: '2026-12-01T08:00:00',
        arrivedAt: null,
      })?.key,
    ).toBe('arrive')
    expect(
      caregiverAction({
        status: 'PAID',
        departedAt: '2026-12-01T08:00:00',
        arrivedAt: '2026-12-01T08:40:00',
      })?.key,
    ).toBe('start')
    expect(
      caregiverAction({
        status: 'IN_SERVICE',
        departedAt: '2026-12-01T08:00:00',
        arrivedAt: '2026-12-01T08:40:00',
      })?.key,
    ).toBe('complete')
  })

  it('未支付或已结束订单没有可执行动作', () => {
    expect(
      caregiverAction({ status: 'PENDING_PAYMENT', departedAt: null, arrivedAt: null }),
    ).toBeNull()
    expect(caregiverAction({ status: 'COMPLETED', departedAt: null, arrivedAt: null })).toBeNull()
    expect(caregiverAction({ status: 'CANCELLED', departedAt: null, arrivedAt: null })).toBeNull()
  })
})
