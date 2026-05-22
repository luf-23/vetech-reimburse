package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.ReimDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimDepartmentRepository extends JpaRepository<ReimDepartment, String> {
}
