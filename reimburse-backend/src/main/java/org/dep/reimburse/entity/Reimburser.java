package org.dep.reimburse.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("reimburser")
public class Reimburser {

    @TableId
    private String reimburserId;
    private String reimburserNo;
    private String reimburserName;
}
