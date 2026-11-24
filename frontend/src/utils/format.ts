/** 金额格式化：12 -> ¥12.00 */
export function formatPrice(value: number | string | null | undefined): string {
  const amount = Number(value)
  if (!Number.isFinite(amount)) {
    return '¥0.00'
  }
  return `¥${amount.toFixed(2)}`
}

/** 手机号脱敏：13800138000 -> 138****8000 */
export function maskPhone(phone: string | null | undefined): string {
  if (!phone) {
    return ''
  }
  const digits = phone.replace(/\D/g, '')
  if (digits.length !== 11) {
    return phone
  }
  return `${digits.slice(0, 3)}****${digits.slice(7)}`
}

/** 时段展示：08:00:00 - 12:00:00 -> 08:00-12:00 */
export function formatTimeRange(start: string, end: string): string {
  const trim = (value: string) => value.slice(0, 5)
  return `${trim(start)}-${trim(end)}`
}
