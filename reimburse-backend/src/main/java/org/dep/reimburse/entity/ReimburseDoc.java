package org.dep.reimburse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("reimburse_doc")
public class ReimburseDoc {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String reimburseNo;
    private String docType;
    private Integer status;
    private String reimburserId;
    private String departmentId;
    private String companyId;
    private String businessTypeId;
    private String title;
    private String reason;
    private BigDecimal subsidyAmount;
    private LocalDate submitDate;
    private LocalDate createTime;
    private String remark;
}
