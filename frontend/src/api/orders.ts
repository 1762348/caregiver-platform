import { request } from './http'

export type OrderStatus = 'PENDING_PAYMENT' | 'PAID' | 'IN_SERVICE' | 'COMPLETED' | 'CANCELLED'

export type OrderStage =
  | 'PENDING_PAYMENT'
  | 'PENDING_DEPARTURE'
  | 'ON_THE_WAY'
  | 'ARRIVED'
  | 'IN_SERVICE'
  | 'COMPLETED'
  | 'CANCELLED'

export type PaymentChannel = 'WECHAT' | 'ALIPAY' | 'MOCK'

export interface Order {
  id: number
  orderNo: string
  status: OrderStatus
  stage: OrderStage
  amount: number
  serviceDate: string
  serviceTime: string
  caregiverId: number
  caregiverName: string | null
  caregiverPhone: string | null
  categoryName: string | null
  receiverId: number
  receiverName: string | null
  createdAt: string
  payTime: string | null
  cancelTime: string | null
  departedAt: string | null
  arrivedAt: string | null
  startedAt: string | null
  completedAt: string | null
  cancelReason: string | null
  canPay: boolean
  canCancel: boolean
}

export interface OrderPage {
  items: Order[]
  total: number
  page: number
  size: number
}

export interface CancelResult {
  order: Order
  refundAmount: number
  feeApplied: boolean
}

export function createOrder(payload: {
  scheduleId: number
  receiverId: number
}): Promise<Order> {
  return request<Order>({ url: '/v1/orders', method: 'post', data: payload })
}

export function fetchOrders(params?: {
  status?: OrderStatus
  page?: number
  size?: number
}): Promise<OrderPage> {
  return request<OrderPage>({ url: '/v1/orders', params })
}

export function fetchOrder(id: number): Promise<Order> {
  return request<Order>({ url: `/v1/orders/${id}` })
}

export function payOrder(id: number, channel: PaymentChannel): Promise<Order> {
  return request<Order>({
    url: `/v1/orders/${id}/pay`,
    method: 'post',
    data: { channel },
  })
}

export function cancelOrder(id: number, reason?: string): Promise<CancelResult> {
  return request<CancelResult>({
    url: `/v1/orders/${id}/cancel`,
    method: 'post',
    data: { reason },
  })
}
