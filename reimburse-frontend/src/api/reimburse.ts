import { request } from './http'
import type { ListQuery, ReimburseFormData, ReimburseListItem } from '@/types/reimburse'
import { enrichListItem } from '@/data/masterData'
import { normalizeReimburseForm } from '@/utils/normalizeReimburse'

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface ListParams extends ListQuery {
  page?: number
  size?: number
}

export interface ApiValidateResponse {
  valid: boolean
  message: string
}

function toQuery(params: ListParams): string {
  const q = new URLSearchParams()
  if (params.reimburseNo) q.set('reimburseNo', params.reimburseNo)
  if (params.title) q.set('title', params.title)
  if (params.reason) q.set('reason', params.reason)
  if (params.companyId) q.set('companyId', params.companyId)
  if (params.departmentId) q.set('departmentId', params.departmentId)
  if (params.reimburserId) q.set('reimburserId', params.reimburserId)
  if (params.businessTypeId) q.set('businessTypeId', params.businessTypeId)
  q.set('page', String(params.page ?? 1))
  q.set('size', String(params.size ?? 10))
  const s = q.toString()
  return s ? `?${s}` : ''
}

function normalizeListItem(row: ReimburseListItem): ReimburseListItem {
  return enrichListItem({
    ...row,
    status: Number(row.status) as ReimburseListItem['status'],
    subsidyAmount: Number(row.subsidyAmount),
  })
}

export async function fetchReimburseList(params: ListParams) {
  const data = await request<PageResult<ReimburseListItem>>(`/reimburse/list${toQuery(params)}`)
  return {
    ...data,
    records: data.records.map(normalizeListItem),
  }
}

export async function fetchReimburseDetail(id: string) {
  const data = await request<ReimburseFormData>(`/reimburse/detail/${id}`)
  return normalizeReimburseForm(data)
}

export function createReimburse(form: ReimburseFormData) {
  return request<ReimburseFormData>('/reimburse/create', {
    method: 'POST',
    body: JSON.stringify(form),
  })
}

export function updateReimburse(id: string, form: ReimburseFormData) {
  return request<ReimburseFormData>(`/reimburse/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(form),
  })
}

export function deleteReimburse(id: string) {
  return request<void>(`/reimburse/delete/${id}`, { method: 'DELETE' })
}

export function copyReimburse(id: string) {
  return request<ReimburseListItem>(`/reimburse/copy/${id}`, { method: 'POST' })
}

/** 5.2.2.9 提交时调用后台校验 */
export async function validateReimburseOnServer(
  form: ReimburseFormData,
  subsidyTotal: number,
): Promise<ApiValidateResponse> {
  return request<ApiValidateResponse>('/reimburse/validate', {
    method: 'POST',
    body: JSON.stringify({ ...form, subsidyTotal }),
  })
}
