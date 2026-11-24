import { request } from './http'

export interface ServerMeta {
  name: string
  displayName: string
  version: string
  profile: string
  serverTime: string
}

/** 读取后端元信息，用于首页“服务状态”卡片 */
export function fetchServerMeta(): Promise<ServerMeta> {
  return request<ServerMeta>({ url: '/v1/meta', method: 'get' })
}
