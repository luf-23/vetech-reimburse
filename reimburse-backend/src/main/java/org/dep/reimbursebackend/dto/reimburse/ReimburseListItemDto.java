package org.dep.reimbursebackend.dto.reimburse;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReimburseListItemDto {
    private String id;
    private String reimburseNo;
    private String docType;
    private Integer status;
    private String reimburserId;
    private String reimburserName;
    private String reimburserNo;
    private String departmentId;
    private String departmentName;
    private String departmentNo;
    private String companyId;
    private String companyName;
    private String businessTypeId;
    private String businessTypeName;
    private String title;
    private String reason;
    private BigDecimal subsidyAmount;
    private String createTime;
}
