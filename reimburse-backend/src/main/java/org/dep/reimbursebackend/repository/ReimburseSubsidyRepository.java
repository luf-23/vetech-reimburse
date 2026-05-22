package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.ReimburseSubsidy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReimburseSubsidyRepository extends JpaRepository<ReimburseSubsidy, Long> {

    List<ReimburseSubsidy> findByDocIdOrderByIdAsc(Long docId);

    void deleteByDocId(Long docId);
}
