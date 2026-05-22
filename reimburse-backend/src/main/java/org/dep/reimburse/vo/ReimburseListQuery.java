package org.dep.reimburse.vo;

import lombok.Data;

@Data
public class ReimburseListQuery {
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
