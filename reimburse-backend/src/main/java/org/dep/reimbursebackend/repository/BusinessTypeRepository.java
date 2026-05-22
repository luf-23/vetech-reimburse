package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.BusinessType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessTypeRepository extends JpaRepository<BusinessType, String> {
}
