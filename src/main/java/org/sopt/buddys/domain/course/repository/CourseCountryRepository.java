package org.sopt.buddys.domain.course.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseCountry;
import org.sopt.buddys.domain.course.entity.CourseCountryId;
import org.springframework.data.domain.Pageable;
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

  @Query("""
      select cc.course.id as courseId, cc.country.name as name
      from CourseCountry cc
      where cc.course.id in :courseIds
      order by cc.course.id asc, cc.country.id asc
      """)
  List<CourseNameProjection> findCountryNamesByCourseIdIn(@Param("courseIds") Collection<Long> courseIds);

  // 삭제되지 않은 코스 수가 많은 국가 순으로 국가 ID를 조회한다. 동점이면 국가 ID가 작은 순이다.
  @Query("""
      select cc.country.id
      from CourseCountry cc
      where cc.course.deletedAt is null
        and (:excludeCountryId is null or cc.country.id <> :excludeCountryId)
      group by cc.country.id
      order by count(cc) desc, cc.country.id asc
      """)
  List<Long> findCountryIdsOrderByCourseCountDesc(
      @Param("excludeCountryId") Long excludeCountryId,
      Pageable pageable
  );

  @Modifying
  @Query("delete from CourseCountry cc where cc.course.id = :courseId")
  void deleteAllByCourseId(@Param("courseId") Long courseId);

  interface CourseNameProjection {
    Long getCourseId();
    String getName();
  }
}
