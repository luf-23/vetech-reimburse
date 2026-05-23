package org.dep.reimburse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubsidyInfoItemDTO {
    private String id;
    private String itineraryId;
    private String travelerId;
    private String travelerName;
    private String startDate;
    private String endDate;
    private Integer days;
    private String route;
    private String subsidyCityNo;
    private String subsidyCityName;
    private BigDecimal applyAmount;
    private BigDecimal subsidyAmount;
    private JsonNode calendar;
    private BigDecimal mealTotal;
    private BigDecimal transportTotal;
    private BigDecimal commTotal;
}
