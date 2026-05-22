import { request } from './http'
import type { ListQuery, ReimburseFormData, ReimburseListItem } from '@/types/reimburse'

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
  return {
    ...row,
    subsidyAmount: Number(row.subsidyAmount),
  }
}

export async function fetchReimburseList(params: ListParams) {
  const data = await request<PageResult<ReimburseListItem>>(`/reimburse${toQuery(params)}`)
  return {
    ...data,
    records: data.records.map(normalizeListItem),
  }
}

export function fetchReimburseDetail(id: string) {
  return request<ReimburseFormData>(`/reimburse/${id}`)
}

export function createReimburse(form: ReimburseFormData) {
  return request<ReimburseFormData>('/reimburse', {
    method: 'POST',
    body: JSON.stringify(form),
  })
}

export function updateReimburse(id: string, form: ReimburseFormData) {
  return request<ReimburseFormData>(`/reimburse/${id}`, {
    method: 'PUT',
    body: JSON.stringify(form),
  })
}

export function deleteReimburse(id: string) {
  return request<void>(`/reimburse/${id}`, { method: 'DELETE' })
}

export function copyReimburse(id: string) {
  return request<ReimburseListItem>(`/reimburse/${id}/copy`, { method: 'POST' })
}
