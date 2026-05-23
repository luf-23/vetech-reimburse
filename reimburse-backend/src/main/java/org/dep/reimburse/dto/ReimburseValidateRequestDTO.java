package org.dep.reimburse.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class ReimburseValidateRequestDTO extends ReimburseFormDTO {
    private BigDecimal subsidyTotal;
}
