package org.sopt.buddys.domain.post.repository;

import java.util.Optional;
import org.sopt.buddys.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

  Slice<Post> findByAuthorId(Long authorId, Pageable pageable);

  @Query("""
      select p
      from Post p
      join fetch p.author author
      left join fetch author.exchangeCountry
      join fetch p.country
      join fetch p.city
      where p.id = :postId
      """)
  Optional<Post> findDetailById(@Param("postId") Long postId);

  @Modifying
  @Query("""
      update Post p
      set p.viewCount = p.viewCount + 1
      where p.id = :postId
      """)
  int increaseViewCount(@Param("postId") Long postId);
}
