package org.dep.reimburse.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReimburseFormVO {
    private String id;
    private String reimburseNo;
    private Integer status;
    private String submitDate;
    private String title;
    private String reason;
    private String reimburserId;
    private String departmentId;
    private String companyId;
    private String businessTypeId;
    private List<ItineraryItemVO> itineraries = new ArrayList<>();
    private List<SubsidyInfoItemVO> subsidies = new ArrayList<>();
    private List<AllocationItemVO> allocations = new ArrayList<>();
    private String remark;
}
