package org.sopt.buddys.domain.location.repository;

import org.sopt.buddys.domain.location.entity.Country;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
  Slice<Country> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword, Pageable pageable);
}
