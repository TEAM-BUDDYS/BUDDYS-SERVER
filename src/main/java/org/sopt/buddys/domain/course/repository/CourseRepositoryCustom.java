package org.sopt.buddys.domain.course.repository;

import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.service.command.CourseSearchCondition;
import org.sopt.buddys.domain.search.service.command.SearchSort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface CourseRepositoryCustom {

  Slice<Course> searchCourses(CourseSearchCondition condition, Pageable pageable);

  Slice<Course> searchCoursesByKeyword(String keyword, SearchSort sort, Pageable pageable);

  long countCoursesByKeyword(String keyword);
}
