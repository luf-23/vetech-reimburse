package org.dep.reimbursebackend.dto.master;

import lombok.Data;
import org.dep.reimbursebackend.entity.City;

@Data
public class CityDto {
    private String cityNo;
    private String cityName;
    private String cityType;

    public static CityDto from(City e) {
        CityDto dto = new CityDto();
        dto.setCityNo(e.getCityNo());
        dto.setCityName(e.getCityName());
        dto.setCityType(e.getCityType());
        return dto;
    }
}
