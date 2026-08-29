package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.dto.response.OrderedTagResponse;
import org.sopt.buddys.domain.user.dto.response.ProfileEditResponse;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.Gender;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.service.command.UpdateProfileCommand;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class UserProfileEditServiceTest {

  @InjectMocks
  private UserProfileEditService userProfileEditService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserTagRepository userTagRepository;

  @Mock
  private TagRepository tagRepository;

  @DisplayName("프로필과 카테고리별 태그를 함께 수정한다")
  @Test
  void updateProfile_updatesProfileAndReplacesTags() {
    // given
    Long userId = 1L;
    User user = createUser(userId);
    Tag activity = createTag(1L, "여행", TagType.ACTIVITY);
    Tag interest = createTag(13L, "자연", TagType.INTEREST);
    Tag secondInterest = createTag(14L, "도시", TagType.INTEREST);
    Tag travelStyle = createTag(27L, "계획형", TagType.TRAVEL_STYLE);
    Tag secondTravelStyle = createTag(28L, "즉흥형", TagType.TRAVEL_STYLE);
    UpdateProfileCommand command = new UpdateProfileCommand(
        "새닉네임",
        Gender.MALE,
        LocalDate.of(2001, 2, 3),
        "새로운 자기소개",
        List.of(27L, 1L, 13L, 28L, 14L)
    );

    given(userRepository.findByIdForProfileUpdate(userId)).willReturn(Optional.of(user));
    given(tagRepository.findAllById(List.of(27L, 1L, 13L, 28L, 14L)))
        .willReturn(List.of(activity, interest, secondInterest, travelStyle, secondTravelStyle));

    // when
    ProfileEditResponse response = userProfileEditService.updateProfile(userId, command);

    // then
    assertThat(user.getNickname()).isEqualTo("새닉네임");
    assertThat(user.getGender()).isEqualTo(Gender.MALE);
    assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(2001, 2, 3));
    assertThat(user.getIntroduction()).isEqualTo("새로운 자기소개");
    assertThat(response.orderedTags())
        .extracting(OrderedTagResponse::id)
        .containsExactly(27L, 1L, 13L, 28L, 14L);
    verify(userTagRepository).deleteAllByUserId(userId);
    verify(userTagRepository).flush();
    verify(userTagRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.anyList());
  }

  @DisplayName("필수 카테고리의 태그가 없으면 프로필을 수정하지 않는다")
  @Test
  void updateProfile_withoutActivityTag_rejectsBeforeMutation() {
    // given
    Long userId = 1L;
    User user = createUser(userId);
    Tag interest = createTag(13L, "자연", TagType.INTEREST);
    Tag secondInterest = createTag(14L, "도시", TagType.INTEREST);
    Tag travelStyle = createTag(27L, "계획형", TagType.TRAVEL_STYLE);
    UpdateProfileCommand command = new UpdateProfileCommand(
        "새닉네임",
        Gender.MALE,
        LocalDate.of(2001, 2, 3),
        null,
        List.of(13L, 14L, 27L)
    );

    given(userRepository.findByIdForProfileUpdate(userId)).willReturn(Optional.of(user));
    given(tagRepository.findAllById(List.of(13L, 14L, 27L)))
        .willReturn(List.of(interest, secondInterest, travelStyle));

    // when & then
    assertThatThrownBy(() -> userProfileEditService.updateProfile(userId, command))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(UserErrorCode.INVALID_TAG_SELECTION_COUNT));
    assertThat(user.getNickname()).isEqualTo("기존닉네임");
    verify(userRepository, never()).flush();
    verify(userTagRepository, never()).deleteAllByUserId(userId);
  }

  @DisplayName("orderedTagIds에 중복 태그가 있으면 프로필을 수정하지 않는다")
  @Test
  void updateProfile_withDuplicateTagIds_rejectsBeforeMutation() {
    // given
    Long userId = 1L;
    User user = createUser(userId);
    UpdateProfileCommand command = new UpdateProfileCommand(
        "새닉네임",
        Gender.MALE,
        LocalDate.of(2001, 2, 3),
        null,
        List.of(1L, 13L, 27L, 27L)
    );

    given(userRepository.findByIdForProfileUpdate(userId)).willReturn(Optional.of(user));

    // when & then
    assertThatThrownBy(() -> userProfileEditService.updateProfile(userId, command))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(UserErrorCode.DUPLICATE_TAG));
    assertThat(user.getNickname()).isEqualTo("기존닉네임");
    verify(userRepository, never()).flush();
    verify(userTagRepository, never()).deleteAllByUserId(userId);
  }

  @DisplayName("현재 사용자의 닉네임은 사용 가능한 것으로 판단한다")
  @Test
  void isNicknameAvailable_excludesCurrentUser() {
    // given
    Long userId = 1L;
    given(userRepository.findByIdAndDeletedAtIsNull(userId))
        .willReturn(Optional.of(createUser(userId)));
    given(userRepository.existsByNicknameAndIdNot("기존닉네임", userId)).willReturn(false);

    // when
    boolean available = userProfileEditService.isNicknameAvailable(userId, "기존닉네임");

    // then
    assertThat(available).isTrue();
  }

  private User createUser(Long id) {
    return User.builder()
        .id(id)
        .provider(AuthProvider.KAKAO)
        .providerId("provider-id")
        .email("test@example.com")
        .nickname("기존닉네임")
        .gender(Gender.FEMALE)
        .birthDate(LocalDate.of(2000, 1, 1))
        .introduction("기존 자기소개")
        .build();
  }

  private Tag createTag(Long id, String name, TagType tagType) {
    Tag tag = mock(Tag.class);
    given(tag.getId()).willReturn(id);
    lenient().when(tag.getName()).thenReturn(name);
    lenient().when(tag.getTagType()).thenReturn(tagType);
    return tag;
  }
}
