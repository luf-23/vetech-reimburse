package org.dep.reimbursebackend.repository;

import org.dep.reimbursebackend.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, String> {
}
