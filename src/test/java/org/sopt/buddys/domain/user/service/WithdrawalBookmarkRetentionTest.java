package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.entity.MagazineBookmark;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceBookmark;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostBookmark;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(UserService.class)
class WithdrawalBookmarkRetentionTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired private EntityManager entityManager;
  @Autowired private UserService userService;

  @Test
  void withdraw_preservesAllBookmarksAndAuthoredContent() {
    User withdrawing = persistUser("withdrawing");
    User other = persistUser("other");
    Country country = BeanUtils.instantiateClass(Country.class);
    ReflectionTestUtils.setField(country, "name", "테스트국가");
    ReflectionTestUtils.setField(country, "isoCode", "ZZ");
    entityManager.persist(country);
    City city = BeanUtils.instantiateClass(City.class);
    ReflectionTestUtils.setField(city, "country", country);
    ReflectionTestUtils.setField(city, "name", "테스트도시");
    ReflectionTestUtils.setField(city, "population", 1L);
    entityManager.persist(city);
    LocalDate date = LocalDate.of(2026, 9, 1);
    Post post = new Post(withdrawing, country, city, "제목", "내용", date, date,
        CompanionType.FULL_TRIP, RecruitmentCountType.ONE);
    Course course = new Course(withdrawing, "코스", "내용", null, date, date);
    Magazine magazine = new Magazine("매거진", "요약", "https://example.com/image",
        "https://example.com", date);
    entityManager.persist(post);
    entityManager.persist(course);
    entityManager.persist(magazine);
    Place place = Place.builder().googlePlaceId("retained-place").name("장소")
        .category(PlaceCategory.values()[0]).country(country).city(city).build();
    entityManager.persist(place);
    for (User user : new User[]{withdrawing, other}) {
      entityManager.persist(new PlaceBookmark(user, place));
      entityManager.persist(new PostBookmark(user, post));
      entityManager.persist(new CourseBookmark(user, course));
      entityManager.persist(new MagazineBookmark(user, magazine));
    }
    entityManager.flush();
    entityManager.clear();

    userService.withdraw(withdrawing.getId());
    entityManager.flush();
    entityManager.clear();

    for (String entity : new String[]{"PlaceBookmark", "PostBookmark", "CourseBookmark", "MagazineBookmark"}) {
      assertThat(bookmarkCount(entity, withdrawing.getId())).isEqualTo(1);
      assertThat(bookmarkCount(entity, other.getId())).isEqualTo(1);
    }
    assertThat(entityManager.find(Post.class, post.getId()).getDeletedAt()).isNull();
    assertThat(entityManager.find(Course.class, course.getId()).getDeletedAt()).isNull();
    assertThat(entityManager.find(Post.class, post.getId()).getContent()).isEqualTo("내용");
    assertThat(entityManager.find(Course.class, course.getId()).getContent()).isEqualTo("내용");
    User withdrawn = entityManager.find(User.class, withdrawing.getId());
    assertThat(withdrawn.getDeletedAt()).isNotNull();
    assertThat(withdrawn.getDisplayNickname()).isEqualTo("탈퇴한 사용자");
  }

  private User persistUser(String name) {
    User user = User.builder().email(name + "@example.com").provider(AuthProvider.KAKAO)
        .providerId(name).nickname(name).build();
    entityManager.persist(user);
    return user;
  }

  private long bookmarkCount(String entity, Long userId) {
    return entityManager.createQuery(
        "select count(b) from " + entity + " b where b.user.id = :userId", Long.class)
        .setParameter("userId", userId)
        .getSingleResult();
  }
}
