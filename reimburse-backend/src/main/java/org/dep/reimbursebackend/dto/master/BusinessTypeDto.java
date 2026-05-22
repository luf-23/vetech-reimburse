package org.dep.reimbursebackend.dto.master;

import lombok.Data;
import org.dep.reimbursebackend.entity.BusinessType;

@Data
public class BusinessTypeDto {
    private String businessTypeId;
    private String businessTypeNo;
    private String businessTypeName;
    private String thereSubordinateNode;
    private String superiorId;

    public static BusinessTypeDto from(BusinessType e) {
        BusinessTypeDto dto = new BusinessTypeDto();
        dto.setBusinessTypeId(e.getBusinessTypeId());
        dto.setBusinessTypeNo(e.getBusinessTypeNo());
        dto.setBusinessTypeName(e.getBusinessTypeName());
        dto.setThereSubordinateNode(e.getThereSubordinateNode());
        dto.setSuperiorId(e.getSuperiorId());
        return dto;
    }
}
