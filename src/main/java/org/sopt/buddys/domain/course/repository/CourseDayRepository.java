package org.sopt.buddys.domain.course.repository;

import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseDayRepository extends JpaRepository<CourseDay, Long> {

  @Query("""
      select cd
      from CourseDay cd
      where cd.course.id = :courseId
      order by cd.dayNumber asc
      """)
  List<CourseDay> findAllByCourseIdOrderByDayNumberAsc(@Param("courseId") Long courseId);
}
