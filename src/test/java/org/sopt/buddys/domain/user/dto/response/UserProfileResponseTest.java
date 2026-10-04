package org.sopt.buddys.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;
import org.sopt.buddys.domain.user.service.result.UserProfileResult.OrderedTagResult;

class UserProfileResponseTest {

  @DisplayName("학교 이메일과 파견교 서류 인증 여부를 각각 반환한다")
  @Test
  void from_returnsEachVerificationStatus() {
    // given
    User user = createUser(true, true);
    UserProfileResult result = new UserProfileResult(user, List.of());

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.universityEmailVerified()).isTrue();
    assertThat(response.exchangeDocumentVerified()).isTrue();
  }

  @DisplayName("학교 이메일만 인증된 경우 각 인증 여부를 반환한다")
  @Test
  void from_universityEmailOnlyVerified_returnsEachVerificationStatus() {
    // given
    User user = createUser(true, false);
    UserProfileResult result = new UserProfileResult(user, List.of());

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.universityEmailVerified()).isTrue();
    assertThat(response.exchangeDocumentVerified()).isFalse();
  }

  @DisplayName("추가 인증이 없으면 모든 인증 여부를 false로 반환한다")
  @Test
  void from_notVerified_returnsFalseVerificationStatuses() {
    // given
    User user = createUser(false, false);
    UserProfileResult result = new UserProfileResult(user, List.of());

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.universityEmailVerified()).isFalse();
    assertThat(response.exchangeDocumentVerified()).isFalse();
  }

  @DisplayName("타 사용자 프로필도 학교 이메일과 파견교 서류 인증 여부를 각각 반환한다")
  @Test
  void publicProfile_returnsEachVerificationStatus() {
    // given
    User user = createUser(true, true);
    UserProfileResult result = new UserProfileResult(user, List.of());

    // when
    UserPublicProfileResponse response = UserPublicProfileResponse.from(result);

    // then
    assertThat(response.universityEmailVerified()).isTrue();
    assertThat(response.exchangeDocumentVerified()).isTrue();
  }

  @DisplayName("내 프로필은 전체 태그를 저장 순서대로 반환한다")
  @Test
  void from_returnsAllOrderedTags() {
    // given
    User user = createUser(false, false);
    UserProfileResult result = new UserProfileResult(user, List.of(
        new OrderedTagResult(27L, "계획형", TagType.TRAVEL_STYLE, 0),
        new OrderedTagResult(1L, "여행", TagType.ACTIVITY, 1),
        new OrderedTagResult(13L, "자연", TagType.INTEREST, 2),
        new OrderedTagResult(28L, "즉흥형", TagType.TRAVEL_STYLE, 3)
    ));

    // when
    UserProfileResponse response = UserProfileResponse.from(result);

    // then
    assertThat(response.orderedTags())
        .extracting(OrderedTagResponse::id)
        .containsExactly(27L, 1L, 13L, 28L);
  }

  @DisplayName("타 사용자 프로필은 저장 순서 상위 3개 태그만 반환한다")
  @Test
  void publicProfile_returnsOnlyTopThreeTags() {
    // given
    User user = createUser(false, false);
    UserProfileResult result = new UserProfileResult(user, List.of(
        new OrderedTagResult(27L, "계획형", TagType.TRAVEL_STYLE, 0),
        new OrderedTagResult(1L, "여행", TagType.ACTIVITY, 1),
        new OrderedTagResult(13L, "자연", TagType.INTEREST, 2),
        new OrderedTagResult(28L, "즉흥형", TagType.TRAVEL_STYLE, 3)
    ));

    // when
    UserPublicProfileResponse response = UserPublicProfileResponse.from(result);

    // then
    assertThat(response.representativeTags())
        .extracting(OrderedTagResponse::id)
        .containsExactly(27L, 1L, 13L);
  }

  private User createUser(boolean universityVerified, boolean exchangeVerified) {
    return User.builder()
        .id(1L)
        .provider(AuthProvider.KAKAO)
        .providerId("12345")
        .email("test@kakao.com")
        .nickname("버디")
        .universityVerified(universityVerified)
        .exchangeVerified(exchangeVerified)
        .build();
  }
}
