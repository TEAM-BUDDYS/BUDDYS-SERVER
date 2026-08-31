package org.sopt.buddys.domain.post.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

  Slice<Post> findByAuthorIdAndDeletedAtIsNull(Long authorId, Pageable pageable);

  Optional<Post> findByIdAndDeletedAtIsNull(Long id);

  boolean existsByIdAndDeletedAtIsNull(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select p
      from Post p
      where p.id = :id
        and p.deletedAt is null
      """)
  Optional<Post> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);

  @Query("""
      select p
      from Post p
      join fetch p.author
      join fetch p.country
      where p.country.id = :countryId
        and p.status = :status
        and p.author.id <> :excludeAuthorId
        and p.deletedAt is null
      """)
  List<Post> findByCountryIdAndStatusAndAuthorIdNot(
      @Param("countryId") Long countryId,
      @Param("status") PostStatus status,
      @Param("excludeAuthorId") Long excludeAuthorId
  );

  @Query("""
      select p from Post p
      join fetch p.author
      join fetch p.country
      where p.country.id = :countryId
        and p.status = :status
        and p.author.id <> :excludeAuthorId
        and p.deletedAt is null
        and exists (select 1 from PostImage pi where pi.post = p)
      """)
  List<Post> findByCountryIdAndStatusAndAuthorIdNotAndHasImage(
      @Param("countryId") Long countryId,
      @Param("status") PostStatus status,
      @Param("excludeAuthorId") Long excludeAuthorId
  );

  @Query("select p.author.id as authorId, count(p) as postCount from Post p where p.author.id in :authorIds and p.deletedAt is null group by p.author.id")
  List<AuthorPostCountProjection> countByAuthorIdIn(@Param("authorIds") Collection<Long> authorIds);

  @Query("""
      select p
      from Post p
      join fetch p.author author
      left join fetch author.exchangeCountry
      join fetch p.country
      join fetch p.city
      where p.id = :postId
        and p.deletedAt is null
      """)
  Optional<Post> findDetailById(@Param("postId") Long postId);

  @Modifying
  @Query("""
      update Post p
      set p.viewCount = p.viewCount + 1
      where p.id = :postId
        and p.deletedAt is null
      """)
  int increaseViewCount(@Param("postId") Long postId);

  @Modifying(flushAutomatically = true)
  @Query("""
      update Post p
      set p.commentCount = p.commentCount + 1
      where p.id = :postId
        and p.deletedAt is null
      """)
  int increaseCommentCount(@Param("postId") Long postId);

  interface AuthorPostCountProjection {
    Long getAuthorId();
    Long getPostCount();
  }
}
