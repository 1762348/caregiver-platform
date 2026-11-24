import { request } from './http'

export type UserRole = 'USER' | 'CAREGIVER' | 'ADMIN'

export interface UserProfile {
  id: number
  phone: string
  name: string | null
  role: UserRole
  avatar: string | null
  permissions: string[]
}

export interface LoginResult {
  token: string
  tokenType: string
  expiresIn: number
  user: UserProfile
}

export function sendSmsCode(phone: string): Promise<void> {
  return request<void>({ url: '/v1/auth/sms-code', method: 'post', data: { phone } })
}

export function login(phone: string, code: string): Promise<LoginResult> {
  return request<LoginResult>({ url: '/v1/auth/login', method: 'post', data: { phone, code } })
}

export function fetchMe(): Promise<UserProfile> {
  return request<UserProfile>({ url: '/v1/auth/me' })
}

export function refreshToken(): Promise<LoginResult> {
  return request<LoginResult>({ url: '/v1/auth/refresh', method: 'post' })
}
