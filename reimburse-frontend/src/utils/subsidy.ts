/**
 * 差旅补助计算：餐补/交通/通讯标准、日历生成、汇总及与行程同步。
 */

import type { ItineraryItem, SubsidyDayItem, SubsidyInfoItem } from '@/types/reimburse'
import { getDateRange, getDaysBetween, getWeekday } from '@/utils/date'
import { findCity } from '@/utils/masterDataLookup'

/** 根据城市类型返回餐补每日标准金额（元） */
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

/** 交通补助每日标准金额（元） */
export const TRANSPORT_STANDARD = 40
/** 通讯补助每日标准金额（元） */
export const COMM_STANDARD = 40

/** 根据城市编号查主数据，返回城市类型，默认三类城市 */
export function getCityType(cityNo: string): string {
  return findCity(cityNo)?.cityType ?? '3'
}

type SubsidyCell = SubsidyDayItem['meal']

/** 规范化单日补助单元格：未勾选时 amount 取标准值，勾选时保留实际金额 */
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

/** 规范化整个补助日历，统一处理餐补、交通、通讯三列 */
export function normalizeSubsidyCalendar(calendar: SubsidyDayItem[]): SubsidyDayItem[] {
  return calendar.map((day) => ({
    ...day,
    meal: normalizeSubsidyCell(day.meal),
    transport: normalizeSubsidyCell(day.transport),
    comm: normalizeSubsidyCell(day.comm),
  }))
}

/** 根据出差起止日期和到达城市，生成每日补助日历（含标准和默认未勾选状态） */
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

/** 汇总日历中已勾选补助的实际金额、标准合计及分项合计 */
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

/** 根据单条行程生成对应的补助信息（含日历和各项合计） */
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

/** 使补助列表与行程列表一一对应：保留已有补助，缺失的按行程自动生成 */
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
