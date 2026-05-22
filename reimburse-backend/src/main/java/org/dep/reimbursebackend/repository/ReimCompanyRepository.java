package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.ReimCompany;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimCompanyRepository extends JpaRepository<ReimCompany, String> {
}
