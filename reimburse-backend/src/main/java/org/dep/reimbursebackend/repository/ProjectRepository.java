package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, String> {
}
