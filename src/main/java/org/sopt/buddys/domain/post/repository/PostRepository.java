package org.sopt.buddys.domain.post.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

  Slice<Post> findByAuthorId(Long authorId, Pageable pageable);

  List<Post> findByCountryIdAndStatusAndAuthorIdNot(Long countryId, PostStatus status, Long excludeAuthorId);

  @Query("select p.author.id as authorId, count(p) as postCount from Post p where p.author.id in :authorIds group by p.author.id")
  List<AuthorPostCountProjection> countByAuthorIdIn(@Param("authorIds") Collection<Long> authorIds);

  interface AuthorPostCountProjection {
    Long getAuthorId();
    Long getPostCount();
  }
}
