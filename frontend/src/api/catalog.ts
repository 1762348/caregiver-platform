import { request } from './http'

export interface Category {
  id: number
  code: string
  name: string
  description: string | null
  basePrice: number
}

export interface Schedule {
  id: number
  caregiverId: number
  caregiverName: string
  categoryId: number | null
  categoryName: string | null
  serviceDate: string
  startTime: string
  endTime: string
  timeRange: string
  remainingQuota: number
  totalQuota: number
  price: number
}

export interface Caregiver {
  id: number
  code: string
  name: string
  gender: string | null
  age: number | null
  categoryId: number | null
  categoryName: string | null
  price: number
  rating: number | null
  reviews: number | null
  qualification: string | null
  intro: string | null
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export function fetchCategories(): Promise<Category[]> {
  return request<Category[]>({ url: '/v1/categories' })
}

export function fetchSchedules(params: { date: string; categoryId?: number }): Promise<Schedule[]> {
  return request<Schedule[]>({ url: '/v1/schedules', params })
}

export function fetchCaregivers(params?: {
  categoryId?: number
  page?: number
  size?: number
}): Promise<PageResult<Caregiver>> {
  return request<PageResult<Caregiver>>({ url: '/v1/caregivers', params })
}
