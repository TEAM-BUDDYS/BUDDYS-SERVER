package org.sopt.buddys.domain.course.repository;

import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseCountry;
import org.sopt.buddys.domain.course.entity.CourseCountryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseCountryRepository extends JpaRepository<CourseCountry, CourseCountryId> {

  @Query("""
      select cc
      from CourseCountry cc
      join fetch cc.country
      where cc.course.id = :courseId
      order by cc.country.id asc
      """)
  List<CourseCountry> findAllByCourseIdWithCountry(@Param("courseId") Long courseId);

  @Modifying
  @Query("delete from CourseCountry cc where cc.course.id = :courseId")
  void deleteAllByCourseId(@Param("courseId") Long courseId);
}
