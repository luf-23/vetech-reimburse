package org.dep.reimbursebackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "reimburse_subsidy")
public class ReimburseSubsidy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "itinerary_id")
    private Long itineraryId;

    @Column(name = "traveler_id")
    private String travelerId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private Integer days;

    private String route;

    @Column(name = "subsidy_city_no")
    private String subsidyCityNo;

    @Column(name = "apply_amount")
    private BigDecimal applyAmount;

    @Column(name = "subsidy_amount")
    private BigDecimal subsidyAmount;

    @Column(name = "meal_total")
    private BigDecimal mealTotal;

    @Column(name = "transport_total")
    private BigDecimal transportTotal;

    @Column(name = "comm_total")
    private BigDecimal commTotal;

    @Column(name = "calendar_json", columnDefinition = "json")
    private String calendarJson;
}
