package org.sopt.buddys.domain.post.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

  Slice<Post> findByAuthorId(Long authorId, Pageable pageable);

  @Query("""
      select p
      from Post p
      join fetch p.author
      join fetch p.country
      where p.country.id = :countryId
        and p.status = :status
        and p.author.id <> :excludeAuthorId
      """)
  List<Post> findByCountryIdAndStatusAndAuthorIdNot(
      @Param("countryId") Long countryId,
      @Param("status") PostStatus status,
      @Param("excludeAuthorId") Long excludeAuthorId
  );

  @Query("select p.author.id as authorId, count(p) as postCount from Post p where p.author.id in :authorIds group by p.author.id")
  List<AuthorPostCountProjection> countByAuthorIdIn(@Param("authorIds") Collection<Long> authorIds);

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

  @Modifying(flushAutomatically = true)
  @Query("""
      update Post p
      set p.commentCount = p.commentCount + 1
      where p.id = :postId
      """)
  int increaseCommentCount(@Param("postId") Long postId);

  interface AuthorPostCountProjection {
    Long getAuthorId();
    Long getPostCount();
  }
}
