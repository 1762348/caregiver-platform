import { describe, expect, it } from 'vitest'

import { formatPrice, formatTimeRange, maskPhone } from '@/utils/format'

describe('formatPrice', () => {
  it('保留两位小数', () => {
    expect(formatPrice(12)).toBe('¥12.00')
    expect(formatPrice('99.9')).toBe('¥99.90')
  })

  it('非法值回退为 0', () => {
    expect(formatPrice(null)).toBe('¥0.00')
    expect(formatPrice('abc')).toBe('¥0.00')
  })
})

describe('maskPhone', () => {
  it('对 11 位手机号脱敏', () => {
    expect(maskPhone('13800138000')).toBe('138****8000')
  })

  it('空值与非手机号原样返回', () => {
    expect(maskPhone('')).toBe('')
    expect(maskPhone('123')).toBe('123')
  })
})

describe('formatTimeRange', () => {
  it('去掉秒并拼接', () => {
    expect(formatTimeRange('08:00:00', '12:00:00')).toBe('08:00-12:00')
  })
})
