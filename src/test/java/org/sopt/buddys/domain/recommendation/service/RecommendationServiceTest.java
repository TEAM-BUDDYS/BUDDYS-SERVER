package org.sopt.buddys.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.repository.PostTagRepository;
import org.sopt.buddys.domain.recommendation.code.ExchangeCountryRecommendationErrorCode;
import org.sopt.buddys.domain.recommendation.code.RecommendationErrorCode;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedPostResult;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository.UserTagBulkProjection;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecommendationServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long COUNTRY_ID = 31L;

  @InjectMocks
  private RecommendationService recommendationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserTagRepository userTagRepository;

  @Mock
  private PostRepository postRepository;

  @Mock
  private PostTagRepository postTagRepository;

  @Mock
  private PostImageRepository postImageRepository;

  @Mock
  private UserService userService;

  @Spy
  private TagSimilarityCalculator similarityCalculator = new TagSimilarityCalculator();

  @DisplayName("관심 국가가 같고 태그 유사도가 높은 순으로 추천 사용자를 정렬한다")
  @Test
  void getRecommendedUsers_sortsBySimilarityDesc() {
    // given
    User me = createUser(USER_ID, COUNTRY_ID);
    User highSimilarity = createUser(2L, COUNTRY_ID);
    User lowSimilarity = createUser(3L, COUNTRY_ID);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(me));
    given(userRepository.findByInterestCountryIdAndIdNotAndDeletedAtIsNull(COUNTRY_ID, USER_ID))
        .willReturn(List.of(highSimilarity, lowSimilarity));
    given(userService.isOnboardingCompleted(any(), anyLong())).willReturn(true);

    given(userTagRepository.findAllByUserIdIn(List.of(USER_ID)))
        .willReturn(List.of(tagRow(USER_ID, 1L, TagType.ACTIVITY), tagRow(USER_ID, 2L, TagType.INTEREST)));
    given(userTagRepository.findAllByUserIdIn(List.of(2L, 3L)))
        .willReturn(List.of(
            tagRow(2L, 1L, TagType.ACTIVITY), // 완전히 겹침
            tagRow(2L, 2L, TagType.INTEREST),
            tagRow(3L, 99L, TagType.ACTIVITY) // 전혀 안 겹침
        ));
    given(postRepository.countByAuthorIdIn(List.of(2L, 3L))).willReturn(List.of());

    // when
    List<RecommendedUserResult> result = recommendationService.getRecommendedUsers(USER_ID, 10);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).user().getId()).isEqualTo(2L);
    assertThat(result.get(0).totalSimilarity()).isEqualTo(0.7);
    assertThat(result.get(1).user().getId()).isEqualTo(3L);
    assertThat(result.get(1).totalSimilarity()).isEqualTo(0.0);
  }

  @DisplayName("온보딩을 완료하지 않은 후보는 추천에서 제외한다")
  @Test
  void getRecommendedUsers_excludesNotOnboardedCandidates() {
    // given
    User me = createUser(USER_ID, COUNTRY_ID);
    User notOnboarded = createUser(2L, COUNTRY_ID);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(me));
    given(userRepository.findByInterestCountryIdAndIdNotAndDeletedAtIsNull(COUNTRY_ID, USER_ID)).willReturn(List.of(notOnboarded));
    given(userService.isOnboardingCompleted(eq(notOnboarded), anyLong())).willReturn(false);
    given(userTagRepository.findAllByUserIdIn(List.of(USER_ID))).willReturn(List.of());
    given(userTagRepository.findAllByUserIdIn(List.of(2L))).willReturn(List.of());
    given(postRepository.countByAuthorIdIn(List.of(2L))).willReturn(List.of());

    // when
    List<RecommendedUserResult> result = recommendationService.getRecommendedUsers(USER_ID, 10);

    // then
    assertThat(result).isEmpty();
  }

  @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생한다")
  @Test
  void getRecommendedUsers_userNotFound_throwsException() {
    // given
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> recommendationService.getRecommendedUsers(USER_ID, 1))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
        );
  }

  @DisplayName("관심 국가를 설정하지 않았으면 예외가 발생한다")
  @Test
  void getRecommendedUsers_interestCountryNotSet_throwsException() {
    // given
    User me = User.builder()
        .id(USER_ID)
        .provider(AuthProvider.KAKAO)
        .providerId("provider-id")
        .email("test@test.com")
        .nickname("버디")
        .build();

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(me));

    // when, then
    assertThatThrownBy(() -> recommendationService.getRecommendedUsers(USER_ID, 1))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(RecommendationErrorCode.INTEREST_COUNTRY_NOT_SET)
        );
  }

  @DisplayName("추천 게시글은 태그 유사도가 높은 순으로 정렬되고 동점이면 조회수가 높은 순이다")
  @Test
  void getRecommendedPosts_sortsBySimilarityThenViewCount() {
    // given
    User me = createUser(USER_ID, COUNTRY_ID);
    Post lowViewCount = mockPost(10L, 5L);
    Post highViewCount = mockPost(11L, 100L);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(me));
    given(userTagRepository.findAllByUserIdIn(List.of(USER_ID))).willReturn(List.of());
    given(postRepository.findByCountryIdAndStatusAndAuthorIdNot(COUNTRY_ID, PostStatus.RECRUITING, USER_ID))
        .willReturn(List.of(lowViewCount, highViewCount));
    given(postTagRepository.findAllByPostIdIn(List.of(10L, 11L))).willReturn(List.of());
    given(postImageRepository.findThumbnailImageUrlsByPostIds(List.of(10L, 11L))).willReturn(List.of());

    // when
    List<RecommendedPostResult> result = recommendationService.getRecommendedPosts(USER_ID, 10);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).post().getId()).isEqualTo(11L);
    assertThat(result.get(1).post().getId()).isEqualTo(10L);
  }

  @DisplayName("관심 국가를 설정하지 않았으면 추천 게시글 조회 시 예외가 발생한다")
  @Test
  void getRecommendedPosts_interestCountryNotSet_throwsException() {
    // given
    User me = User.builder()
        .id(USER_ID)
        .provider(AuthProvider.KAKAO)
        .providerId("provider-id")
        .email("test@test.com")
        .nickname("버디")
        .build();

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(me));

    // when, then
    assertThatThrownBy(() -> recommendationService.getRecommendedPosts(USER_ID, 1))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(RecommendationErrorCode.INTEREST_COUNTRY_NOT_SET)
        );
  }

  @DisplayName("추천 게시글 조회 시 본인이 작성한 게시글은 조회 대상에서 제외한다")
  @Test
  void getRecommendedPosts_excludesOwnPosts() {
    // given
    User me = createUser(USER_ID, COUNTRY_ID);
    Post othersPost = mockPost(10L, 5L);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(me));
    given(userTagRepository.findAllByUserIdIn(List.of(USER_ID))).willReturn(List.of());
    given(postRepository.findByCountryIdAndStatusAndAuthorIdNot(COUNTRY_ID, PostStatus.RECRUITING, USER_ID))
        .willReturn(List.of(othersPost));
    given(postTagRepository.findAllByPostIdIn(List.of(10L))).willReturn(List.of());
    given(postImageRepository.findThumbnailImageUrlsByPostIds(List.of(10L))).willReturn(List.of());

    // when
    recommendationService.getRecommendedPosts(USER_ID, 10);

    // then
    verify(postRepository).findByCountryIdAndStatusAndAuthorIdNot(COUNTRY_ID, PostStatus.RECRUITING, USER_ID);
  }

  @DisplayName("size 파라미터만큼만 추천 게시글을 반환한다")
  @Test
  void getRecommendedPosts_limitsResultBySize() {
    // given
    User me = createUser(USER_ID, COUNTRY_ID);
    Post lowest = mockPost(10L, 1L);
    Post middle = mockPost(11L, 5L);
    Post highest = mockPost(12L, 10L);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(me));
    given(userTagRepository.findAllByUserIdIn(List.of(USER_ID))).willReturn(List.of());
    given(postRepository.findByCountryIdAndStatusAndAuthorIdNot(COUNTRY_ID, PostStatus.RECRUITING, USER_ID))
        .willReturn(List.of(lowest, middle, highest));
    given(postTagRepository.findAllByPostIdIn(List.of(10L, 11L, 12L))).willReturn(List.of());
    given(postImageRepository.findThumbnailImageUrlsByPostIds(List.of(10L, 11L, 12L))).willReturn(List.of());

    // when
    List<RecommendedPostResult> result = recommendationService.getRecommendedPosts(USER_ID, 2);

    // then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).post().getId()).isEqualTo(12L);
    assertThat(result.get(1).post().getId()).isEqualTo(11L);
  }

  @DisplayName("같은 파견 국가 사용자 중 태그 일치율이 높은 순서로 추천한다")
  @Test
  void getExchangeCountryRecommendedUsers_sortsBySimilarity() {
    // given
    Long userId = 1L;
    Country france = mock(Country.class);
    given(france.getId()).willReturn(10L);
    User me = createUserWithExchangeCountry(userId, "나", france);
    User highMatch = createUserWithExchangeCountry(2L, "높은일치", france);
    User lowMatch = createUserWithExchangeCountry(3L, "낮은일치", france);

    given(userRepository.findByIdWithExchangeCountry(userId)).willReturn(Optional.of(me));
    given(userRepository.findByExchangeCountryIdWithExchangeCountry(10L, userId))
        .willReturn(List.of(lowMatch, highMatch));
    given(userTagRepository.findAllByUserIdIn(List.of(3L, 2L, 1L))).willReturn(List.of(
        tagRow(1L, 1L, TagType.ACTIVITY),
        tagRow(1L, 2L, TagType.INTEREST),
        tagRow(1L, 3L, TagType.TRAVEL_STYLE),
        tagRow(2L, 1L, TagType.ACTIVITY),
        tagRow(2L, 2L, TagType.INTEREST),
        tagRow(2L, 3L, TagType.TRAVEL_STYLE),
        tagRow(3L, 99L, TagType.ACTIVITY)
    ));

    // when
    List<RecommendedUserResult> results =
        recommendationService.getExchangeCountryRecommendedUsers(userId, 5);

    // then
    assertThat(results).extracting(result -> result.user().getId())
        .containsExactly(2L, 3L);
    assertThat(results.get(0).totalSimilarity()).isEqualTo(1.0);
    assertThat(results.get(1).totalSimilarity()).isEqualTo(0.0);
  }

  @DisplayName("size만큼 파견 국가 추천 사용자를 제한한다")
  @Test
  void getExchangeCountryRecommendedUsers_limitsBySize() {
    // given
    Long userId = 1L;
    Country france = mock(Country.class);
    given(france.getId()).willReturn(10L);
    User me = createUserWithExchangeCountry(userId, "나", france);
    User first = createUserWithExchangeCountry(2L, "첫번째", france);
    User second = createUserWithExchangeCountry(3L, "두번째", france);

    given(userRepository.findByIdWithExchangeCountry(userId)).willReturn(Optional.of(me));
    given(userRepository.findByExchangeCountryIdWithExchangeCountry(10L, userId))
        .willReturn(List.of(first, second));
    given(userTagRepository.findAllByUserIdIn(List.of(2L, 3L, 1L))).willReturn(List.of());

    // when
    List<RecommendedUserResult> results =
        recommendationService.getExchangeCountryRecommendedUsers(userId, 1);

    // then
    assertThat(results).hasSize(1);
  }

  @DisplayName("내 파견 국가가 설정되어 있지 않으면 예외가 발생한다")
  @Test
  void getExchangeCountryRecommendedUsers_withoutExchangeCountry_throwsException() {
    // given
    Long userId = 1L;
    User me = createUserWithExchangeCountry(userId, "나", null);

    given(userRepository.findByIdWithExchangeCountry(userId)).willReturn(Optional.of(me));

    // when, then
    assertThatThrownBy(() -> recommendationService.getExchangeCountryRecommendedUsers(userId, 5))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(
                ExchangeCountryRecommendationErrorCode.EXCHANGE_COUNTRY_NOT_SET
            )
        );
  }

  private User createUserWithExchangeCountry(Long id, String nickname, Country exchangeCountry) {
    return User.builder()
        .id(id)
        .provider(AuthProvider.KAKAO)
        .providerId("provider-" + id)
        .email("user" + id + "@test.com")
        .nickname(nickname)
        .exchangeCountry(exchangeCountry)
        .build();
  }

  private User createUser(Long id, Long interestCountryId) {
    Country country = mock(Country.class);
    given(country.getId()).willReturn(interestCountryId);

    return User.builder()
        .id(id)
        .provider(AuthProvider.KAKAO)
        .providerId("provider-" + id)
        .email("test" + id + "@test.com")
        .nickname("버디" + id)
        .interestCountry(country)
        .build();
  }

  private Post mockPost(Long id, long viewCount) {
    Post post = mock(Post.class);
    given(post.getId()).willReturn(id);
    given(post.getViewCount()).willReturn(viewCount);
    return post;
  }

  private UserTagBulkProjection tagRow(Long userId, Long tagId, TagType tagType) {
    return new UserTagBulkProjection() {
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
    };
  }
}