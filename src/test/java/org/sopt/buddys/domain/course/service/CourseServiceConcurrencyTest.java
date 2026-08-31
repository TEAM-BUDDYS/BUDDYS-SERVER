package org.sopt.buddys.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
import org.sopt.buddys.domain.course.service.command.CourseDayCommand;
import org.sopt.buddys.domain.course.service.command.CoursePlaceCommand;
import org.sopt.buddys.domain.course.service.command.CreateCourseCommand;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class CourseServiceConcurrencyTest extends IntegrationTestSupport {

  @Autowired
  private CourseService courseService;

  @Autowired
  private CourseBookmarkRepository courseBookmarkRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlaceRepository placeRepository;

  @DisplayName("동시에 같은 코스를 저장해도 북마크는 1개만 생성되고 두 요청 모두 예외 없이 끝난다")
  @Test
  void bookmarkCourse_concurrentRequest_createsOnlyOneBookmark() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Course course = courseService.createCourse(
        author.getId(),
        createDefaultCommand(countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId));

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      Future<Void> first = executorService.submit(
          () -> bookmarkAtSameTime(viewer.getId(), course.getId(), readyLatch, startLatch));
      Future<Void> second = executorService.submit(
          () -> bookmarkAtSameTime(viewer.getId(), course.getId(), readyLatch, startLatch));

      assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();

      // when
      startLatch.countDown();
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);

      // then
      CourseBookmarkRepository.BookmarkSummary summary =
          courseBookmarkRepository.findBookmarkSummary(viewer.getId(), course.getId());
      assertThat(summary.getTotalCount()).isEqualTo(1L);
    } finally {
      executorService.shutdownNow();
    }
  }

  private Void bookmarkAtSameTime(
      Long userId,
      Long courseId,
      CountDownLatch readyLatch,
      CountDownLatch startLatch
  ) throws InterruptedException {
    readyLatch.countDown();
    assertThat(startLatch.await(3, TimeUnit.SECONDS)).isTrue();
    courseService.bookmarkCourse(userId, courseId);
    return null;
  }

  @DisplayName("동시에 같은 신규 장소를 담은 코스를 생성해도 place 캐시는 1행만 생기고 두 요청 모두 성공한다")
  @Test
  void createCourse_concurrentSameNewPlace_savesOnePlaceRow() throws Exception {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    String sharedGooglePlaceId = "ChIJ-concurrent-place";

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      Future<Course> first = executorService.submit(() -> createCourseWithPlaceAtSameTime(
          author.getId(), countryId, cityId, tagId, sharedGooglePlaceId, readyLatch, startLatch));
      Future<Course> second = executorService.submit(() -> createCourseWithPlaceAtSameTime(
          author.getId(), countryId, cityId, tagId, sharedGooglePlaceId, readyLatch, startLatch));

      assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();

      // when
      startLatch.countDown();
      Course firstCourse = first.get(5, TimeUnit.SECONDS);
      Course secondCourse = second.get(5, TimeUnit.SECONDS);

      // then
      assertThat(firstCourse.getId()).isNotNull();
      assertThat(secondCourse.getId()).isNotNull();
      assertThat(firstCourse.getId()).isNotEqualTo(secondCourse.getId());
      assertThat(placeRepository.findAll())
          .filteredOn(place -> place.getGooglePlaceId().equals(sharedGooglePlaceId))
          .hasSize(1);
    } finally {
      executorService.shutdownNow();
    }
  }

  private Course createCourseWithPlaceAtSameTime(
      Long authorId,
      Long countryId,
      Long cityId,
      Long tagId,
      String googlePlaceId,
      CountDownLatch readyLatch,
      CountDownLatch startLatch
  ) throws InterruptedException {
    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"),
            List.of(new CoursePlaceCommand(
                googlePlaceId, "콜로세움", "TOURISM", null, null, (short) 0, null, null)))),
        null
    );
    readyLatch.countDown();
    assertThat(startLatch.await(3, TimeUnit.SECONDS)).isTrue();
    return courseService.createCourse(authorId, command);
  }

  private CreateCourseCommand createDefaultCommand(
      Long countryId, Long cityId, LocalDate startDate, LocalDate endDate, Long tagId
  ) {
    return new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        startDate, endDate,
        List.of(tagId), null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );
  }

  private User createUser(String email, String providerId, String nickname) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .build();
  }

  private Long insertCountry(String name, String isoCode) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO country (name, iso_code) VALUES (?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, isoCode);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private Long insertCity(Long countryId, String name, String koreanName, Long population) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO city (country_id, name, korean_name, population) VALUES (?, ?, ?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setLong(1, countryId);
          preparedStatement.setString(2, name);
          preparedStatement.setString(3, koreanName);
          preparedStatement.setLong(4, population);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private Long insertTag(String name, String tagType) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO tag (name, tag_type) VALUES (?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, tagType);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }
}
