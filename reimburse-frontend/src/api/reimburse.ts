/**
 * 差旅报销相关 API：列表、详情、增删改查及服务端校验。
 */

import { request } from './http'
import type { ListQuery, ReimburseFormData, ReimburseListItem } from '@/types/reimburse'
import { enrichListItem } from '@/utils/enrichReimburse'
import { normalizeReimburseForm } from '@/utils/normalizeReimburse'

/** 分页查询结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 列表查询参数（含分页） */
export interface ListParams extends ListQuery {
  page?: number
  size?: number
}

/** 服务端校验接口返回 */
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

/** 分页查询报销单列表，并补全主数据名称 */
export async function fetchReimburseList(params: ListParams) {
  const data = await request<PageResult<ReimburseListItem>>(`/reimburse/list${toQuery(params)}`)
  return {
    ...data,
    records: data.records.map(normalizeListItem),
  }
}

/** 根据 ID 获取报销单详情，并规范化表单数据 */
export async function fetchReimburseDetail(id: string) {
  const data = await request<ReimburseFormData>(`/reimburse/detail/${id}`)
  return normalizeReimburseForm(data)
}

/** 新建报销单 */
export function createReimburse(form: ReimburseFormData) {
  return request<ReimburseFormData>('/reimburse/create', {
    method: 'POST',
    body: JSON.stringify(form),
  })
}

/** 更新已有报销单 */
export function updateReimburse(id: string, form: ReimburseFormData) {
  return request<ReimburseFormData>(`/reimburse/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(form),
  })
}

/** 删除报销单 */
export function deleteReimburse(id: string) {
  return request<void>(`/reimburse/delete/${id}`, { method: 'DELETE' })
}

/** 复制报销单，生成新单据 */
export function copyReimburse(id: string) {
  return request<ReimburseListItem>(`/reimburse/copy/${id}`, { method: 'POST' })
}

/** 提交前调用服务端校验，传入表单及补助合计金额 */
export async function validateReimburseOnServer(
  form: ReimburseFormData,
  subsidyTotal: number,
): Promise<ApiValidateResponse> {
  return request<ApiValidateResponse>('/reimburse/validate', {
    method: 'POST',
    body: JSON.stringify({ ...form, subsidyTotal }),
  })
}
