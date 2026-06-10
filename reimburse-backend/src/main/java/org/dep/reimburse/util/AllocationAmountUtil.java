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
        // 财务金额统一保留两位小数，避免后续比较和入库出现精度不一致
        BigDecimal normalizedTotal = total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (allocations.size() == 1) {
            // 只有一条分摊时，直接承担全部金额
            allocations.get(0).setAmount(normalizedTotal);
            return;
        }

        // 从第二行开始先按比例计算，最后把四舍五入差额补到第一行
        List<AllocationItem> others = allocations.subList(1, allocations.size());
        BigDecimal firstOtherRatio = defaultRatio(others.get(0));
        boolean othersSameRatio = others.stream()
                .allMatch(row -> defaultRatio(row).compareTo(firstOtherRatio) == 0);

        BigDecimal othersSum = BigDecimal.ZERO;
        if (othersSameRatio) {
            // 多条相同比例分摊时，先算统一金额，确保相同比例行金额一致
            BigDecimal unitAmount = normalizedTotal.multiply(firstOtherRatio)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            for (AllocationItem row : others) {
                row.setAmount(unitAmount);
                othersSum = othersSum.add(unitAmount);
            }
        } else {
            // 比例不完全相同时，逐行按各自比例计算金额
            for (AllocationItem row : others) {
                BigDecimal amount = normalizedTotal.multiply(defaultRatio(row))
                        .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                row.setAmount(amount);
                othersSum = othersSum.add(amount);
            }
        }
        // 第一行吸收舍入差额，保证所有分摊金额合计严格等于补助总金额
        allocations.get(0).setAmount(normalizedTotal.subtract(othersSum));
    }

    public static BigDecimal normalizeMoney(BigDecimal value) {
        // 金额比较前统一保留两位小数，和数据库 DECIMAL(12,2) 对齐
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal defaultRatio(AllocationItem row) {
        return row.getRatio() != null ? row.getRatio() : BigDecimal.ZERO;
    }
}
