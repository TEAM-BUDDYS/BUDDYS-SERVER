package org.sopt.buddys.domain.course.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.springframework.data.domain.Pageable;
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

  // 장소 사진 대체용: 장소가 그 장소 하나뿐인 일차의 사진만 쓴다. 장소가 여러 개인 일차는 사진이 어느 장소 것인지 알 수 없다.
  // 삭제되지 않은 코스 중 최근 코스, 일차 내 첫 사진 순이다.
  // place(google_place_id 유니크) → course_place(place_id FK 인덱스) → course_day → course_image(course_day_id 인덱스) 순으로 좁힌다.
  // 일차에 장소가 하나뿐이라는 조건 때문에 같은 사진이 중복으로 나오지 않는다.
  @Query("""
      select ci.imageUrl
      from CoursePlace cp
      join cp.place p
      join cp.courseDay cd
      join cd.course c
      join CourseImage ci on ci.courseDay = cd
      where p.googlePlaceId = :googlePlaceId
        and c.deletedAt is null
        and (select count(cp2) from CoursePlace cp2 where cp2.courseDay = cd) = 1
      order by c.id desc, ci.orderNo asc, ci.id asc
      """)
  List<String> findImageUrlsOfSinglePlaceDays(
      @Param("googlePlaceId") String googlePlaceId,
      Pageable pageable
  );

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
