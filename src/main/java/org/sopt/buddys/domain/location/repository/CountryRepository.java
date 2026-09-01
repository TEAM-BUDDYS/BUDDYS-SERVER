package org.sopt.buddys.domain.location.repository;

import java.util.List;
import org.sopt.buddys.domain.location.entity.Country;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CountryRepository extends JpaRepository<Country, Long> {
  Slice<Country> findByNameContainingIgnoreCaseOrderByNameAsc(String keyword, Pageable pageable);

  Slice<Country> findAllByOrderByNameAsc(Pageable pageable);

  @Query("""
      select c.name
      from Country c
      where lower(c.name) like :containsPattern escape '!'
      order by case
          when lower(c.name) = :exactKeyword then 0
          when lower(c.name) like :prefixPattern escape '!' then 1
          else 2
        end,
        lower(c.name) asc,
        c.id asc
      """)
  List<String> findSuggestionNames(
      @Param("exactKeyword") String exactKeyword,
      @Param("prefixPattern") String prefixPattern,
      @Param("containsPattern") String containsPattern,
      Pageable pageable
  );
}
