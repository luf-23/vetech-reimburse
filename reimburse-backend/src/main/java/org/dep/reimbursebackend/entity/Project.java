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
@Table(name = "project")
public class Project {

    @Id
    @Column(name = "project_id")
    private String projectId;

    @Column(name = "project_no")
    private String projectNo;

    @Column(name = "project_name")
    private String projectName;
}
