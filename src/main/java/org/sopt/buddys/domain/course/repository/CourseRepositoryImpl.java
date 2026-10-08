package org.sopt.buddys.domain.course.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.QCourse;
import org.sopt.buddys.domain.course.entity.QCourseBookmark;
import org.sopt.buddys.domain.course.entity.QCourseCity;
import org.sopt.buddys.domain.course.entity.QCourseCountry;
import org.sopt.buddys.domain.course.entity.QCourseDay;
import org.sopt.buddys.domain.course.entity.QCoursePlace;
import org.sopt.buddys.domain.course.entity.QCourseTag;
import org.sopt.buddys.domain.course.service.command.CourseSearchCondition;
import org.sopt.buddys.domain.place.entity.QPlace;
import org.sopt.buddys.domain.search.service.command.SearchSort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

public class CourseRepositoryImpl implements CourseRepositoryCustom {

  private static final QCourse course = QCourse.course;
  private static final QCourseCountry courseCountry = QCourseCountry.courseCountry;
  private static final QCourseCity courseCity = QCourseCity.courseCity;
  private static final QCourseDay courseDay = QCourseDay.courseDay;
  private static final QCoursePlace coursePlace = QCoursePlace.coursePlace;
  private static final QPlace place = QPlace.place;
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

  @Override
  public Slice<Course> searchCoursesByKeyword(String keyword, SearchSort sort, Pageable pageable) {
    JPAQuery<Course> contentQuery = queryFactory
        .selectFrom(course)
        .where(course.deletedAt.isNull(), keywordContains(keyword));

    if (sort == SearchSort.BOOKMARK) {
      QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;
      contentQuery.leftJoin(courseBookmark).on(courseBookmark.course.eq(course))
          .groupBy(course.id)
          .orderBy(courseBookmark.id.userId.count().desc(), course.createdAt.desc(), course.id.desc());
    } else {
      contentQuery.orderBy(course.createdAt.desc(), course.id.desc());
    }

    List<Course> courses = contentQuery
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
  public long countCoursesByKeyword(String keyword) {
    return queryFactory
        .select(course.id.count())
        .from(course)
        .where(course.deletedAt.isNull(), keywordContains(keyword))
        .fetchOne();
  }

  private BooleanExpression keywordContains(String keyword) {
    String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
    return course.title.lower().contains(normalizedKeyword)
        .or(course.content.lower().contains(normalizedKeyword))
        .or(course.id.in(
            JPAExpressions
                .select(courseDay.course.id)
                .from(coursePlace)
                .join(coursePlace.courseDay, courseDay)
                .join(coursePlace.place, place)
                .where(place.name.lower().contains(normalizedKeyword))
        ))
        .or(course.id.in(
            JPAExpressions
                .select(courseCountry.course.id)
                .from(courseCountry)
                .where(courseCountry.country.name.lower().contains(normalizedKeyword))
        ))
        .or(course.id.in(
            JPAExpressions
                .select(courseCity.course.id)
                .from(courseCity)
                .where(
                    courseCity.city.name.lower().contains(normalizedKeyword)
                        .or(courseCity.city.koreanName.lower().contains(normalizedKeyword))
                )
        ));
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
