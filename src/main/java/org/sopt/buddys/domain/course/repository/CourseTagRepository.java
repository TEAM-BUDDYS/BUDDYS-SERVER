package org.sopt.buddys.domain.course.repository;

import org.sopt.buddys.domain.course.entity.CourseTag;
import org.sopt.buddys.domain.course.entity.CourseTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseTagRepository extends JpaRepository<CourseTag, CourseTagId> {
}
