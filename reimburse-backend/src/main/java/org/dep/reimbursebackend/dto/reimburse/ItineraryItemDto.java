package org.dep.reimbursebackend.dto.reimburse;

import lombok.Data;

@Data
public class ItineraryItemDto {
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
