package org.dep.reimburse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("reimburse_subsidy")
public class ReimburseSubsidy {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private Long itineraryId;
    private String travelerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private String route;
    private String subsidyCityNo;
    private BigDecimal applyAmount;
    private BigDecimal subsidyAmount;
    private BigDecimal mealTotal;
    private BigDecimal transportTotal;
    private BigDecimal commTotal;
    private String calendarJson;
}
