import type { BusinessType, ItineraryItem, ReimburseFormData, SubsidyInfoItem } from '@/types/reimburse'
import { MASTER_DATA } from '@/data/masterData'
import { isBusinessTypeLeaf } from '@/utils/businessTypeTree'
import { datesOverlap, isItineraryDuplicate } from '@/utils/reimburse'

export interface ValidateResult {
  valid: boolean
  message: string
}

function fail(message: string): ValidateResult {
  return { valid: false, message }
}

function ok(): ValidateResult {
  return { valid: true, message: '' }
}

/** 校验补助日历中勾选金额是否在 0 ~ 标准额 范围内 */
export function validateSubsidyAmounts(subsidies: SubsidyInfoItem[]): ValidateResult {
  for (const sub of subsidies) {
    for (const day of sub.calendar) {
      if (day.meal.checked) {
        if (day.meal.amount < 0 || day.meal.amount > day.meal.standard) {
          return fail(`${sub.travelerName} ${day.date} 餐补金额不能超过标准金额`)
        }
      }
      if (day.transport.checked) {
        if (day.transport.amount < 0 || day.transport.amount > day.transport.standard) {
          return fail(`${sub.travelerName} ${day.date} 交通补助金额不能超过标准金额`)
        }
      }
      if (day.comm.checked) {
        if (day.comm.amount < 0 || day.comm.amount > day.comm.standard) {
          return fail(`${sub.travelerName} ${day.date} 通讯补助金额不能超过标准金额`)
        }
      }
    }
  }
  return ok()
}

/** 校验行程列表是否存在同人员日期重复 */
export function validateItineraryList(itineraries: ItineraryItem[]): ValidateResult {
  for (let i = 0; i < itineraries.length; i++) {
    const a = itineraries[i]!
    for (let j = i + 1; j < itineraries.length; j++) {
      const b = itineraries[j]!
      if (
        a.travelerId === b.travelerId &&
        datesOverlap(a.startDate, a.endDate, b.startDate, b.endDate)
      ) {
        return fail(`出行人员「${a.travelerName}」存在重复日期的行程，请调整`)
      }
    }
  }
  for (const it of itineraries) {
    if (!it.travelerId) return fail('补录行程出行人员不能为空')
    if (!it.departCityNo || !it.arriveCityNo) return fail('补录行程出发/到达城市不能为空')
    if (!it.startDate || !it.endDate) return fail('补录行程出差日期不能为空')
    if (!it.description?.trim()) return fail('补录行程说明不能为空')
    if (isItineraryDuplicate(itineraries, it.travelerId, it.startDate, it.endDate, it.id)) {
      return fail(`出行人员「${it.travelerName}」存在重复日期的行程，请调整`)
    }
  }
  return ok()
}

/** 补助信息必须与补录行程一一对应 */
export function validateItinerarySubsidyLink(
  itineraries: ItineraryItem[],
  subsidies: SubsidyInfoItem[],
): ValidateResult {
  if (!itineraries.length) {
    if (subsidies.length) return fail('没有补录行程时不能填写补助信息')
    return ok()
  }
  if (!subsidies.length) return ok()

  const itineraryIds = new Set(itineraries.map((it) => it.id))
  if (subsidies.length !== itineraryIds.size) {
    return fail('补助信息须与补录行程一一对应')
  }

  const linked = new Set<string>()
  for (const sub of subsidies) {
    if (!sub.itineraryId || !itineraryIds.has(sub.itineraryId)) {
      return fail('存在未关联补录行程的补助信息')
    }
    if (linked.has(sub.itineraryId)) {
      return fail('每条补录行程只能对应一条补助信息')
    }
    linked.add(sub.itineraryId)
  }
  if (linked.size !== itineraryIds.size) {
    return fail('每条补录行程均须维护补助信息')
  }
  return ok()
}

/** 5.2.2.2 基础信息：全部必填 */
export function validateBasicInfo(
  form: ReimburseFormData,
  businessTypes: BusinessType[] = MASTER_DATA.businessTypes,
): ValidateResult {
  if (!form.title?.trim()) return fail('请输入报销标题')
  if (!form.reimburserId) return fail('请选择报销人')
  if (!form.departmentId) return fail('请选择报销部门')
  if (!form.companyId) return fail('请选择费用归属公司')
  if (!form.businessTypeId) return fail('请选择业务类型')
  if (!isBusinessTypeLeaf(form.businessTypeId, businessTypes)) {
    return fail('请选择末级业务类型')
  }
  if (!form.reason?.trim()) return fail('请输入出差事由')
  if (form.title.length > 500) return fail('报销标题不可超过500字')
  if (form.reason.length > 500) return fail('出差事由不可超过500字')
  return ok()
}

export function validateReimburseForm(
  form: ReimburseFormData,
  subsidyTotal: number,
  businessTypes: BusinessType[] = MASTER_DATA.businessTypes,
): ValidateResult {
  const basicResult = validateBasicInfo(form, businessTypes)
  if (!basicResult.valid) return basicResult

  if (form.remark && form.remark.length > 1000) return fail('备注信息不可超过1000字')

  if (!form.itineraries.length) {
    if (form.subsidies.length) return fail('没有补录行程时不能填写补助信息')
    return fail('请补录行程')
  }

  const itineraryResult = validateItineraryList(form.itineraries)
  if (!itineraryResult.valid) return itineraryResult

  const linkResult = validateItinerarySubsidyLink(form.itineraries, form.subsidies)
  if (!linkResult.valid) return linkResult

  if (!form.subsidies.length) return fail('请完善补助信息')

  const subsidyAmountResult = validateSubsidyAmounts(form.subsidies)
  if (!subsidyAmountResult.valid) return subsidyAmountResult

  if (!form.allocations.length) return fail('请填写费用归属及分摊信息')

  for (let i = 0; i < form.allocations.length; i++) {
    const row = form.allocations[i]!
    if (!row.costAttributionId) {
      return fail(`第${i + 1}行费用归属不能为空`)
    }
  }

  const ratioSum = form.allocations.reduce((s, r) => s + r.ratio, 0)
  if (Math.abs(ratioSum - 1) > 0.001) {
    return fail('分摊比例合计必须为100%')
  }

  const amountSum = form.allocations.reduce((s, r) => s + r.amount, 0)
  if (+amountSum.toFixed(2) !== +subsidyTotal.toFixed(2)) {
    return fail('分摊金额合计必须等于补助总金额')
  }

  return ok()
}
