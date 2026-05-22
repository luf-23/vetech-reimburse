package org.dep.reimbursebackend.dto.reimburse;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReimburseFormDto {
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
    private List<ItineraryItemDto> itineraries = new ArrayList<>();
    private List<SubsidyInfoItemDto> subsidies = new ArrayList<>();
    private List<AllocationItemDto> allocations = new ArrayList<>();
    private String remark;
}
