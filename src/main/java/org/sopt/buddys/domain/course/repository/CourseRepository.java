package org.sopt.buddys.domain.course.repository;

import org.sopt.buddys.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
