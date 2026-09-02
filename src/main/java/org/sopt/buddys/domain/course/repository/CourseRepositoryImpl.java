package org.sopt.buddys.domain.course.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.QCourse;
import org.sopt.buddys.domain.course.entity.QCourseCountry;
import org.sopt.buddys.domain.course.entity.QCourseTag;
import org.sopt.buddys.domain.course.service.command.CourseSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

public class CourseRepositoryImpl implements CourseRepositoryCustom {

  private static final QCourse course = QCourse.course;
  private static final QCourseCountry courseCountry = QCourseCountry.courseCountry;
  private static final QCourseTag courseTag = QCourseTag.courseTag;

  private final JPAQueryFactory queryFactory;

  public CourseRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Slice<Course> searchCourses(CourseSearchCondition condition, Pageable pageable) {
    List<Course> courses = queryFactory
        .selectFrom(course)
        .where(
            course.deletedAt.isNull(),
            countryEquals(condition.countryId()),
            tagEquals(condition.tagId())
        )
        .orderBy(course.createdAt.desc(), course.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize() + 1L)
        .fetch();

    boolean hasNext = courses.size() > pageable.getPageSize();
    if (hasNext) {
      courses = courses.subList(0, pageable.getPageSize());
    }
    return new SliceImpl<>(courses, pageable, hasNext);
  }

  private BooleanExpression countryEquals(Long countryId) {
    if (countryId == null) {
      return null;
    }
    return course.id.in(
        JPAExpressions
            .select(courseCountry.course.id)
            .from(courseCountry)
            .where(courseCountry.country.id.eq(countryId))
    );
  }

  private BooleanExpression tagEquals(Long tagId) {
    if (tagId == null) {
      return null;
    }
    return course.id.in(
        JPAExpressions
            .select(courseTag.course.id)
            .from(courseTag)
            .where(courseTag.tag.id.eq(tagId))
    );
  }
}
