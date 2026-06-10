/**
 * 费用分摊金额计算：根据费用总额和各分摊行的比例，算出每行应摊金额。
 *
 * 核心思路（零误差分摊）：
 * - 第 2 行起按「总额 × 比例」四舍五入到分
 * - 第 1 行 = 总额 − 其余各行之和，吸收四舍五入误差，保证各行相加严格等于总额
 */

/**
 * 按分摊比例将总金额分配到各行（原地修改 allocations[].amount）。
 *
 * @param total 费用总额（来自补助合计等）
 * @param allocations 分摊行数组，每项含 ratio（比例）和 amount（输出金额）
 */
export function distributeAllocationAmounts(
  total: number,
  allocations: { ratio: number; amount: number }[],
): void {
  // 总额统一保留两位小数，避免浮点精度问题
  const normalizedTotal = +total.toFixed(2)

  if (allocations.length === 0) return

  // 只有一行时，全额给这一行，无需按比例拆分
  if (allocations.length === 1) {
    allocations[0]!.amount = normalizedTotal
    return
  }

  // 从第 2 行开始处理；第 1 行最后用「总额 − 其余之和」赋值
  const others = allocations.slice(1)
  const firstOtherRatio = others[0]!.ratio
  // 判断第 2 行及以后是否比例相同（例如都是 50%）
  const othersSameRatio = others.every((row) => row.ratio === firstOtherRatio)

  let othersAmount = 0

  if (othersSameRatio) {
    // 其余行比例相同：每行金额 = 总额 × 该比例（四舍五入）
    const unitAmount = +(normalizedTotal * firstOtherRatio).toFixed(2)
    for (let i = 1; i < allocations.length; i++) {
      allocations[i]!.amount = unitAmount
      othersAmount += unitAmount
    }
  } else {
    // 其余行比例不同：各行分别按「总额 × 各自比例」四舍五入
    for (let i = 1; i < allocations.length; i++) {
      const row = allocations[i]!
      row.amount = +(normalizedTotal * row.ratio).toFixed(2)
      othersAmount += row.amount
    }
  }

  // 第 1 行承担差额：保证 sum(amount) === normalizedTotal
  allocations[0]!.amount = +(normalizedTotal - othersAmount).toFixed(2)
}
