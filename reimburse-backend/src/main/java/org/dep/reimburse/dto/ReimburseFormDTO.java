package org.dep.reimburse.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReimburseFormDTO {
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
    private List<ItineraryItemDTO> itineraries = new ArrayList<>();
    private List<SubsidyInfoItemDTO> subsidies = new ArrayList<>();
    private List<AllocationItemDTO> allocations = new ArrayList<>();
    private String remark;
}
