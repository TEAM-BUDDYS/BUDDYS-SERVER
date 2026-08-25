package org.sopt.buddys.domain.course.repository;

import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseTag;
import org.sopt.buddys.domain.course.entity.CourseTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseTagRepository extends JpaRepository<CourseTag, CourseTagId> {

  @Query("""
      select ct
      from CourseTag ct
      join fetch ct.tag
      where ct.course.id = :courseId
      order by ct.tag.id asc
      """)
  List<CourseTag> findAllByCourseIdWithTag(@Param("courseId") Long courseId);
}
