package org.dep.reimburse.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReimburseFormDTO {
    private String id;
    private String reimburseNo;
    private Integer status;
    private String submitDate;
    private String title;
    private String reason;
    private String reimburserId;
    private String departmentId;
    private String companyId;
    private String businessTypeId;
    private List<ItineraryItem> itineraries = new ArrayList<>();
    private List<SubsidyInfoItem> subsidies = new ArrayList<>();
    private List<AllocationItem> allocations = new ArrayList<>();
    private String remark;

    @Data
    public static class ItineraryItem {
        private String id;
        private String travelerId;
        private String travelerName;
        private String travelerNo;
        private String departCityNo;
        private String departCityName;
        private String arriveCityNo;
        private String arriveCityName;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    public static class SubsidyInfoItem {
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

    @Data
    public static class AllocationItem {
        private String id;
        private String costAttributionId;
        private String costAttributionName;
        private String projectId;
        private String projectName;
        private BigDecimal ratio;
        private BigDecimal amount;
    }
}
