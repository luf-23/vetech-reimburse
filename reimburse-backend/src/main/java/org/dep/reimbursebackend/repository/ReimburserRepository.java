package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.Reimburser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReimburserRepository extends JpaRepository<Reimburser, String> {
}
