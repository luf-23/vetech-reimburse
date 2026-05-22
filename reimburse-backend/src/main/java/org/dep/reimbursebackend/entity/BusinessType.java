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
@Table(name = "business_type")
public class BusinessType {

    @Id
    @Column(name = "business_type_id")
    private String businessTypeId;

    @Column(name = "business_type_no")
    private String businessTypeNo;

    @Column(name = "business_type_name")
    private String businessTypeName;

    @Column(name = "there_subordinate_node")
    private String thereSubordinateNode;

    @Column(name = "superior_id")
    private String superiorId;
}
