package org.sopt.buddys.domain.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.repository.PlaceBookmarkRepository;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PlaceBookmarkTransactionServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private PlaceBookmarkTransactionService placeBookmarkTransactionService;

  @Autowired
  private PlaceBookmarkRepository placeBookmarkRepository;

  @Autowired
  private PlaceRepository placeRepository;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  private void cleanUp() {
    placeBookmarkRepository.deleteAllInBatch();
    placeRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @DisplayName("같은 장소를 반복 저장해도 북마크 한 건만 생성된다")
  @Test
  void saveBookmark_repeatedCalls_areIdempotent() {
    // given
    User user = userRepository.save(createUser());
    Place place = placeRepository.save(createPlace());

    // when
    placeBookmarkTransactionService.saveBookmark(user.getId(), place.getId());
    placeBookmarkTransactionService.saveBookmark(user.getId(), place.getId());

    // then
    assertThat(placeBookmarkRepository.count()).isOne();
  }

  @DisplayName("동시에 같은 장소를 최초 저장해도 두 요청 모두 성공하고 북마크는 한 건만 생성된다")
  @Test
  void saveBookmark_concurrentFirstRequests_areIdempotent() throws Exception {
    // given
    User user = userRepository.save(createUser());
    Place place = placeRepository.save(createPlace());
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch readyLatch = new CountDownLatch(2);
    CountDownLatch startLatch = new CountDownLatch(1);

    try {
      Future<?> first = executorService.submit(
          () -> saveBookmarkAfterSignal(user.getId(), place.getId(), readyLatch, startLatch));
      Future<?> second = executorService.submit(
          () -> saveBookmarkAfterSignal(user.getId(), place.getId(), readyLatch, startLatch));

      assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
      startLatch.countDown();

      first.get(10, TimeUnit.SECONDS);
      second.get(10, TimeUnit.SECONDS);
      assertThat(placeBookmarkRepository.count()).isOne();
    } finally {
      executorService.shutdownNow();
      assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  private void saveBookmarkAfterSignal(
      Long userId,
      Long placeId,
      CountDownLatch readyLatch,
      CountDownLatch startLatch
  ) {
    readyLatch.countDown();
    try {
      if (!startLatch.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("동시 저장 시작 신호를 기다리는 중 시간 초과");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
    placeBookmarkTransactionService.saveBookmark(userId, placeId);
  }

  private User createUser() {
    return User.builder()
        .email("user@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("provider-user")
        .nickname("사용자")
        .build();
  }

  private Place createPlace() {
    return Place.builder()
        .googlePlaceId("ChIJN1t_tDeuEmsRUsoyG83frY4")
        .name("루브르 박물관")
        .category(PlaceCategory.TOURISM)
        .address("Rue de Rivoli, 75001 Paris")
        .latitude(new BigDecimal("48.8606"))
        .longitude(new BigDecimal("2.3376"))
        .build();
  }
}
