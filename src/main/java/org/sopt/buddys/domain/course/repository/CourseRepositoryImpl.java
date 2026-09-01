package org.sopt.buddys.domain.course.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.QCourse;
import org.sopt.buddys.domain.course.entity.QCourseCountry;
import org.sopt.buddys.domain.course.entity.QCourseDay;
import org.sopt.buddys.domain.course.entity.QCoursePlace;
import org.sopt.buddys.domain.course.service.command.CourseSearchCondition;
import org.sopt.buddys.domain.location.entity.QCity;
import org.sopt.buddys.domain.location.entity.QCountry;
import org.sopt.buddys.domain.place.entity.QPlace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

public class CourseRepositoryImpl implements CourseRepositoryCustom {

  private static final QCourse course = QCourse.course;
  private static final QCourseCountry courseCountry = QCourseCountry.courseCountry;
  private static final QCourseDay courseDay = QCourseDay.courseDay;
  private static final QCoursePlace coursePlace = QCoursePlace.coursePlace;
  private static final QPlace place = QPlace.place;
  private static final QCountry country = QCountry.country;
  private static final QCity city = QCity.city;

  private final JPAQueryFactory queryFactory;

  public CourseRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Slice<Course> searchCourses(CourseSearchCondition condition, Pageable pageable) {
    List<Course> courses = queryFactory
        .selectFrom(course)
        .where(course.deletedAt.isNull(), countryEquals(condition.countryId()))
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

  @Override
  public Slice<Course> searchCoursesByKeyword(String keyword, Pageable pageable) {
    String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
    List<Course> courses = queryFactory
        .selectFrom(course)
        .where(
            course.deletedAt.isNull(),
            course.title.lower().contains(normalizedKeyword)
                .or(course.content.lower().contains(normalizedKeyword))
                .or(course.id.in(
                    JPAExpressions
                        .select(courseDay.course.id)
                        .from(coursePlace)
                        .join(coursePlace.courseDay, courseDay)
                        .join(coursePlace.place, place)
                        .leftJoin(place.country, country)
                        .leftJoin(place.city, city)
                        .where(
                            place.name.lower().contains(normalizedKeyword)
                                .or(country.name.lower().contains(normalizedKeyword))
                                .or(city.name.lower().contains(normalizedKeyword))
                                .or(city.koreanName.lower().contains(normalizedKeyword))
                        )
                ))
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
}
