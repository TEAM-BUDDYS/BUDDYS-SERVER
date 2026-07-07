package org.sopt.buddys.domain.location.repository;

import org.sopt.buddys.domain.location.entity.City;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CityRepository extends JpaRepository<City, Long> {
  @Query("""
      select c from City c
      where c.country.id = :countryId
        and (
          lower(c.name) like lower(concat('%', :keyword, '%'))
          or lower(c.koreanName) like lower(concat('%', :keyword, '%'))
        )
      order by c.population desc
      """)
  Slice<City> search(@Param("countryId") Long countryId, @Param("keyword") String keyword, Pageable pageable);

}
