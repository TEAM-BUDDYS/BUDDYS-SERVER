package org.sopt.buddys.domain.place.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.place.entity.Place;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  Optional<Place> findByGooglePlaceId(String googlePlaceId);

  List<Place> findByGooglePlaceIdIn(Collection<String> googlePlaceIds);

  // 장소 사진 대체(Unsplash) 검색어: 도시 영문명 → 국가 영문명 → 구글 표기 도시명 → 구글 표기 국가명 순이다.
  @Query("""
      select coalesce(ci.name, co.englishName, nullif(p.cityName, ''), nullif(p.countryName, ''))
      from Place p
      left join p.city ci
      left join p.country co
      where p.googlePlaceId = :googlePlaceId
      """)
  Optional<String> findPhotoSearchKeyword(@Param("googlePlaceId") String googlePlaceId);

  // 비어 있는 컬럼만 DB에서 원자적으로 채운다. 엔티티 변경 감지(전체 컬럼 UPDATE)로 하면
  // 동시에 다른 트랜잭션이 채운 값을 오래된 스냅샷으로 덮어쓸 수 있다.
  @Modifying
  @Query("""
      update Place p
      set p.address = coalesce(nullif(p.address, ''), :address),
          p.countryName = coalesce(nullif(p.countryName, ''), :countryName),
          p.cityName = coalesce(nullif(p.cityName, ''), :cityName)
      where p.id = :placeId
      """)
  int fillMissingLocation(
      @Param("placeId") Long placeId,
      @Param("address") String address,
      @Param("countryName") String countryName,
      @Param("cityName") String cityName
  );

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
