package org.sopt.buddys.domain.course.repository;

import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.entity.CourseBookmarkId;
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

  interface BookmarkSummary {
    long getTotalCount();
    long getBookmarkedCount();
  }
}
