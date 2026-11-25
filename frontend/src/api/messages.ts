import { request } from './http'
import type { PageResult } from './catalog'

export type MessageType = 'ORDER' | 'PAYMENT' | 'SERVICE' | 'SYSTEM'

export interface Message {
  id: number
  title: string
  body: string
  type: MessageType
  deeplink: string | null
  read: boolean
  createdAt: string
}

export function fetchMessages(params?: {
  page?: number
  size?: number
}): Promise<PageResult<Message>> {
  return request<PageResult<Message>>({ url: '/v1/messages', params })
}

export function fetchUnreadCount(): Promise<number> {
  return request<number>({ url: '/v1/messages/unread-count' })
}

export function markMessageRead(id: number): Promise<void> {
  return request<void>({ url: `/v1/messages/${id}/read`, method: 'post' })
}

export function markAllMessagesRead(): Promise<number> {
  return request<number>({ url: '/v1/messages/read-all', method: 'post' })
}
