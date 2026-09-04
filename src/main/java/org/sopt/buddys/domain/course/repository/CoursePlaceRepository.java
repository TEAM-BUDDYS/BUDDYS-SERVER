package org.sopt.buddys.domain.course.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.course.entity.CoursePlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoursePlaceRepository extends JpaRepository<CoursePlace, Long> {

  @Query("""
      select cp
      from CoursePlace cp
      join fetch cp.place
      where cp.courseDay.id in :courseDayIds
      order by cp.courseDay.id asc, cp.orderNo asc, cp.id asc
      """)
  List<CoursePlace> findAllByCourseDayIdInWithPlace(@Param("courseDayIds") Collection<Long> courseDayIds);

  @Modifying
  @Query("""
      delete from CoursePlace cp
      where cp.courseDay.id in (
          select cd.id from CourseDay cd where cd.course.id = :courseId
      )
      """)
  void deleteAllByCourseId(@Param("courseId") Long courseId);
}
