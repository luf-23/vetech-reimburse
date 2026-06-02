/** 按分摊比例计算各行金额：除首行外先四舍五入到分（同比例时各行相等），差额补到首行 */
export function distributeAllocationAmounts(
  total: number,
  allocations: { ratio: number; amount: number }[],
): void {
  const normalizedTotal = +total.toFixed(2)
  if (allocations.length === 0) return
  if (allocations.length === 1) {
    allocations[0]!.amount = normalizedTotal
    return
  }

  const others = allocations.slice(1)
  const firstOtherRatio = others[0]!.ratio
  const othersSameRatio = others.every((row) => row.ratio === firstOtherRatio)

  let othersAmount = 0
  if (othersSameRatio) {
    const unitAmount = +(normalizedTotal * firstOtherRatio).toFixed(2)
    for (let i = 1; i < allocations.length; i++) {
      allocations[i]!.amount = unitAmount
      othersAmount += unitAmount
    }
  } else {
    for (let i = 1; i < allocations.length; i++) {
      const row = allocations[i]!
      row.amount = +(normalizedTotal * row.ratio).toFixed(2)
      othersAmount += row.amount
    }
  }
  allocations[0]!.amount = +(normalizedTotal - othersAmount).toFixed(2)
}
