package org.sopt.buddys.domain.course.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseDay;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;

class CourseImageRepositoryTest extends IntegrationTestSupport {

  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseDayRepository courseDayRepository;
  @Autowired private CourseImageRepository courseImageRepository;
  @Autowired private UserRepository userRepository;

  @DisplayName("요청한 코스의 1일차 첫 사진만 조회하고 해당 사진이 없는 코스는 제외한다")
  @Test
  void findThumbnails_returnsOnlyOneImagePerRequestedCourse() {
    User author = userRepository.save(User.builder()
        .provider(AuthProvider.KAKAO).providerId("thumbnail-author")
        .email("thumbnail@test.com").nickname("작성자").build());
    Course first = saveCourse(author);
    Course second = saveCourse(author);
    Course empty = saveCourse(author);
    Course withoutDayOne = saveCourse(author);
    Course excluded = saveCourse(author);

    CourseDay later = saveDay(first, 2);
    saveImage(later, "later-day", 0);
    CourseDay earliest = saveDay(first, 1);
    saveImage(earliest, "later-order", 2);
    saveImage(earliest, "first-image", 0);
    saveImage(saveDay(second, 1), "second-course-image", 0);
    saveImage(saveDay(withoutDayOne, 3), "not-day-one", 0);
    saveImage(saveDay(excluded, 1), "excluded", 0);

    var result = courseImageRepository.findThumbnailImageUrlsByCourseIds(
        List.of(first.getId(), second.getId(), empty.getId(), withoutDayOne.getId()));

    assertThat(result)
        .extracting(CourseImageRepository.CourseImageUrlProjection::getCourseId,
            CourseImageRepository.CourseImageUrlProjection::getImageUrl)
        .containsExactlyInAnyOrder(
            tuple(first.getId(), "first-image"),
            tuple(second.getId(), "second-course-image"));
  }

  private Course saveCourse(User author) {
    return courseRepository.save(new Course(author, "코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)));
  }

  private CourseDay saveDay(Course course, int dayNumber) {
    return courseDayRepository.save(new CourseDay(course, (short) dayNumber, null));
  }

  private void saveImage(CourseDay day, String url, int orderNo) {
    courseImageRepository.save(new CourseImage(day, url, (short) orderNo));
  }
}
