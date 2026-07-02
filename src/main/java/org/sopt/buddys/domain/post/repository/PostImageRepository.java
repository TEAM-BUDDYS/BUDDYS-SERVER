package org.sopt.buddys.domain.post.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

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
