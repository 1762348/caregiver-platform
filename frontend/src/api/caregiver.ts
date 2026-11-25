import { request } from './http'
import type { Order, OrderPage } from './orders'

export type ProgressAction = 'depart' | 'arrive' | 'start' | 'complete'

export interface CaregiverProfile {
  id: number
  code: string
  name: string
  phone: string
  categoryName: string | null
  price: number
  rating: number | null
  reviews: number | null
  status: string
}

export interface CaregiverSchedule {
  id: number
  serviceDate: string
  timeRange: string
  totalQuota: number
  remainingQuota: number
  price: number
  status: 'OPEN' | 'CLOSED'
}

export function fetchCaregiverProfile(): Promise<CaregiverProfile> {
  return request<CaregiverProfile>({ url: '/v1/caregiver/me' })
}

export function fetchCaregiverSchedules(params?: {
  from?: string
  to?: string
}): Promise<CaregiverSchedule[]> {
  return request<CaregiverSchedule[]>({ url: '/v1/caregiver/schedules', params })
}

export function fetchCaregiverOrders(params?: {
  page?: number
  size?: number
}): Promise<OrderPage> {
  return request<OrderPage>({ url: '/v1/caregiver/orders', params })
}

export function updateOrderProgress(orderId: number, action: ProgressAction): Promise<Order> {
  return request<Order>({
    url: `/v1/caregiver/orders/${orderId}/${action}`,
    method: 'post',
  })
}
