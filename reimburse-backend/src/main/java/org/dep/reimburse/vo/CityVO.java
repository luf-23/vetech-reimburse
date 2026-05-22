package org.dep.reimburse.vo;

import lombok.Data;
import org.dep.reimburse.entity.City;

@Data
public class CityVO {
    private String cityNo;
    private String cityName;
    private String cityType;

    public static CityVO from(City e) {
        CityVO vo = new CityVO();
        vo.setCityNo(e.getCityNo());
        vo.setCityName(e.getCityName());
        vo.setCityType(e.getCityType());
        return vo;
    }
}
