package org.sopt.buddys.domain.place.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.place.entity.PlaceBookmark;
import org.sopt.buddys.domain.place.entity.PlaceBookmarkId;
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
}
