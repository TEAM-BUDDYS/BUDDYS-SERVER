package org.sopt.buddys.domain.magazine.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.magazine.entity.MagazineBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MagazineBookmarkRepository extends JpaRepository<MagazineBookmark, Long> {

  @Modifying
  @Query(value = """
      INSERT INTO magazine_bookmark (user_id, magazine_id, created_at)
      VALUES (:userId, :magazineId, CURRENT_TIMESTAMP(6))
      ON DUPLICATE KEY UPDATE created_at = created_at
      """, nativeQuery = true)
  int insertOrKeep(
      @Param("userId") Long userId,
      @Param("magazineId") Long magazineId
  );

  @Modifying
  @Query("""
      delete from MagazineBookmark b
      where b.user.id = :userId
        and b.magazine.id = :magazineId
      """)
  void deleteByUserIdAndMagazineId(
      @Param("userId") Long userId,
      @Param("magazineId") Long magazineId
  );

  @Query("""
      select b.magazine.id
      from MagazineBookmark b
      where b.user.id = :userId
        and b.magazine.id in :magazineIds
      """)
  List<Long> findBookmarkedMagazineIds(
      @Param("userId") Long userId,
      @Param("magazineIds") Collection<Long> magazineIds
  );
}
