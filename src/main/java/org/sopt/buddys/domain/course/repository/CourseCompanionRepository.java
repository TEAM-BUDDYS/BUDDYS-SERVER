package org.sopt.buddys.domain.course.repository;

import org.sopt.buddys.domain.course.entity.CourseCompanion;
import org.sopt.buddys.domain.course.entity.CourseCompanionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCompanionRepository extends JpaRepository<CourseCompanion, CourseCompanionId> {
}
