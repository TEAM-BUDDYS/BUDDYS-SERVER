package org.sopt.buddys.domain.course.repository;

import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseCompanion;
import org.sopt.buddys.domain.course.entity.CourseCompanionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseCompanionRepository extends JpaRepository<CourseCompanion, CourseCompanionId> {

  @Query("""
      select cc
      from CourseCompanion cc
      join fetch cc.user
      where cc.course.id = :courseId
      order by cc.user.id asc
      """)
  List<CourseCompanion> findAllByCourseIdWithUser(@Param("courseId") Long courseId);
}
