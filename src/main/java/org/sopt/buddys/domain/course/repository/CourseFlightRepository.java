package org.sopt.buddys.domain.course.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseFlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseFlightRepository extends JpaRepository<CourseFlight, Long> {

  @Query("""
      select cf
      from CourseFlight cf
      where cf.courseDay.id in :courseDayIds
      order by cf.courseDay.id asc, cf.orderNo asc, cf.id asc
      """)
  List<CourseFlight> findAllByCourseDayIdIn(@Param("courseDayIds") Collection<Long> courseDayIds);

  @Modifying
  @Query("""
      delete from CourseFlight cf
      where cf.courseDay.id in (
          select cd.id from CourseDay cd where cd.course.id = :courseId
      )
      """)
  void deleteAllByCourseId(@Param("courseId") Long courseId);
}
