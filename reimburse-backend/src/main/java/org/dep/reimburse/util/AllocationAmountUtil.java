package org.dep.reimburse.util;

import org.dep.reimburse.dto.ReimburseFormDTO.AllocationItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class AllocationAmountUtil {

    private static final int MONEY_SCALE = 2;

    private AllocationAmountUtil() {
    }

    /**
     * 按分摊比例计算各行金额：除首行外先四舍五入到分（同比例时各行相等），
     * 差额补到首行，保证合计严格等于补助总金额。
     */
    public static void distribute(BigDecimal total, List<AllocationItem> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            return;
        }
        BigDecimal normalizedTotal = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (allocations.size() == 1) {
            allocations.get(0).setAmount(normalizedTotal);
            return;
        }

        List<AllocationItem> others = allocations.subList(1, allocations.size());
        BigDecimal firstOtherRatio = defaultRatio(others.get(0));
        boolean othersSameRatio = others.stream()
                .allMatch(row -> defaultRatio(row).compareTo(firstOtherRatio) == 0);

        BigDecimal othersSum = BigDecimal.ZERO;
        if (othersSameRatio) {
            BigDecimal unitAmount = normalizedTotal.multiply(firstOtherRatio)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            for (AllocationItem row : others) {
                row.setAmount(unitAmount);
                othersSum = othersSum.add(unitAmount);
            }
        } else {
            for (AllocationItem row : others) {
                BigDecimal amount = normalizedTotal.multiply(defaultRatio(row))
                        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                row.setAmount(amount);
                othersSum = othersSum.add(amount);
            }
        }
        allocations.get(0).setAmount(normalizedTotal.subtract(othersSum));
    }

    public static BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultRatio(AllocationItem row) {
        return row.getRatio() != null ? row.getRatio() : BigDecimal.ZERO;
    }
}
