package org.sopt.buddys.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.recommendation.code.RecommendationErrorCode;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

  @InjectMocks
  private RecommendationService recommendationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserTagRepository userTagRepository;

  @DisplayName("같은 파견 국가 사용자 중 태그 일치율이 높은 순서로 추천한다")
  @Test
  void getExchangeCountryRecommendedUsers_sortsBySimilarity() {
    // given
    Long userId = 1L;
    Country france = mock(Country.class);
    given(france.getId()).willReturn(10L);
    User me = createUser(userId, "나", france);
    User highMatch = createUser(2L, "높은일치", france);
    User lowMatch = createUser(3L, "낮은일치", france);

    given(userRepository.findByIdWithExchangeCountry(userId)).willReturn(Optional.of(me));
    given(userRepository.findByExchangeCountryIdWithExchangeCountry(10L, userId))
        .willReturn(List.of(lowMatch, highMatch));
    given(userTagRepository.findAllByUserIdIn(List.of(3L, 2L, 1L))).willReturn(List.of(
        new TestUserTagBulkProjection(1L, 1L, TagType.ACTIVITY),
        new TestUserTagBulkProjection(1L, 2L, TagType.INTEREST),
        new TestUserTagBulkProjection(1L, 3L, TagType.TRAVEL_STYLE),
        new TestUserTagBulkProjection(2L, 1L, TagType.ACTIVITY),
        new TestUserTagBulkProjection(2L, 2L, TagType.INTEREST),
        new TestUserTagBulkProjection(2L, 3L, TagType.TRAVEL_STYLE),
        new TestUserTagBulkProjection(3L, 99L, TagType.ACTIVITY)
    ));

    // when
    List<RecommendedUserResult> results = recommendationService.getExchangeCountryRecommendedUsers(userId, 5);

    // then
    assertThat(results).extracting(result -> result.user().getId())
        .containsExactly(2L, 3L);
    assertThat(results.get(0).totalSimilarity()).isEqualTo(1.0);
    assertThat(results.get(1).totalSimilarity()).isEqualTo(0.0);
  }

  @DisplayName("size만큼 추천 사용자를 제한한다")
  @Test
  void getExchangeCountryRecommendedUsers_limitsBySize() {
    // given
    Long userId = 1L;
    Country france = mock(Country.class);
    given(france.getId()).willReturn(10L);
    User me = createUser(userId, "나", france);
    User first = createUser(2L, "첫번째", france);
    User second = createUser(3L, "두번째", france);

    given(userRepository.findByIdWithExchangeCountry(userId)).willReturn(Optional.of(me));
    given(userRepository.findByExchangeCountryIdWithExchangeCountry(10L, userId))
        .willReturn(List.of(first, second));
    given(userTagRepository.findAllByUserIdIn(List.of(2L, 3L, 1L))).willReturn(List.of());

    // when
    List<RecommendedUserResult> results = recommendationService.getExchangeCountryRecommendedUsers(userId, 1);

    // then
    assertThat(results).hasSize(1);
  }

  @DisplayName("내 파견 국가가 설정되어 있지 않으면 예외가 발생한다")
  @Test
  void getExchangeCountryRecommendedUsers_withoutExchangeCountry_throwsException() {
    // given
    Long userId = 1L;
    User me = createUser(userId, "나", null);

    given(userRepository.findByIdWithExchangeCountry(userId)).willReturn(Optional.of(me));

    // when, then
    assertThatThrownBy(() -> recommendationService.getExchangeCountryRecommendedUsers(userId, 5))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(RecommendationErrorCode.EXCHANGE_COUNTRY_NOT_SET)
        );
  }

  private User createUser(Long id, String nickname, Country exchangeCountry) {
    return User.builder()
        .id(id)
        .provider(AuthProvider.KAKAO)
        .providerId("provider-" + id)
        .email("user" + id + "@test.com")
        .nickname(nickname)
        .exchangeCountry(exchangeCountry)
        .build();
  }

  private record TestUserTagBulkProjection(
      Long userId,
      Long tagId,
      TagType tagType
  ) implements UserTagRepository.UserTagBulkProjection {

    @Override
    public Long getUserId() {
      return userId;
    }

    @Override
    public Long getTagId() {
      return tagId;
    }

    @Override
    public TagType getTagType() {
      return tagType;
    }
  }
}
