/**
 * 日期工具：今天、天数差、日期区间、星期、区间是否重叠。
 */

import dayjs from 'dayjs'

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

/** 返回今天的日期字符串 YYYY-MM-DD */
export function getToday(): string {
  return dayjs().format('YYYY-MM-DD')
}

/** 计算起止日期之间的天数（含首尾两天） */
export function getDaysBetween(start: string, end: string): number {
  return dayjs(end).diff(dayjs(start), 'day') + 1
}

/** 生成起止日期之间每一天的日期字符串数组 */
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

/** 返回指定日期是星期几（中文） */
export function getWeekday(date: string): string {
  return WEEKDAYS[dayjs(date).day()] ?? ''
}

/** 判断两个日期区间是否有重叠（闭区间） */
export function datesOverlap(
  start1: string,
  end1: string,
  start2: string,
  end2: string,
): boolean {
  return !dayjs(end1).isBefore(dayjs(start2), 'day') && !dayjs(end2).isBefore(dayjs(start1), 'day')
}
