package org.sopt.buddys.domain.location.repository;

import org.sopt.buddys.domain.location.entity.University;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UniversityRepository extends JpaRepository<University, Long> {
  @Query("""
      select u from University u
      where u.country.id = :countryId
        and lower(u.name) like lower(concat('%', :keyword, '%'))
      order by u.name asc
      """)
  Slice<University> search(
      @Param("countryId") Long countryId, @Param("keyword") String keyword, Pageable pageable
  );
}