package org.dep.reimbursebackend.dto.master;

import lombok.Data;
import org.dep.reimbursebackend.entity.ReimCompany;

@Data
public class ReimCompanyDto {
    private String reimCompanyId;
    private String reimCompanyNo;
    private String reimCompanyName;

    public static ReimCompanyDto from(ReimCompany e) {
        ReimCompanyDto dto = new ReimCompanyDto();
        dto.setReimCompanyId(e.getReimCompanyId());
        dto.setReimCompanyNo(e.getReimCompanyNo());
        dto.setReimCompanyName(e.getReimCompanyName());
        return dto;
    }
}
