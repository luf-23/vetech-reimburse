/**
 * 唯一 ID 生成：时间戳加随机串，用于前端临时行标识。
 */

/** 生成前端临时唯一 ID */
export function genId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}
