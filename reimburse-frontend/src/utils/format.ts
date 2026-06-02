import dayjs from 'dayjs'

export function formatDate(d: string | Date): string {
  return dayjs(d).format('YYYY-MM-DD')
}

export function formatDateTime(d: string | Date): string {
  return dayjs(d).format('YYYY-MM-DD HH:mm:ss')
}

export function formatMoney(n: number): string {
  return n.toFixed(2)
}

export function formatPercent(ratio: number): string {
  return `${(ratio * 100).toFixed(2)} %`
}
