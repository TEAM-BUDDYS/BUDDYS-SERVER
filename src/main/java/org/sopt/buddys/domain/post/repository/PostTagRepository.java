package org.sopt.buddys.domain.post.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.post.entity.PostTag;
import org.sopt.buddys.domain.post.entity.PostTagId;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {
  @Query("""
      select pt.post.id as postId, t.id as tagId, t.tagType as tagType
      from PostTag pt
      join pt.tag t
      where pt.post.id in :postIds
      """)
  List<PostTagBulkProjection> findAllByPostIdIn(@Param("postIds") Collection<Long> postIds);

  interface PostTagBulkProjection {
    Long getPostId();
    Long getTagId();
    TagType getTagType();
  }
}
