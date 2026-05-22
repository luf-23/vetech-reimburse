package org.dep.reimburse.vo;

import lombok.Data;
import org.dep.reimburse.entity.ReimCompany;

@Data
public class ReimCompanyVO {
    private String reimCompanyId;
    private String reimCompanyNo;
    private String reimCompanyName;

    public static ReimCompanyVO from(ReimCompany e) {
        ReimCompanyVO vo = new ReimCompanyVO();
        vo.setReimCompanyId(e.getReimCompanyId());
        vo.setReimCompanyNo(e.getReimCompanyNo());
        vo.setReimCompanyName(e.getReimCompanyName());
        return vo;
    }
}
