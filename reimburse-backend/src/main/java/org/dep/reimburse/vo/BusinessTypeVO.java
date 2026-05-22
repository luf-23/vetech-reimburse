package org.dep.reimburse.vo;

import lombok.Data;
import org.dep.reimburse.entity.BusinessType;

@Data
public class BusinessTypeVO {
    private String businessTypeId;
    private String businessTypeNo;
    private String businessTypeName;
    private String thereSubordinateNode;
    private String superiorId;

    public static BusinessTypeVO from(BusinessType e) {
        BusinessTypeVO vo = new BusinessTypeVO();
        vo.setBusinessTypeId(e.getBusinessTypeId());
        vo.setBusinessTypeNo(e.getBusinessTypeNo());
        vo.setBusinessTypeName(e.getBusinessTypeName());
        vo.setThereSubordinateNode(e.getThereSubordinateNode());
        vo.setSuperiorId(e.getSuperiorId());
        return vo;
    }
}
