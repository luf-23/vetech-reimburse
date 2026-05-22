package org.dep.reimburse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("reimburse_allocation")
public class ReimburseAllocation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private String costAttributionId;
    private String projectId;
    private BigDecimal ratio;
    private BigDecimal amount;
    private Integer sortOrder;
}
