package org.dep.reimbursebackend.dto.master;

import lombok.Data;
import org.dep.reimbursebackend.entity.ReimDepartment;

@Data
public class ReimDepartmentDto {
    private String reimDepartmentId;
    private String reimDepartmentNo;
    private String reimDepartmentName;

    public static ReimDepartmentDto from(ReimDepartment e) {
        ReimDepartmentDto dto = new ReimDepartmentDto();
        dto.setReimDepartmentId(e.getReimDepartmentId());
        dto.setReimDepartmentNo(e.getReimDepartmentNo());
        dto.setReimDepartmentName(e.getReimDepartmentName());
        return dto;
    }
}
