package org.sopt.buddys.domain.place.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.place.entity.Place;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  Optional<Place> findByGooglePlaceId(String googlePlaceId);

  List<Place> findByGooglePlaceIdIn(Collection<String> googlePlaceIds);

  @Query("""
      select p.name
      from Place p
      where lower(p.name) like :containsPattern escape '!'
      order by case
          when lower(p.name) = :exactKeyword then 0
          when lower(p.name) like :prefixPattern escape '!' then 1
          else 2
        end,
        lower(p.name) asc,
        p.id asc
      """)
  List<String> findSuggestionNames(
      @Param("exactKeyword") String exactKeyword,
      @Param("prefixPattern") String prefixPattern,
      @Param("containsPattern") String containsPattern,
      Pageable pageable
  );
}
