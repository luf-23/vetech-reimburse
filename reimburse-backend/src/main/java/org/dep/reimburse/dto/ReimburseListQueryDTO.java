package org.dep.reimburse.dto;

import lombok.Data;

@Data
public class ReimburseListQueryDTO {
    private String reimburseNo = "";
    private String title = "";
    private String reason = "";
    private String companyId = "";
    private String departmentId = "";
    private String reimburserId = "";
    private String businessTypeId = "";
    private int page = 1;
    private int size = 10;
}
