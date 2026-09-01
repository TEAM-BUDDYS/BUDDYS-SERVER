package org.sopt.buddys.domain.location.repository;

import java.util.List;
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
      order by c.population desc, c.id desc
      """)
  Slice<City> search(@Param("countryId") Long countryId, @Param("keyword") String keyword, Pageable pageable);

  @Query("""
      select c.id as id, c.name as name, c.koreanName as koreanName
      from City c
      where lower(c.name) like :containsPattern escape '!'
        or lower(c.koreanName) like :containsPattern escape '!'
      order by case
          when lower(c.name) = :exactKeyword or lower(c.koreanName) = :exactKeyword then 0
          when lower(c.name) like :prefixPattern escape '!'
            or lower(c.koreanName) like :prefixPattern escape '!' then 1
          else 2
        end,
        lower(c.name) asc,
        c.id asc
      """)
  List<CitySuggestionProjection> findSuggestionCities(
      @Param("exactKeyword") String exactKeyword,
      @Param("prefixPattern") String prefixPattern,
      @Param("containsPattern") String containsPattern,
      Pageable pageable
  );

  interface CitySuggestionProjection {
    Long getId();
    String getName();
    String getKoreanName();
  }
}
