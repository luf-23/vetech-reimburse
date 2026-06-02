/** 报销业务工具 barrel，便于统一引用 */
export { distributeAllocationAmounts } from '@/utils/allocation'
export { datesOverlap, getDateRange, getDaysBetween, getToday, getWeekday } from '@/utils/date'
export { formatDate, formatDateTime, formatMoney, formatPercent } from '@/utils/format'
export { genId } from '@/utils/id'
export { isItineraryDuplicate } from '@/utils/itinerary'
export {
  buildSubsidyFromItinerary,
  calcCalendarTotals,
  createSubsidyCalendar,
  syncSubsidiesWithItineraries,
} from '@/utils/subsidy'
