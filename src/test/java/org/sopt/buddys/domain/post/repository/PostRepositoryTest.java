package org.sopt.buddys.domain.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.course.service.command.CourseSearchCondition;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostImage;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.sopt.buddys.domain.search.service.command.SearchSort;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class PostRepositoryTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired private PostRepository postRepository;
  @Autowired private CourseRepository courseRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Country country;
  private City city;
  private User viewer;
  private User author;
  private User withdrawn;
  private User suspended;
  private final LocalDate today = LocalDate.of(2026, 10, 20);

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("INSERT INTO country (name, iso_code) VALUES ('Test Country', 'ZZ')");
    Long countryId = jdbcTemplate.queryForObject(
        "SELECT id FROM country WHERE iso_code = 'ZZ'", Long.class);
    jdbcTemplate.update("INSERT INTO city (country_id, name, population) VALUES (?, 'Test City', 1)", countryId);
    Long cityId = jdbcTemplate.queryForObject(
        "SELECT id FROM city WHERE country_id = ?", Long.class, countryId);
    country = entityManager.find(Country.class, countryId);
    city = entityManager.find(City.class, cityId);
    viewer = saveUser("viewer", AccountStatus.ACTIVE);
    author = saveUser("author", AccountStatus.ACTIVE);
    withdrawn = saveUser("withdrawn", AccountStatus.ACTIVE);
    suspended = saveUser("suspended", AccountStatus.SUSPENDED);
  }

  @DisplayName("추천 조회는 이미지 조건과 기존 필터를 유지하며 탈퇴 작성자만 제외한다")
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void findRecommendedPosts_excludesWithdrawnAuthorsWithOrWithoutImage(boolean requireImage) {
    // given
    Post withImage = savePost(author, today);
    Post withoutImage = savePost(author, today);
    Post suspendedPost = savePost(suspended, today);
    Post withdrawnPost = savePost(withdrawn, today);
    Post ownPost = savePost(viewer, today);
    Post completed = savePost(author, today);
    completed.updateStatus(PostStatus.COMPLETED);
    Post deleted = savePost(author, today);
    deleted.softDelete(LocalDateTime.now());
    jdbcTemplate.update("INSERT INTO country (name, iso_code) VALUES ('Other Country', 'ZY')");
    Country otherCountry = entityManager.find(Country.class, jdbcTemplate.queryForObject(
        "SELECT id FROM country WHERE iso_code = 'ZY'", Long.class));
    Post otherCountryPost = savePost(author, today);
    otherCountryPost.update(otherCountry, city, today, today, "동행", "본문",
        CompanionType.FULL_TRIP, RecruitmentCountType.TWO);
    for (Post post : List.of(withImage, suspendedPost, withdrawnPost, ownPost, completed, deleted, otherCountryPost)) {
      entityManager.persist(new PostImage(post, "https://example.com/image.jpg", (short) 0));
    }
    withdrawn.withdraw();
    flushAndClear();

    // when
    List<Post> result = requireImage
        ? postRepository.findByCountryIdAndStatusAndAuthorIdNotAndHasImage(
            country.getId(), PostStatus.RECRUITING, viewer.getId())
        : postRepository.findByCountryIdAndStatusAndAuthorIdNot(
            country.getId(), PostStatus.RECRUITING, viewer.getId());

    // then
    List<Long> expectedIds = new ArrayList<>(List.of(withImage.getId(), suspendedPost.getId()));
    if (!requireImage) {
      expectedIds.add(withoutImage.getId());
    }
    assertThat(result).extracting(Post::getId)
        .containsExactlyInAnyOrderElementsOf(expectedIds)
        .doesNotContain(withdrawnPost.getId(), ownPost.getId(), completed.getId(),
            deleted.getId(), otherCountryPost.getId());
  }

  @DisplayName("마감 임박 조회는 탈퇴 작성자를 limit 전에 제외하고 기존 조건과 오래된 순서를 유지한다")
  @Test
  void findClosingSoonPosts_excludesWithdrawnBeforeLimitAndPreservesOrder() {
    // given
    Post withdrawnPost = savePost(withdrawn, today);
    setCreatedAt(withdrawnPost, 0);
    Post completed = savePost(author, today);
    completed.updateStatus(PostStatus.COMPLETED);
    setCreatedAt(completed, 1);
    Post deleted = savePost(author, today);
    deleted.softDelete(LocalDateTime.now());
    setCreatedAt(deleted, 2);
    Post yesterday = savePost(author, today.minusDays(1));
    Post tomorrow = savePost(author, today.plusDays(1));
    List<Long> expectedIds = new ArrayList<>();
    for (int index = 0; index < 5; index++) {
      Post post = savePost(index == 0 ? suspended : author, today);
      setCreatedAt(post, 10 + index);
      expectedIds.add(post.getId());
    }
    withdrawn.withdraw();
    flushAndClear();

    // when
    List<Post> result = postRepository.findClosingSoonPosts(today, 4);

    // then
    assertThat(result).extracting(Post::getId)
        .containsExactlyElementsOf(expectedIds.subList(0, 4))
        .doesNotContain(withdrawnPost.getId(), completed.getId(), deleted.getId(),
            yesterday.getId(), tomorrow.getId(), expectedIds.get(4));
  }

  @DisplayName("홈 필터는 일반 목록 및 통합 검색에서 사용하는 조회와 집계 조건에 영향을 주지 않는다")
  @Test
  void searchPosts_keepsWithdrawnAuthorsVisibleOutsideHome() {
    Post activePost = savePost(author, today);
    Post withdrawnPost = savePost(withdrawn, today);
    withdrawn.withdraw();
    flushAndClear();
    PostSearchCondition condition = PostSearchCondition.keywordOnly("동행");

    for (SearchSort sort : SearchSort.values()) {
      assertThat(postRepository.searchPosts(viewer.getId(), condition, sort, PageRequest.of(0, 20)))
          .extracting(Post::getId)
          .containsExactlyInAnyOrder(activePost.getId(), withdrawnPost.getId());
    }
    assertThat(postRepository.countPosts(condition)).isEqualTo(2);
  }

  @DisplayName("코스 목록은 기존처럼 탈퇴 작성자의 삭제되지 않은 코스를 포함한다")
  @Test
  void searchCourses_keepsExistingVisibilityForWithdrawnAuthors() {
    Course activeCourse = courseRepository.save(new Course(author, "정상 코스", null, today, today));
    Course withdrawnCourse = courseRepository.save(new Course(withdrawn, "탈퇴 코스", null, today, today));
    withdrawn.withdraw();
    flushAndClear();

    assertThat(courseRepository.searchCourses(new CourseSearchCondition(null, null), PageRequest.of(0, 20)))
        .extracting(Course::getId)
        .containsExactlyInAnyOrder(activeCourse.getId(), withdrawnCourse.getId());
  }

  private User saveUser(String name, AccountStatus status) {
    User user = User.builder().email(name + "@test.com").provider(AuthProvider.KAKAO)
        .providerId(name).nickname(name).accountStatus(status).build();
    entityManager.persist(user);
    return user;
  }

  private Post savePost(User user, LocalDate startDate) {
    return postRepository.save(new Post(user, country, city, "동행", "본문", startDate,
        startDate.plusDays(1), CompanionType.FULL_TRIP, RecruitmentCountType.TWO));
  }

  private void setCreatedAt(Post post, int dayOffset) {
    entityManager.flush();
    jdbcTemplate.update("UPDATE post SET created_at = ? WHERE id = ?",
        today.minusDays(30).atStartOfDay().plusDays(dayOffset), post.getId());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
