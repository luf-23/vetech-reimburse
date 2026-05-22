import type { ReimburseFormData } from '@/types/reimburse'

export interface ApiValidateResponse {
  valid: boolean
  message: string
}

/** 5.2.2.9 提交时调用后台校验 */
export async function validateReimburseOnServer(
  form: ReimburseFormData,
  subsidyTotal: number,
): Promise<ApiValidateResponse> {
  const res = await fetch('/api/reimburse/validate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ ...form, subsidyTotal }),
  })
  if (!res.ok) {
    throw new Error(`校验接口异常(${res.status})`)
  }
  return res.json() as Promise<ApiValidateResponse>
}
