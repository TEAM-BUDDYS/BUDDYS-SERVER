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
      select c.id as id,
        case
          when lower(c.name) = :exactKeyword and lower(c.koreanName) = :exactKeyword then
            case
              when lower(c.name) < lower(c.koreanName) then c.name
              when lower(c.name) = lower(c.koreanName) and c.name <= c.koreanName then c.name
              else c.koreanName
            end
          when lower(c.name) = :exactKeyword then c.name
          when lower(c.koreanName) = :exactKeyword then c.koreanName
          when lower(c.name) like :prefixPattern escape '!'
            and lower(c.koreanName) like :prefixPattern escape '!' then
            case
              when lower(c.name) < lower(c.koreanName) then c.name
              when lower(c.name) = lower(c.koreanName) and c.name <= c.koreanName then c.name
              else c.koreanName
            end
          when lower(c.name) like :prefixPattern escape '!' then c.name
          when lower(c.koreanName) like :prefixPattern escape '!' then c.koreanName
          when lower(c.name) like :containsPattern escape '!'
            and lower(c.koreanName) like :containsPattern escape '!' then
            case
              when lower(c.name) < lower(c.koreanName) then c.name
              when lower(c.name) = lower(c.koreanName) and c.name <= c.koreanName then c.name
              else c.koreanName
            end
          when lower(c.name) like :containsPattern escape '!' then c.name
          else c.koreanName
        end as matchedName,
        case
          when lower(c.name) = :exactKeyword or lower(c.koreanName) = :exactKeyword then 0
          when lower(c.name) like :prefixPattern escape '!'
            or lower(c.koreanName) like :prefixPattern escape '!' then 1
          else 2
        end as relevance
      from City c
      where lower(c.name) like :containsPattern escape '!'
        or lower(c.koreanName) like :containsPattern escape '!'
      order by relevance asc,
        matchedName asc,
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
    String getMatchedName();
    int getRelevance();
  }
}
