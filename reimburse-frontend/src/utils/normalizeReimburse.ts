import type { DocStatus, ReimburseFormData, SubsidyDayItem, SubsidyInfoItem } from '@/types/reimburse'

function toNum(v: unknown, fallback = 0): number {
  if (v == null || v === '') return fallback
  const n = Number(v)
  return Number.isFinite(n) ? n : fallback
}

function normalizeCalendar(raw: unknown): SubsidyDayItem[] {
  if (!raw) return []
  if (Array.isArray(raw)) {
    return raw.map((day) => normalizeDay(day as SubsidyDayItem))
  }
  return []
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

/** 将后端返回的报销单详情规范为前端表单结构 */
export function normalizeReimburseForm(data: ReimburseFormData): ReimburseFormData {
  return {
    ...data,
    status: toNum(data.status, 0) as DocStatus,
    itineraries: data.itineraries ?? [],
    subsidies: (data.subsidies ?? []).map((s) => normalizeSubsidy(s as SubsidyInfoItem)),
    allocations: (data.allocations ?? []).map((a) => ({
      ...a,
      ratio: toNum(a.ratio),
      amount: toNum(a.amount),
    })),
  }
}
