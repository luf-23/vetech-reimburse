package org.dep.reimbursebackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "reimburse_itinerary")
public class ReimburseItinerary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "traveler_id")
    private String travelerId;

    @Column(name = "depart_city_no")
    private String departCityNo;

    @Column(name = "arrive_city_no")
    private String arriveCityNo;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String description;
}
