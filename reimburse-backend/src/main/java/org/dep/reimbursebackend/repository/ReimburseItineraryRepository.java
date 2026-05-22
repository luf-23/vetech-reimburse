package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.ReimburseItinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReimburseItineraryRepository extends JpaRepository<ReimburseItinerary, Long> {

    List<ReimburseItinerary> findByDocIdOrderByIdAsc(Long docId);

    void deleteByDocId(Long docId);
}
