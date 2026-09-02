package org.sopt.buddys.domain.course.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.course.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long>, CourseRepositoryCustom {

  @Query("""
      select c
      from Course c
      join fetch c.author author
      left join fetch author.exchangeCountry
      where c.id = :courseId
        and c.deletedAt is null
      """)
  Optional<Course> findDetailById(@Param("courseId") Long courseId);

  Optional<Course> findByIdAndDeletedAtIsNull(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select c
      from Course c
      where c.id = :courseId
        and c.deletedAt is null
      """)
  Optional<Course> findByIdAndDeletedAtIsNullForUpdate(@Param("courseId") Long courseId);

  boolean existsByIdAndDeletedAtIsNull(Long id);

  @Modifying
  @Query("""
      update Course c
      set c.viewCount = c.viewCount + 1
      where c.id = :courseId
        and c.deletedAt is null
      """)
  int increaseViewCount(@Param("courseId") Long courseId);

  @Modifying(flushAutomatically = true)
  @Query("""
      update Course c
      set c.commentCount = c.commentCount + 1
      where c.id = :courseId
        and c.deletedAt is null
      """)
  int increaseCommentCount(@Param("courseId") Long courseId);

  @Query("""
      select c.title
      from Course c
      where lower(c.title) like :containsPattern escape '!'
        and c.deletedAt is null
      order by case
          when lower(c.title) = :exactKeyword then 0
          when lower(c.title) like :prefixPattern escape '!' then 1
          else 2
        end,
        lower(c.title) asc,
        c.id asc
      """)
  List<String> findSuggestionTitles(
      @Param("exactKeyword") String exactKeyword,
      @Param("prefixPattern") String prefixPattern,
      @Param("containsPattern") String containsPattern,
      Pageable pageable
  );
}
