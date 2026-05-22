package org.dep.reimburse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("reimburse_itinerary")
public class ReimburseItinerary {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long docId;
    private String travelerId;
    private String departCityNo;
    private String arriveCityNo;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
