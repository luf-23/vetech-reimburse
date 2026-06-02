import type { ItineraryItem } from '@/types/reimburse'
import { datesOverlap } from '@/utils/date'

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
