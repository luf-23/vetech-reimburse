/**
 * 业务常量：单据状态码与中文名称映射。
 */

/** 报销单状态：数字 → 展示文案 */
export const DOC_STATUS_MAP: Record<number, string> = {
  0: '草稿',
  1: '审批通过',
  2: '已作废',
  3: '审批中',
}
