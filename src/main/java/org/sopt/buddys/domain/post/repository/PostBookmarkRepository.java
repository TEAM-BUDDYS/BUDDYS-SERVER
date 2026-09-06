package org.sopt.buddys.domain.post.repository;

import java.util.Collection;
import java.util.Set;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostBookmark;
import org.sopt.buddys.domain.post.entity.PostBookmarkId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, PostBookmarkId> {

  @Query("""
      select pb.post
      from PostBookmark pb
      join fetch pb.post.country
      join fetch pb.post.city
      where pb.user.id = :userId
        and pb.post.deletedAt is null
      order by pb.createdAt desc, pb.post.id desc
      """)
  Slice<Post> findBookmarkedPostsByUserId(@Param("userId") Long userId, Pageable pageable);

  @Query("""
      select pb.post.id
      from PostBookmark pb
      where pb.user.id = :userId
        and pb.post.id in :postIds
      """)
  Set<Long> findBookmarkedPostIds(
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
