/**
 * 报销模块工具统一出口：分摊、日期、格式化、行程、补助等。
 */

export { distributeAllocationAmounts } from '@/utils/allocation'
export { datesOverlap, getDateRange, getDaysBetween, getToday, getWeekday } from '@/utils/date'
export { formatDate, formatDateTime, formatMoney, formatPercent } from '@/utils/format'
export { genId } from '@/utils/id'
export { isItineraryDuplicate } from '@/utils/itinerary'
export {
  buildSubsidyFromItinerary,
  calcCalendarTotals,
  createSubsidyCalendar,
  normalizeSubsidyCalendar,
  syncSubsidiesWithItineraries,
} from '@/utils/subsidy'
