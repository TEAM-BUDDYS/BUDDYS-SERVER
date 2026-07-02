package org.sopt.buddys.domain.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByIdAndDeletedAtIsNull(Long id);

  @Query("""
      select t.name
      from UserTag ut
      join ut.tag t
      where ut.user.id = :userId
      order by ut.createdAt asc
      """)
  List<String> findTagNamesByUserId(@Param("userId") Long userId);

  @Query("""
      select p
      from Post p
      where p.author.id = :userId
      order by p.createdAt desc
      """)
  List<Post> findPostsByUserId(@Param("userId") Long userId);

  @Query("""
      select pi.post.id as postId,
             pi.imageUrl as thumbnailImageUrl
      from PostImage pi
      where pi.post.id in :postIds
        and pi.orderNo = (
          select min(pi2.orderNo)
          from PostImage pi2
          where pi2.post.id = pi.post.id
        )
      """)
  List<PostThumbnailProjection> findThumbnailImageUrlsByPostIds(
      @Param("postIds") Collection<Long> postIds
  );

  interface PostThumbnailProjection {
    Long getPostId();
    String getThumbnailImageUrl();
  }
}
