package org.sopt.buddys.domain.post.repository;

import org.sopt.buddys.domain.post.entity.PostBookmark;
import org.sopt.buddys.domain.post.entity.PostBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, PostBookmarkId> {

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
