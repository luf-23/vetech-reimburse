package org.dep.reimburse.vo;

import lombok.Data;
import org.dep.reimburse.entity.ReimDepartment;

@Data
public class ReimDepartmentVO {
    private String reimDepartmentId;
    private String reimDepartmentNo;
    private String reimDepartmentName;

    public static ReimDepartmentVO from(ReimDepartment e) {
        ReimDepartmentVO vo = new ReimDepartmentVO();
        vo.setReimDepartmentId(e.getReimDepartmentId());
        vo.setReimDepartmentNo(e.getReimDepartmentNo());
        vo.setReimDepartmentName(e.getReimDepartmentName());
        return vo;
    }
}
