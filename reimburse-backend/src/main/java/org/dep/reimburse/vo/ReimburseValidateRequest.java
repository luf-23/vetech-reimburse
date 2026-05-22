package org.dep.reimburse.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReimburseValidateRequest extends ReimburseFormVO {
    private BigDecimal subsidyTotal;
}
