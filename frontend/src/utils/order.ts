import type { OrderStage, OrderStatus } from '@/api/orders'

export const STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

export const STAGE_LABELS: Record<OrderStage, string> = {
  PENDING_PAYMENT: '待支付',
  PENDING_DEPARTURE: '待出发',
  ON_THE_WAY: '护工在路上',
  ARRIVED: '护工已到达',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

/** 状态对应的展示颜色（Vant tag / 自定义 pill 共用） */
export function statusTone(status: OrderStatus): 'warning' | 'primary' | 'success' | 'default' | 'danger' {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'warning'
    case 'PAID':
      return 'primary'
    case 'IN_SERVICE':
      return 'success'
    case 'COMPLETED':
      return 'default'
    default:
      return 'danger'
  }
}

/** 服务进度的四个节点及完成条件 */
export interface ProgressStep {
  key: 'departed' | 'arrived' | 'started' | 'completed'
  label: string
  time: string | null
  done: boolean
  active: boolean
}

export function progressSteps(order: {
  departedAt: string | null
  arrivedAt: string | null
  startedAt: string | null
  completedAt: string | null
}): ProgressStep[] {
  const steps: ProgressStep[] = [
    { key: 'departed', label: '护工出发', time: order.departedAt, done: !!order.departedAt, active: false },
    { key: 'arrived', label: '到达服务地点', time: order.arrivedAt, done: !!order.arrivedAt, active: false },
    { key: 'started', label: '开始服务', time: order.startedAt, done: !!order.startedAt, active: false },
    { key: 'completed', label: '服务完成', time: order.completedAt, done: !!order.completedAt, active: false },
  ]
  const firstPending = steps.findIndex((step) => !step.done)
  if (firstPending >= 0) {
    steps[firstPending].active = true
  }
  return steps
}

/** 2026-12-01 -> 12月1日 周一 */
export function formatDateLabel(value: string): string {
  const [year, month, day] = value.split('-').map(Number)
  if (!year || !month || !day) return value
  const date = new Date(year, month - 1, day)
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
  return `${month}月${day}日 ${weekday}`
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return '-'
  return value.replace('T', ' ').slice(0, 16)
}

/** 生成未来 N 天的日期选项（YYYY-MM-DD） */
export function nextDays(count: number, from = new Date()): Array<{ value: string; label: string }> {
  const result: Array<{ value: string; label: string }> = []
  for (let i = 0; i < count; i++) {
    const date = new Date(from.getFullYear(), from.getMonth(), from.getDate() + i)
    const value = [
      date.getFullYear(),
      String(date.getMonth() + 1).padStart(2, '0'),
      String(date.getDate()).padStart(2, '0'),
    ].join('-')
    const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()]
    result.push({
      value,
      label: i === 0 ? '今天' : i === 1 ? '明天' : weekday,
    })
  }
  return result
}
