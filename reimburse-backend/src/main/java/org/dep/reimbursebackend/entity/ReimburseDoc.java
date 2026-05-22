package org.dep.reimbursebackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "reimburse_doc")
public class ReimburseDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reimburse_no")
    private String reimburseNo;

    @Column(name = "doc_type")
    private String docType;

    private Integer status;

    @Column(name = "reimburser_id")
    private String reimburserId;

    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "company_id")
    private String companyId;

    @Column(name = "business_type_id")
    private String businessTypeId;

    private String title;

    private String reason;

    @Column(name = "subsidy_amount")
    private BigDecimal subsidyAmount;

    @Column(name = "submit_date")
    private LocalDate submitDate;

    @Column(name = "create_time")
    private LocalDate createTime;

    private String remark;
}
