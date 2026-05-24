import dayjs from 'dayjs'
import { findCity } from '@/data/masterData'
import type { ItineraryItem, SubsidyDayItem, SubsidyInfoItem } from '@/types/reimburse'

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

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

export function formatDate(d: string | Date): string {
  return dayjs(d).format('YYYY-MM-DD')
}

export function formatDateTime(d: string | Date): string {
  return dayjs(d).format('YYYY-MM-DD HH:mm:ss')
}

export function getToday(): string {
  return dayjs().format('YYYY-MM-DD')
}

export function getDaysBetween(start: string, end: string): number {
  return dayjs(end).diff(dayjs(start), 'day') + 1
}

export function getDateRange(start: string, end: string): string[] {
  const dates: string[] = []
  let cur = dayjs(start)
  const endD = dayjs(end)
  while (cur.isBefore(endD) || cur.isSame(endD, 'day')) {
    dates.push(cur.format('YYYY-MM-DD'))
    cur = cur.add(1, 'day')
  }
  return dates
}

export function getWeekday(date: string): string {
  return WEEKDAYS[dayjs(date).day()] ?? ''
}

export function getCityType(cityNo: string): string {
  return findCity(cityNo)?.cityType ?? '3'
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

export function datesOverlap(
  start1: string,
  end1: string,
  start2: string,
  end2: string,
): boolean {
  return !dayjs(end1).isBefore(dayjs(start2), 'day') && !dayjs(end2).isBefore(dayjs(start1), 'day')
}

export function isItineraryDuplicate(
  list: ItineraryItem[],
  travelerId: string,
  startDate: string,
  endDate: string,
  excludeId?: string,
): boolean {
  return list.some(
    (it) =>
      it.travelerId === travelerId &&
      it.id !== excludeId &&
      datesOverlap(it.startDate, it.endDate, startDate, endDate),
  )
}

export function genId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

export function formatMoney(n: number): string {
  return n.toFixed(2)
}

export function formatPercent(ratio: number): string {
  return `${(ratio * 100).toFixed(2)} %`
}

/** 按分摊比例计算各行金额：除首行外先四舍五入到分（同比例时各行相等），差额补到首行 */
export function distributeAllocationAmounts(
  total: number,
  allocations: { ratio: number; amount: number }[],
): void {
  const normalizedTotal = +total.toFixed(2)
  if (allocations.length === 0) return
  if (allocations.length === 1) {
    allocations[0]!.amount = normalizedTotal
    return
  }

  const others = allocations.slice(1)
  const firstOtherRatio = others[0]!.ratio
  const othersSameRatio = others.every((row) => row.ratio === firstOtherRatio)

  let othersAmount = 0
  if (othersSameRatio) {
    const unitAmount = +(normalizedTotal * firstOtherRatio).toFixed(2)
    for (let i = 1; i < allocations.length; i++) {
      allocations[i]!.amount = unitAmount
      othersAmount += unitAmount
    }
  } else {
    for (let i = 1; i < allocations.length; i++) {
      const row = allocations[i]!
      row.amount = +(normalizedTotal * row.ratio).toFixed(2)
      othersAmount += row.amount
    }
  }
  allocations[0]!.amount = +(normalizedTotal - othersAmount).toFixed(2)
}
