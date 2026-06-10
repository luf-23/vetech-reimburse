package org.dep.reimburse.util;

import org.dep.reimburse.dto.ReimburseFormDTO;
import org.dep.reimburse.dto.ReimburseFormDTO.ItineraryItem;
import org.dep.reimburse.dto.ReimburseFormDTO.SubsidyInfoItem;
import org.dep.reimburse.vo.ValidateResultVO;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReimburseFormValidator {

    /** 与前端一致：ratio 为 0~1 小数，合计应为 1 */
    private static final BigDecimal RATIO_SUM_TARGET = BigDecimal.ONE;
    private static final BigDecimal RATIO_SUM_TOLERANCE = new BigDecimal("0.001");

    private ReimburseFormValidator() {
    }

    public static ValidateResultVO validate(ReimburseFormDTO form, BigDecimal subsidyTotal) {
        // 先校验主表必填项，保证单据基础信息完整
        if (!StringUtils.hasText(form.getTitle())) {
            return ValidateResultVO.fail("请输入报销标题");
        }
        if (!StringUtils.hasText(form.getReimburserId())) {
            return ValidateResultVO.fail("请选择报销人");
        }
        if (!StringUtils.hasText(form.getDepartmentId())) {
            return ValidateResultVO.fail("请选择报销部门");
        }
        if (!StringUtils.hasText(form.getCompanyId())) {
            return ValidateResultVO.fail("请选择费用归属公司");
        }
        if (!StringUtils.hasText(form.getBusinessTypeId())) {
            return ValidateResultVO.fail("请选择业务类型");
        }
        if (!StringUtils.hasText(form.getReason())) {
            return ValidateResultVO.fail("请输入出差事由");
        }
        // 字段长度与数据库表结构保持一致，避免入库时字段超长
        if (form.getTitle().length() > 500) {
            return ValidateResultVO.fail("报销标题不可超过500字");
        }
        if (form.getReason().length() > 500) {
            return ValidateResultVO.fail("出差事由不可超过500字");
        }
        if (form.getRemark() != null && form.getRemark().length() > 1000) {
            return ValidateResultVO.fail("备注信息不可超过1000字");
        }

        List<ItineraryItem> itineraries = form.getItineraries() != null ? form.getItineraries() : List.of();
        List<SubsidyInfoItem> subsidies = form.getSubsidies() != null ? form.getSubsidies() : List.of();

        // 报销单必须至少包含一条行程，补助信息也必须基于行程生成
        if (itineraries.isEmpty()) {
            return ValidateResultVO.fail("请补录行程");
        }

        // 校验行程和补助的一一对应关系，防止补助挂错或重复挂到同一行程
        ValidateResultVO itinerarySubsidy = validateItinerarySubsidyLink(itineraries, subsidies);
        if (!itinerarySubsidy.isValid()) {
            return itinerarySubsidy;
        }

        // 校验每条行程自身的必填字段
        for (ItineraryItem it : itineraries) {
            if (!StringUtils.hasText(it.getTravelerId())) {
                return ValidateResultVO.fail("补录行程出行人员不能为空");
            }
            if (!StringUtils.hasText(it.getDepartCityNo()) || !StringUtils.hasText(it.getArriveCityNo())) {
                return ValidateResultVO.fail("补录行程出发/到达城市不能为空");
            }
            if (!StringUtils.hasText(it.getStartDate()) || !StringUtils.hasText(it.getEndDate())) {
                return ValidateResultVO.fail("补录行程出差日期不能为空");
            }
            if (!StringUtils.hasText(it.getDescription())) {
                return ValidateResultVO.fail("补录行程说明不能为空");
            }
        }

        if (subsidies.isEmpty()) {
            return ValidateResultVO.fail("请完善补助信息");
        }

        if (form.getAllocations() == null || form.getAllocations().isEmpty()) {
            return ValidateResultVO.fail("请填写费用归属及分摊信息");
        }

        // 分摊比例使用 0~1 小数表示，合计应为 1，也就是前端显示的 100%
        BigDecimal ratioSum = form.getAllocations().stream()
                .map(a -> a.getRatio() != null ? a.getRatio() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (ratioSum.subtract(RATIO_SUM_TARGET).abs().compareTo(RATIO_SUM_TOLERANCE) > 0) {
            return ValidateResultVO.fail("分摊比例合计必须为100%");
        }

        if (subsidyTotal != null) {
            // 分摊金额合计必须等于补助总金额，保证财务金额前后一致
            BigDecimal amountSum = form.getAllocations().stream()
                    .map(a -> a.getAmount() != null ? a.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (AllocationAmountUtil.normalizeMoney(amountSum)
                    .compareTo(AllocationAmountUtil.normalizeMoney(subsidyTotal)) != 0) {
                return ValidateResultVO.fail("分摊金额合计必须等于补助总金额");
            }
        }

        return ValidateResultVO.ok();
    }

    public static ValidateResultVO validateItinerarySubsidyLink(
            List<ItineraryItem> itineraries,
            List<SubsidyInfoItem> subsidies
    ) {
        if (itineraries.isEmpty()) {
            if (!subsidies.isEmpty()) {
                return ValidateResultVO.fail("没有补录行程时不能填写补助信息");
            }
            return ValidateResultVO.ok();
        }
        if (subsidies.isEmpty()) {
            return ValidateResultVO.ok();
        }

        // 收集前端行程 id，后续用于判断补助是否关联了本单据内的有效行程
        Set<String> itineraryIds = new HashSet<>();
        for (ItineraryItem it : itineraries) {
            if (StringUtils.hasText(it.getId())) {
                itineraryIds.add(it.getId());
            }
        }
        if (itineraryIds.isEmpty()) {
            return ValidateResultVO.fail("补录行程数据异常");
        }
        if (subsidies.size() != itineraryIds.size()) {
            return ValidateResultVO.fail("补助信息须与补录行程一一对应");
        }

        // linkedItineraryIds 用于识别重复关联，并最终确认每条行程都维护了补助
        Set<String> linkedItineraryIds = new HashSet<>();
        for (SubsidyInfoItem sub : subsidies) {
            if (!StringUtils.hasText(sub.getItineraryId())) {
                return ValidateResultVO.fail("补助信息必须关联补录行程");
            }
            if (!itineraryIds.contains(sub.getItineraryId())) {
                return ValidateResultVO.fail("存在未关联补录行程的补助信息");
            }
            if (!linkedItineraryIds.add(sub.getItineraryId())) {
                return ValidateResultVO.fail("每条补录行程只能对应一条补助信息");
            }
        }
        if (!linkedItineraryIds.equals(itineraryIds)) {
            return ValidateResultVO.fail("每条补录行程均须维护补助信息");
        }
        return ValidateResultVO.ok();
    }
}
