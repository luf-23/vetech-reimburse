package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.ReimburseDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReimburseDocRepository extends JpaRepository<ReimburseDoc, Long>, JpaSpecificationExecutor<ReimburseDoc> {
}
