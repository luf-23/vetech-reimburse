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
@Table(name = "reim_company")
public class ReimCompany {

    @Id
    @Column(name = "reim_company_id")
    private String reimCompanyId;

    @Column(name = "reim_company_no")
    private String reimCompanyNo;

    @Column(name = "reim_company_name")
    private String reimCompanyName;
}
