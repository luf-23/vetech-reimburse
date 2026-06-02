import dayjs from 'dayjs'

const WEEKDAYS = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

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

export function datesOverlap(
  start1: string,
  end1: string,
  start2: string,
  end2: string,
): boolean {
  return !dayjs(end1).isBefore(dayjs(start2), 'day') && !dayjs(end2).isBefore(dayjs(start1), 'day')
}
