package org.dep.reimburse.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("reim_department")
public class ReimDepartment {

    @TableId
    private String reimDepartmentId;
    private String reimDepartmentNo;
    private String reimDepartmentName;
}
