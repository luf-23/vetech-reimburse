import type { ItineraryItem, SubsidyDayItem, SubsidyInfoItem } from '@/types/reimburse'
import { getDateRange, getDaysBetween, getWeekday } from '@/utils/date'
import { findCity } from '@/utils/masterDataLookup'

export function getMealStandard(cityType: string): number {
  switch (cityType) {
    case '1':
      return 100
    case '2':
      return 80
    case '3':
      return 50
    default:
      return 50
  }
}

export const TRANSPORT_STANDARD = 40
export const COMM_STANDARD = 40

export function getCityType(cityNo: string): string {
  return findCity(cityNo)?.cityType ?? '3'
}

type SubsidyCell = SubsidyDayItem['meal']

/** 未勾选项展示标准额（兼容后端 calendar 里 amount=0 的历史数据） */
export function normalizeSubsidyCell(cell: Partial<SubsidyCell> | undefined): SubsidyCell {
  const standard = Number(cell?.standard ?? 0)
  const checked = !!cell?.checked
  const rawAmount = Number(cell?.amount ?? 0)
  return {
    checked,
    standard,
    amount: checked ? rawAmount : standard,
  }
}

/** 打开补助日历时规范化展示：未勾选行的输入框显示默认标准额 */
export function normalizeSubsidyCalendar(calendar: SubsidyDayItem[]): SubsidyDayItem[] {
  return calendar.map((day) => ({
    ...day,
    meal: normalizeSubsidyCell(day.meal),
    transport: normalizeSubsidyCell(day.transport),
    comm: normalizeSubsidyCell(day.comm),
  }))
}

export function createSubsidyCalendar(
  startDate: string,
  endDate: string,
  arriveCityNo: string,
  arriveCityName: string,
): SubsidyDayItem[] {
  const cityType = getCityType(arriveCityNo)
  const mealStd = getMealStandard(cityType)
  return getDateRange(startDate, endDate).map((date) => ({
    date,
    weekday: getWeekday(date),
    cityNo: arriveCityNo,
    cityName: arriveCityName,
    cityType,
    meal: { checked: false, standard: mealStd, amount: mealStd },
    transport: { checked: false, standard: TRANSPORT_STANDARD, amount: TRANSPORT_STANDARD },
    comm: { checked: false, standard: COMM_STANDARD, amount: COMM_STANDARD },
  }))
}

export function calcCalendarTotals(calendar: SubsidyDayItem[]) {
  let subsidyAmount = 0
  let standardTotal = 0
  let mealTotal = 0
  let transportTotal = 0
  let commTotal = 0
  for (const day of calendar) {
    if (day.meal.checked) {
      subsidyAmount += day.meal.amount
      standardTotal += day.meal.standard
      mealTotal += day.meal.amount
    }
    if (day.transport.checked) {
      subsidyAmount += day.transport.amount
      standardTotal += day.transport.standard
      transportTotal += day.transport.amount
    }
    if (day.comm.checked) {
      subsidyAmount += day.comm.amount
      standardTotal += day.comm.standard
      commTotal += day.comm.amount
    }
  }
  return { subsidyAmount, standardTotal, mealTotal, transportTotal, commTotal }
}

export function buildSubsidyFromItinerary(it: ItineraryItem): SubsidyInfoItem {
  const days = getDaysBetween(it.startDate, it.endDate)
  const calendar = createSubsidyCalendar(it.startDate, it.endDate, it.arriveCityNo, it.arriveCityName)
  const totals = calcCalendarTotals(calendar)
  return {
    id: `sub-${it.id}`,
    itineraryId: it.id,
    travelerId: it.travelerId,
    travelerName: it.travelerName,
    startDate: it.startDate,
    endDate: it.endDate,
    days,
    route: `${it.departCityName}-${it.arriveCityName}`,
    subsidyCityNo: it.arriveCityNo,
    subsidyCityName: it.arriveCityName,
    applyAmount: totals.standardTotal,
    subsidyAmount: totals.subsidyAmount,
    calendar,
    mealTotal: totals.mealTotal,
    transportTotal: totals.transportTotal,
    commTotal: totals.commTotal,
  }
}

/** 补助仅随补录行程存在：剔除孤儿补助，并为缺失行程生成默认补助行 */
export function syncSubsidiesWithItineraries(
  itineraries: ItineraryItem[],
  subsidies: SubsidyInfoItem[],
): SubsidyInfoItem[] {
  if (!itineraries.length) return []
  const itineraryIds = new Set(itineraries.map((it) => it.id))
  const byItineraryId = new Map<string, SubsidyInfoItem>()
  for (const sub of subsidies) {
    if (sub.itineraryId && itineraryIds.has(sub.itineraryId)) {
      byItineraryId.set(sub.itineraryId, sub)
    }
  }
  return itineraries.map((it) => byItineraryId.get(it.id) ?? buildSubsidyFromItinerary(it))
}
