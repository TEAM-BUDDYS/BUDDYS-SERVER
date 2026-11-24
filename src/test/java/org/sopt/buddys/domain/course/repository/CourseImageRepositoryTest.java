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
import org.sopt.buddys.domain.course.entity.CoursePlace;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class CourseImageRepositoryTest extends IntegrationTestSupport {

  @Autowired private CourseRepository courseRepository;
  @Autowired private CourseDayRepository courseDayRepository;
  @Autowired private CourseImageRepository courseImageRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private CoursePlaceRepository coursePlaceRepository;
  @Autowired private PlaceRepository placeRepository;

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

  @DisplayName("장소가 그 장소 하나뿐인 일차의 사진만 최근 코스, 사진 순서대로 조회하고 삭제된 코스는 제외한다")
  @Test
  void findImageUrlsOfSinglePlaceDays_onlySinglePlaceDaysOfActiveCourses() {
    User author = userRepository.save(User.builder()
        .provider(AuthProvider.KAKAO).providerId("photo-author")
        .email("photo@test.com").nickname("작성자").build());
    Place target = savePlace("google-target");
    Place other = savePlace("google-other");

    Course older = saveCourse(author);
    CourseDay olderDay = saveDay(older, 1);
    linkPlace(olderDay, target);
    saveImage(olderDay, "older-course", 0);

    Course newer = saveCourse(author);
    CourseDay multiPlaceDay = saveDay(newer, 1);
    linkPlace(multiPlaceDay, target);
    linkPlace(multiPlaceDay, other);
    saveImage(multiPlaceDay, "multi-place-day", 0);
    CourseDay singlePlaceDay = saveDay(newer, 2);
    linkPlace(singlePlaceDay, target);
    saveImage(singlePlaceDay, "second-image", 1);
    saveImage(singlePlaceDay, "first-image", 0);

    Course deleted = saveCourse(author);
    deleted.delete();
    courseRepository.save(deleted);
    CourseDay deletedDay = saveDay(deleted, 1);
    linkPlace(deletedDay, target);
    saveImage(deletedDay, "deleted-course", 0);

    assertThat(courseImageRepository.findImageUrlsOfSinglePlaceDays("google-target", PageRequest.of(0, 10)))
        .containsExactly("first-image", "second-image", "older-course");
    assertThat(courseImageRepository.findImageUrlsOfSinglePlaceDays("google-target", PageRequest.of(0, 1)))
        .containsExactly("first-image");
    assertThat(courseImageRepository.findImageUrlsOfSinglePlaceDays("google-other", PageRequest.of(0, 1)))
        .isEmpty();
  }

  private Course saveCourse(User author) {
    return courseRepository.save(new Course(author, "코스", null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)));
  }

  private CourseDay saveDay(Course course, int dayNumber) {
    return courseDayRepository.save(new CourseDay(course, (short) dayNumber, null, null, null));
  }

  private void saveImage(CourseDay day, String url, int orderNo) {
    courseImageRepository.save(new CourseImage(day, url, (short) orderNo));
  }

  private Place savePlace(String googlePlaceId) {
    return placeRepository.save(Place.builder()
        .googlePlaceId(googlePlaceId).name("장소-" + googlePlaceId).category(PlaceCategory.CAFE).build());
  }

  private void linkPlace(CourseDay day, Place place) {
    coursePlaceRepository.save(new CoursePlace(day, place, (short) 0));
  }
}
