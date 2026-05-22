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
@Table(name = "reimburser")
public class Reimburser {

    @Id
    @Column(name = "reimburser_id")
    private String reimburserId;

    @Column(name = "reimburser_no")
    private String reimburserNo;

    @Column(name = "reimburser_name")
    private String reimburserName;
}
