package org.dep.reimbursebackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "reimburse_allocation")
public class ReimburseAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "cost_attribution_id")
    private String costAttributionId;

    @Column(name = "project_id")
    private String projectId;

    private BigDecimal ratio;

    private BigDecimal amount;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
