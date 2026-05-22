export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export class ApiError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

const BASE = '/api'

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
