package org.sopt.buddys.domain.course.repository;

import java.util.Collection;
import java.util.Set;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.entity.CourseBookmarkId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, CourseBookmarkId> {

  @Query("""
      select count(cb) as totalCount,
             count(case when cb.user.id = :userId then 1 end) as bookmarkedCount
      from CourseBookmark cb
      where cb.course.id = :courseId
      """)
  BookmarkSummary findBookmarkSummary(@Param("userId") Long userId, @Param("courseId") Long courseId);

  @Query("""
      select cb.course.id
      from CourseBookmark cb
      where cb.user.id = :userId
        and cb.course.id in :courseIds
      """)
  Set<Long> findBookmarkedCourseIds(@Param("userId") Long userId, @Param("courseIds") Collection<Long> courseIds);

  @Query("""
      select cb.course
      from CourseBookmark cb
      where cb.user.id = :userId
        and cb.course.deletedAt is null
      order by cb.createdAt desc
      """)
  Slice<Course> findBookmarkedCoursesByUserId(@Param("userId") Long userId, Pageable pageable);

  interface BookmarkSummary {
    long getTotalCount();
    long getBookmarkedCount();
  }
}
