package org.sopt.buddys.domain.course.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

  @Query("""
      select ci.courseDay.course.id as courseId, ci.imageUrl as imageUrl
      from CourseImage ci
      where ci.courseDay.course.id in :courseIds
      order by ci.courseDay.course.id asc, ci.courseDay.dayNumber asc, ci.orderNo asc, ci.id asc
      """)
  List<CourseImageUrlProjection> findImageUrlsByCourseIdIn(@Param("courseIds") Collection<Long> courseIds);

  @Query("""
      select ci.courseDay.course.id as courseId, ci.imageUrl as imageUrl
      from CourseImage ci
      where ci.courseDay.course.id in :courseIds
        and ci.courseDay.dayNumber = 1
        and ci.orderNo = 0
      """)
  List<CourseImageUrlProjection> findThumbnailImageUrlsByCourseIds(
      @Param("courseIds") Collection<Long> courseIds);

  @Modifying
  @Query("""
      delete from CourseImage ci
      where ci.courseDay.id in (
          select cd.id from CourseDay cd where cd.course.id = :courseId
      )
      """)
  void deleteAllByCourseId(@Param("courseId") Long courseId);

  interface CourseImageUrlProjection {
    Long getCourseId();
    String getImageUrl();
  }
}
