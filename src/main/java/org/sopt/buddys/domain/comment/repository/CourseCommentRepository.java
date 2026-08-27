package org.sopt.buddys.domain.comment.repository;

import org.sopt.buddys.domain.comment.entity.CourseComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseCommentRepository extends JpaRepository<CourseComment, Long> {

  @Query("""
      select cc
      from CourseComment cc
      join fetch cc.author
      where cc.course.id = :courseId
      order by cc.createdAt asc, cc.id asc
      """)
  Slice<CourseComment> findAllByCourseIdWithAuthorOrderByCreatedAtAsc(
      @Param("courseId") Long courseId,
      Pageable pageable
  );
}
