package org.sopt.buddys.domain.post.repository;

import java.util.List;
import org.sopt.buddys.domain.post.entity.PostTag;
import org.sopt.buddys.domain.post.entity.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {

  @Query("""
      select pt
      from PostTag pt
      join fetch pt.tag
      where pt.post.id = :postId
      order by pt.tag.id asc
      """)
  List<PostTag> findAllByPostIdWithTag(@Param("postId") Long postId);
}
