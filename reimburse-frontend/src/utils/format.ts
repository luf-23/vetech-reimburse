/**
 * 格式化工具：日期、金额、百分比等展示格式。
 */

import dayjs from 'dayjs'

/** 格式化为 YYYY-MM-DD */
export function formatDate(d: string | Date): string {
  return dayjs(d).format('YYYY-MM-DD')
}

/** 格式化为 YYYY-MM-DD HH:mm:ss */
export function formatDateTime(d: string | Date): string {
  return dayjs(d).format('YYYY-MM-DD HH:mm:ss')
}

/** 金额保留两位小数 */
export function formatMoney(n: number): string {
  return n.toFixed(2)
}

/** 比例转为百分比字符串，如 0.5 → "50.00 %" */
export function formatPercent(ratio: number): string {
  return `${(ratio * 100).toFixed(2)} %`
}
