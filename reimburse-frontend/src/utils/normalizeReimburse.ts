/**
 * 报销表单数据规范化：将接口返回的松散数据转为类型安全、结构完整的表单对象。
 */

import type { DocStatus, ReimburseFormData, SubsidyDayItem, SubsidyInfoItem } from '@/types/reimburse'
import { enrichReimburseForm } from '@/utils/enrichReimburse'
import { normalizeSubsidyCalendar, syncSubsidiesWithItineraries } from '@/utils/subsidy'

function toNum(v: unknown, fallback = 0): number {
  if (v == null || v === '') return fallback
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

function normalizeCalendar(raw: unknown): SubsidyDayItem[] {
  if (!raw || !Array.isArray(raw)) return []
  return normalizeSubsidyCalendar(raw.map((day) => normalizeDay(day as SubsidyDayItem)))
}

function normalizeDay(day: SubsidyDayItem): SubsidyDayItem {
  return {
    date: day.date,
    weekday: day.weekday ?? '',
    cityNo: day.cityNo,
    cityName: day.cityName,
    cityType: String(day.cityType ?? '3'),
    meal: {
      checked: !!day.meal?.checked,
      standard: toNum(day.meal?.standard),
      amount: toNum(day.meal?.amount),
    },
    transport: {
      checked: !!day.transport?.checked,
      standard: toNum(day.transport?.standard),
      amount: toNum(day.transport?.amount),
    },
    comm: {
      checked: !!day.comm?.checked,
      standard: toNum(day.comm?.standard),
      amount: toNum(day.comm?.amount),
    },
    isPlanned: day.isPlanned,
  }
}

function normalizeSubsidy(sub: SubsidyInfoItem): SubsidyInfoItem {
  return {
    ...sub,
    days: toNum(sub.days),
    applyAmount: toNum(sub.applyAmount),
    subsidyAmount: toNum(sub.subsidyAmount),
    mealTotal: toNum(sub.mealTotal),
    transportTotal: toNum(sub.transportTotal),
    commTotal: toNum(sub.commTotal),
    calendar: normalizeCalendar(sub.calendar as unknown),
  }
}

/** 规范化整张报销表单：数字字段、补助日历、行程与补助同步，并补全主数据名称 */
export function normalizeReimburseForm(data: ReimburseFormData): ReimburseFormData {
  const itineraries = data.itineraries ?? []
  const subsidies = syncSubsidiesWithItineraries(
    itineraries,
    (data.subsidies ?? []).map((s) => normalizeSubsidy(s as SubsidyInfoItem)),
  )
  return enrichReimburseForm({
    ...data,
    status: toNum(data.status, 0) as DocStatus,
    itineraries,
    subsidies,
    allocations: (data.allocations ?? []).map((a) => ({
      ...a,
      ratio: toNum(a.ratio),
      amount: toNum(a.amount),
    })),
  })
}
