package org.dep.reimburse.vo;

import lombok.Data;
import org.dep.reimburse.entity.Reimburser;

@Data
public class ReimburserVO {
    private String reimburserId;
    private String reimburserNo;
    private String reimburserName;

    public static ReimburserVO from(Reimburser e) {
        ReimburserVO vo = new ReimburserVO();
        vo.setReimburserId(e.getReimburserId());
        vo.setReimburserNo(e.getReimburserNo());
        vo.setReimburserName(e.getReimburserName());
        return vo;
    }
}
