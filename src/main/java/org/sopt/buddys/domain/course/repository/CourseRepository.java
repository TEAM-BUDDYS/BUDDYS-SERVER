package org.sopt.buddys.domain.course.repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.sopt.buddys.domain.course.entity.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long>, CourseRepositoryCustom {

  Slice<Course> findByAuthorIdAndDeletedAtIsNull(Long authorId, Pageable pageable);

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
}
