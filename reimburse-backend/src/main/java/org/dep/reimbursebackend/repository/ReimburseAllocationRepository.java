package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.ReimburseAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReimburseAllocationRepository extends JpaRepository<ReimburseAllocation, Long> {

    List<ReimburseAllocation> findByDocIdOrderBySortOrderAscIdAsc(Long docId);

    void deleteByDocId(Long docId);
}
