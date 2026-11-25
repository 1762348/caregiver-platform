import { request } from './http'

export interface Receiver {
  id: number
  name: string
  idCard: string
  phone: string | null
  relation: string | null
  health: string | null
  special: string | null
  gender: string | null
  birth: string | null
}

export interface ReceiverPayload {
  name: string
  idCard: string
  phone?: string
  relation?: string
  health?: string
  special?: string
}

export function fetchReceivers(): Promise<Receiver[]> {
  return request<Receiver[]>({ url: '/v1/receivers' })
}

export function createReceiver(payload: ReceiverPayload): Promise<Receiver> {
  return request<Receiver>({ url: '/v1/receivers', method: 'post', data: payload })
}

export function updateReceiver(id: number, payload: ReceiverPayload): Promise<Receiver> {
  return request<Receiver>({ url: `/v1/receivers/${id}`, method: 'put', data: payload })
}

export function deleteReceiver(id: number): Promise<void> {
  return request<void>({ url: `/v1/receivers/${id}`, method: 'delete' })
}
