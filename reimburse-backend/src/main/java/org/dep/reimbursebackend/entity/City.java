package org.dep.reimbursebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "city")
public class City {

    @Id
    @Column(name = "city_no")
    private String cityNo;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "city_type")
    private String cityType;
}
