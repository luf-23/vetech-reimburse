/**
 * 行程相关工具：判断同一出行人是否存在日期重叠的重复行程。
 */

import type { ItineraryItem } from '@/types/reimburse'
import { datesOverlap } from '@/utils/date'

/** 检查列表中是否已有同一出行人、日期区间重叠的另一条行程（可排除当前编辑行） */
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
