/**
 * HTTP 请求封装：统一调用后端 /api 接口，解析标准响应格式并抛出业务错误。
 */

/** 后端统一响应结构 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** API 请求失败时抛出的错误类型 */
export class ApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

const BASE = '/api'

/** 发送 JSON 请求，成功时返回 data 字段，失败时抛出 ApiError */
export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...init?.headers },
    ...init,
  })
  if (!res.ok) {
    throw new ApiError(`请求失败: ${res.status}`)
  }
  const body = (await res.json()) as ApiResponse<T>
  if (body.code !== 0) {
    throw new ApiError(body.message || '请求失败')
  }
  return body.data
}
