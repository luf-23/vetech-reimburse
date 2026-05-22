package org.dep.reimburse.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("reim_company")
public class ReimCompany {

    @TableId
    private String reimCompanyId;
    private String reimCompanyNo;
    private String reimCompanyName;
}
