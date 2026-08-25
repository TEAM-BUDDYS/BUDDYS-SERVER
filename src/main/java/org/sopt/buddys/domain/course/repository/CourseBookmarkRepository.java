package org.sopt.buddys.domain.course.repository;

import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.entity.CourseBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseBookmarkRepository extends JpaRepository<CourseBookmark, CourseBookmarkId> {
}
