import { request } from './http'
import type { PageResult } from './catalog'

export type RecordStatus = 'ACTIVE' | 'INACTIVE'
export type ScheduleStatus = 'OPEN' | 'CLOSED'

export interface AdminCategory {
  id: number
  code: string
  name: string
  description: string | null
  basePrice: number
  status: RecordStatus
  caregiverCount: number
}

export interface AdminCaregiver {
  id: number
  userId: number
  code: string
  name: string
  gender: 'MALE' | 'FEMALE'
  birth: string | null
  phone: string
  categoryId: number | null
  categoryName: string | null
  price: number
  status: RecordStatus
  rating: number | null
  reviews: number | null
  createdAt: string
}

export interface AdminSchedule {
  id: number
  caregiverId: number
  caregiverName: string
  serviceDate: string
  timeRange: string
  totalQuota: number
  remainingQuota: number
  price: number
  status: ScheduleStatus
}

export interface CategoryPayload {
  code: string
  name: string
  description?: string
  basePrice: number
  status: RecordStatus
}

export interface CaregiverPayload {
  phone: string
  name: string
  gender: 'MALE' | 'FEMALE'
  birth?: string
  code: string
  categoryId?: number | null
  qualification?: string
  intro?: string
  price: number
  status?: RecordStatus
}

export interface SchedulePayload {
  caregiverId: number
  serviceDate: string
  startTime: string
  endTime: string
  totalQuota: number
  price: number
  status?: ScheduleStatus
}

export interface StopResult {
  scheduleId: number
  cancelledOrders: number
  refundTotal: number
}

export function listCategories(): Promise<AdminCategory[]> {
  return request<AdminCategory[]>({ url: '/v1/admin/categories' })
}

export function createCategory(payload: CategoryPayload): Promise<AdminCategory> {
  return request<AdminCategory>({ url: '/v1/admin/categories', method: 'post', data: payload })
}

export function updateCategory(id: number, payload: CategoryPayload): Promise<AdminCategory> {
  return request<AdminCategory>({
    url: `/v1/admin/categories/${id}`,
    method: 'put',
    data: payload,
  })
}

export function deleteCategory(id: number): Promise<void> {
  return request<void>({ url: `/v1/admin/categories/${id}`, method: 'delete' })
}

export function listCaregivers(params: {
  keyword?: string
  status?: RecordStatus
  page?: number
  size?: number
}): Promise<PageResult<AdminCaregiver>> {
  return request<PageResult<AdminCaregiver>>({ url: '/v1/admin/caregivers', params })
}

export function createCaregiver(payload: CaregiverPayload): Promise<AdminCaregiver> {
  return request<AdminCaregiver>({ url: '/v1/admin/caregivers', method: 'post', data: payload })
}

export function updateCaregiver(
  id: number,
  payload: CaregiverPayload,
): Promise<AdminCaregiver> {
  return request<AdminCaregiver>({
    url: `/v1/admin/caregivers/${id}`,
    method: 'put',
    data: payload,
  })
}

export function updateCaregiverStatus(
  id: number,
  status: RecordStatus,
): Promise<AdminCaregiver> {
  return request<AdminCaregiver>({
    url: `/v1/admin/caregivers/${id}/status`,
    method: 'post',
    params: { status },
  })
}

export function listSchedules(
  caregiverId: number,
  from: string,
  to: string,
): Promise<AdminSchedule[]> {
  return request<AdminSchedule[]>({
    url: '/v1/admin/schedules',
    params: { caregiverId, from, to },
  })
}

export function generateSchedules(payload: {
  caregiverId: number
  from: string
  to: string
  totalQuota: number
}): Promise<number> {
  return request<number>({
    url: '/v1/admin/schedules/generate',
    method: 'post',
    data: payload,
  })
}

export function createSchedule(payload: SchedulePayload): Promise<AdminSchedule> {
  return request<AdminSchedule>({ url: '/v1/admin/schedules', method: 'post', data: payload })
}

export function updateSchedule(
  id: number,
  payload: SchedulePayload,
): Promise<AdminSchedule> {
  return request<AdminSchedule>({
    url: `/v1/admin/schedules/${id}`,
    method: 'put',
    data: payload,
  })
}

export function updateQuota(id: number, remainingQuota: number): Promise<AdminSchedule> {
  return request<AdminSchedule>({
    url: `/v1/admin/schedules/${id}/quota`,
    method: 'post',
    data: { remainingQuota },
  })
}

export function stopSchedule(id: number, reason?: string): Promise<StopResult> {
  return request<StopResult>({
    url: `/v1/admin/schedules/${id}/stop`,
    method: 'post',
    data: { reason },
  })
}

export function deleteSchedule(id: number): Promise<void> {
  return request<void>({ url: `/v1/admin/schedules/${id}`, method: 'delete' })
}
