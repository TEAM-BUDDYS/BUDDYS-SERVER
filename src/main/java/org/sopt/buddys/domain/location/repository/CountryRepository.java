package org.sopt.buddys.domain.location.repository;

import java.util.List;
import org.sopt.buddys.domain.location.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
  List<Country> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword);
}
