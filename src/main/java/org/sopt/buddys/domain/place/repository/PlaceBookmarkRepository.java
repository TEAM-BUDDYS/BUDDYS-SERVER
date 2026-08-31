package org.sopt.buddys.domain.place.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.place.entity.PlaceBookmark;
import org.sopt.buddys.domain.place.entity.PlaceBookmarkId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceBookmarkRepository extends JpaRepository<PlaceBookmark, PlaceBookmarkId> {

  @Query("""
      select pb.place.googlePlaceId
      from PlaceBookmark pb
      where pb.user.id = :userId
        and pb.place.googlePlaceId in :googlePlaceIds
      """)
  List<String> findBookmarkedGooglePlaceIds(
      @Param("userId") Long userId,
      @Param("googlePlaceIds") Collection<String> googlePlaceIds
  );

  @Query("""
      select pb
      from PlaceBookmark pb
      join fetch pb.place
      where pb.user.id = :userId
      order by pb.createdAt desc
      """)
  Slice<PlaceBookmark> findAllByUserIdWithPlaceOrderByCreatedAtDesc(
      @Param("userId") Long userId,
      Pageable pageable
  );

  /**
   * 지도 영역(남서-북동 사각형) 안에 좌표가 있는, 유저가 저장한 장소. 좌표가 null인 장소는 자연히 제외된다.
   * 날짜변경선을 넘는 영역(swLng > neLng)은 지원하지 않는다.
   */
  @Query("""
      select pb
      from PlaceBookmark pb
      join fetch pb.place p
      where pb.user.id = :userId
        and p.latitude between :swLat and :neLat
        and p.longitude between :swLng and :neLng
      order by pb.createdAt desc
      """)
  List<PlaceBookmark> findMarkersByUserIdWithinBounds(
      @Param("userId") Long userId,
      @Param("swLat") BigDecimal swLat,
      @Param("neLat") BigDecimal neLat,
      @Param("swLng") BigDecimal swLng,
      @Param("neLng") BigDecimal neLng,
      Pageable pageable
  );
}
