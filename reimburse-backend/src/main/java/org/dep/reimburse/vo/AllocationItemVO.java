package org.dep.reimburse.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AllocationItemVO {
    private String id;
    private String costAttributionId;
    private String costAttributionName;
    private String projectId;
    private String projectName;
    private BigDecimal ratio;
    private BigDecimal amount;
}
