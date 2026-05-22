package org.dep.reimbursebackend.dto.master;

import lombok.Data;
import org.dep.reimbursebackend.entity.Reimburser;

@Data
public class ReimburserDto {
    private String reimburserId;
    private String reimburserNo;
    private String reimburserName;

    public static ReimburserDto from(Reimburser e) {
        ReimburserDto dto = new ReimburserDto();
        dto.setReimburserId(e.getReimburserId());
        dto.setReimburserNo(e.getReimburserNo());
        dto.setReimburserName(e.getReimburserName());
        return dto;
    }
}
