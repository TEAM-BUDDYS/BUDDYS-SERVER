package org.sopt.buddys.domain.post.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.post.entity.PostBookmark;
import org.sopt.buddys.domain.post.entity.PostBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, PostBookmarkId> {

  @Query("""
      select pb.id.postId
      from PostBookmark pb
      where pb.id.userId = :userId
        and pb.id.postId in :postIds
      """)
  List<Long> findBookmarkedPostIds(
      @Param("userId") Long userId,
      @Param("postIds") Collection<Long> postIds
  );

  @Modifying
  @Query(value = """
      INSERT INTO post_bookmark (user_id, post_id, created_at)
      VALUES (:userId, :postId, CURRENT_TIMESTAMP(6))
      ON DUPLICATE KEY UPDATE created_at = created_at
      """, nativeQuery = true)
  int insertOrKeep(
      @Param("userId") Long userId,
      @Param("postId") Long postId
  );
}
