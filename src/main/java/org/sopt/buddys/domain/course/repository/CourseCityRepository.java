package org.sopt.buddys.domain.course.repository;

import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseCity;
import org.sopt.buddys.domain.course.entity.CourseCityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseCityRepository extends JpaRepository<CourseCity, CourseCityId> {

  @Query("""
      select cc
      from CourseCity cc
      join fetch cc.city
      where cc.course.id = :courseId
      order by cc.city.id asc
      """)
  List<CourseCity> findAllByCourseIdWithCity(@Param("courseId") Long courseId);

  @Modifying
  @Query("delete from CourseCity cc where cc.course.id = :courseId")
  void deleteAllByCourseId(@Param("courseId") Long courseId);
}
