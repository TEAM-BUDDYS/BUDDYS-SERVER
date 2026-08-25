package org.sopt.buddys.domain.course.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseImageRepository extends JpaRepository<CourseImage, Long> {

  @Query("""
      select ci
      from CourseImage ci
      where ci.courseDay.id in :courseDayIds
      order by ci.courseDay.id asc, ci.orderNo asc, ci.id asc
      """)
  List<CourseImage> findAllByCourseDayIdIn(@Param("courseDayIds") Collection<Long> courseDayIds);
}
