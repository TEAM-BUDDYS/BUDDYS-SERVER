package org.sopt.buddys.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseCompanion;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

class CourseRepositoryTest extends IntegrationTestSupport {

  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseCompanionRepository courseCompanionRepository;
  @Autowired private UserRepository userRepository;

  @DisplayName("삭제된 코스는 작성자와 동행 모두에게 조회되지 않는다")
  @Test
  void findParticipatedCourses_excludesDeletedCourses() {
    User user = saveUser("participant");
    User other = saveUser("other");
    Course deletedAuthored = saveCourse(user);
    Course deletedTagged = saveCourse(other);
    saveCompanion(deletedTagged, user);
    deleteCourse(deletedAuthored);
    deleteCourse(deletedTagged);

    Slice<Course> result = courseRepository.findParticipatedCoursesByUserId(
        user.getId(), pageRequest(0, 10));

    assertThat(result.getContent()).isEmpty();
  }

  @DisplayName("작성 코스와 동행 코스를 중복 없이 최신순으로 페이지 조회하고 무관한 코스는 제외한다")
  @Test
  void findParticipatedCourses_returnsAuthoredAndCompanionCoursesByLatest() {
    User user = saveUser("participant");
    User other = saveUser("other");
    User another = saveUser("another");
    Course first = saveCourse(user);
    Course second = saveCourse(other);
    saveCompanion(second, user);
    Course third = saveCourse(user);
    saveCompanion(third, other);
    Course fourth = saveCourse(other);
    saveCompanion(fourth, user);
    saveCompanion(fourth, another);
    Course unrelated = saveCourse(other);
    saveCompanion(unrelated, another);

    Slice<Course> firstPage = courseRepository.findParticipatedCoursesByUserId(
        user.getId(), pageRequest(0, 3));
    Slice<Course> secondPage = courseRepository.findParticipatedCoursesByUserId(
        user.getId(), pageRequest(1, 3));

    assertThat(firstPage.getContent())
        .extracting(Course::getId)
        .containsExactly(fourth.getId(), third.getId(), second.getId());
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(secondPage.getContent())
        .extracting(Course::getId)
        .containsExactly(first.getId());
    assertThat(secondPage.hasNext()).isFalse();
  }

  private Pageable pageRequest(int page, int size) {
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
  }

  private User saveUser(String providerId) {
    return userRepository.save(User.builder()
        .provider(AuthProvider.KAKAO).providerId(providerId)
        .email(providerId + "@test.com").nickname(providerId).build());
  }

  private Course saveCourse(User author) {
    return courseRepository.save(new Course(author, "코스", null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)));
  }

  private void saveCompanion(Course course, User user) {
    courseCompanionRepository.save(new CourseCompanion(course, user));
  }

  private void deleteCourse(Course course) {
    course.delete();
    courseRepository.save(course);
  }
}
