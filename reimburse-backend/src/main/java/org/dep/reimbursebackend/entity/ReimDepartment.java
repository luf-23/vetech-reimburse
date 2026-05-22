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
@Table(name = "reim_department")
public class ReimDepartment {

    @Id
    @Column(name = "reim_department_id")
    private String reimDepartmentId;

    @Column(name = "reim_department_no")
    private String reimDepartmentNo;

    @Column(name = "reim_department_name")
    private String reimDepartmentName;
}
