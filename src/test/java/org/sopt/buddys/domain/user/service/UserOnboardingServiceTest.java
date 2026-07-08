package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.Gender;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.service.command.OnboardingCommand;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
public class UserOnboardingServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long COUNTRY_ID = 31L;
  private static final Long CITY_ID = 20692L;

  @InjectMocks
  private UserOnboardingService userOnboardingService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserTagRepository userTagRepository;

  @Mock
  private CountryRepository countryRepository;

  @Mock
  private CityRepository cityRepository;

  @Mock
  private TagRepository tagRepository;

  @DisplayName("정상적인 온보딩 요청이면 유저 정보와 태그를 저장한다")
  @Test
  void completeOnboarding_validRequest_savesUserAndTags() {
    // given
    User user = createUser();
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    Tag activityTag = mockTag(1L, TagType.ACTIVITY);
    Tag interestTag = mockTag(2L, TagType.INTEREST);
    Tag travelStyleTag = mockTag(3L, TagType.TRAVEL_STYLE);
    OnboardingCommand request = createValidRequest();

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(tagRepository.findAllById(any())).willReturn(List.of(activityTag, interestTag, travelStyleTag));

    // when
    User result = userOnboardingService.completeOnboarding(USER_ID, request);

    // then
    assertThat(result.getNickname()).isEqualTo("해령");
    assertThat(result.getGender()).isEqualTo(Gender.FEMALE);
    assertThat(result.getInterestCountry()).isEqualTo(interestCountry);
    assertThat(result.getInterestCity()).isEqualTo(interestCity);
    then(userRepository).should(times(1)).flush();
    then(userTagRepository).should(times(1)).saveAll(any());
  }

  @DisplayName("존재하지 않는 사용자면 USER_NOT_FOUND 예외가 발생한다")
  @Test
  void completeOnboarding_userNotFound_throwsException() {
    // given
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)
        );
    then(userTagRepository).should(never()).saveAll(any());
  }

  @DisplayName("이미 온보딩을 완료한 사용자면 ONBOARDING_ALREADY_COMPLETED 예외가 발생한다")
  @Test
  void completeOnboarding_alreadyOnboarded_throwsException() {
    // given
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(true);

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.ONBOARDING_ALREADY_COMPLETED)
        );
  }

  @DisplayName("관심 도시가 존재하지 않으면 LOC-E002 예외가 발생한다")
  @Test
  void completeOnboarding_interestCityNotFound_throwsException() {
    // given
    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(LocationErrorCode.CITY_NOT_FOUND)
        );
  }

  @DisplayName("관심 도시가 관심 국가에 속하지 않으면 예외가 발생한다")
  @Test
  void completeOnboarding_interestCityCountryMismatch_throwsException() {
    // given
    Country otherCountry = mockCountry(999L);
    City interestCity = mockCity(otherCountry);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INTEREST_CITY_COUNTRY_MISMATCH)
        );
  }

  @DisplayName("교환학생 국가가 존재하지 않으면 예외가 발생한다")
  @Test
  void completeOnboarding_exchangeCountryNotFound_throwsException() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    Long exchangeCountryId = 999L;

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(countryRepository.findById(exchangeCountryId)).willReturn(Optional.empty());

    OnboardingCommand request = createRequestWithExchangeCountry(
        exchangeCountryId,
        LocalDate.of(2027, 3, 1),
        LocalDate.of(2027, 8, 31)
    );

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, request))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(LocationErrorCode.COUNTRY_NOT_FOUND)
        );
  }

  @DisplayName("교환학생 시작일이 종료일보다 늦으면 예외가 발생한다")
  @Test
  void completeOnboarding_invalidExchangePeriod_throwsException() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    Long exchangeCountryId = 35L;
    Country exchangeCountry = mock(Country.class);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(countryRepository.findById(exchangeCountryId)).willReturn(Optional.of(exchangeCountry));

    OnboardingCommand request = createRequestWithExchangeCountry(
        exchangeCountryId,
        LocalDate.of(2027, 8, 31),
        LocalDate.of(2027, 3, 1)
    );

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, request))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_EXCHANGE_PERIOD)
        );
  }

  @DisplayName("교환학생 국가 없이 대학/기간만 채워지면 EXCHANGE_INFO_INCOMPLETE 예외가 발생한다")
  @Test
  void completeOnboarding_exchangeInfoIncomplete_throwsException() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));

    OnboardingCommand request = createRequestWithExchangeCountry(
        null,
        LocalDate.of(2027, 3, 1),
        LocalDate.of(2027, 8, 31)
    );

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, request))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.EXCHANGE_INFO_INCOMPLETE)
        );
    then(countryRepository).should(never()).findById(any());
  }

  @DisplayName("존재하지 않는 태그 ID가 포함되면 TAG_NOT_FOUND 예외가 발생한다")
  @Test
  void completeOnboarding_tagNotFound_throwsException() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    Tag activityTag = mockTag(1L, TagType.ACTIVITY);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(tagRepository.findAllById(any())).willReturn(List.of(activityTag));

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.TAG_NOT_FOUND)
        );
  }

  @DisplayName("태그 카테고리가 일치하지 않으면 INVALID_TAG 예외가 발생한다")
  @Test
  void completeOnboarding_tagTypeMismatch_throwsException() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    // activityTagIds(1L)에 해당하는 태그가 실제로는 INTEREST 타입
    Tag wrongTypeTag = mockTag(1L, TagType.INTEREST);
    Tag interestTag = mockTag(2L, TagType.INTEREST);
    Tag travelStyleTag = mockTag(3L, TagType.TRAVEL_STYLE);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(tagRepository.findAllById(any())).willReturn(List.of(wrongTypeTag, interestTag, travelStyleTag));

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.INVALID_TAG)
        );
  }

  @DisplayName("닉네임이 중복되면 DUPLICATE_NICKNAME 예외로 변환한다")
  @Test
  void completeOnboarding_duplicateNickname_throwsException() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    Tag activityTag = mockTag(1L, TagType.ACTIVITY);
    Tag interestTag = mockTag(2L, TagType.INTEREST);
    Tag travelStyleTag = mockTag(3L, TagType.TRAVEL_STYLE);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(tagRepository.findAllById(any())).willReturn(List.of(activityTag, interestTag, travelStyleTag));

    ConstraintViolationException nicknameConstraintViolation = new ConstraintViolationException(
        "duplicate nickname", new SQLException("duplicate"), "uk_user_nickname"
    );
    willThrow(new DataIntegrityViolationException("duplicate", nicknameConstraintViolation))
        .given(userRepository).flush();

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(org.sopt.buddys.domain.auth.code.AuthErrorCode.DUPLICATE_NICKNAME)
        );
    then(userTagRepository).should(never()).saveAll(any());
  }

  @DisplayName("닉네임 충돌이 아닌 다른 제약 위반은 원본 예외를 그대로 전파한다")
  @Test
  void completeOnboarding_otherConstraintViolation_rethrowsOriginalException() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    Tag activityTag = mockTag(1L, TagType.ACTIVITY);
    Tag interestTag = mockTag(2L, TagType.INTEREST);
    Tag travelStyleTag = mockTag(3L, TagType.TRAVEL_STYLE);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(tagRepository.findAllById(any())).willReturn(List.of(activityTag, interestTag, travelStyleTag));

    DataIntegrityViolationException otherViolation =
        new DataIntegrityViolationException("data too long", new RuntimeException("cause"));
    willThrow(otherViolation).given(userRepository).flush();

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isSameAs(otherViolation);
  }

  @DisplayName("동시 요청으로 태그 저장이 충돌하면 ONBOARDING_ALREADY_COMPLETED로 변환한다")
  @Test
  void completeOnboarding_concurrentTagSaveConflict_throwsOnboardingAlreadyCompleted() {
    // given
    Country interestCountry = mockCountry(COUNTRY_ID);
    City interestCity = mockCity(interestCountry);
    Tag activityTag = mockTag(1L, TagType.ACTIVITY);
    Tag interestTag = mockTag(2L, TagType.INTEREST);
    Tag travelStyleTag = mockTag(3L, TagType.TRAVEL_STYLE);

    given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(createUser()));
    given(userTagRepository.existsByUserId(USER_ID)).willReturn(false);
    given(cityRepository.findById(CITY_ID)).willReturn(Optional.of(interestCity));
    given(tagRepository.findAllById(any())).willReturn(List.of(activityTag, interestTag, travelStyleTag));

    willThrow(new DataIntegrityViolationException("duplicate key"))
        .given(userTagRepository).saveAll(any());

    // when, then
    assertThatThrownBy(() -> userOnboardingService.completeOnboarding(USER_ID, createValidRequest()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.ONBOARDING_ALREADY_COMPLETED)
        );
  }

  private User createUser() {
    return User.builder()
        .email("test@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("provider-id")
        .nickname("임시닉네임")
        .build();
  }

  private Country mockCountry(Long id) {
    Country country = mock(Country.class);
    given(country.getId()).willReturn(id);
    return country;
  }

  private City mockCity(Country country) {
    City city = mock(City.class);
    given(city.getCountry()).willReturn(country);
    return city;
  }

  private Tag mockTag(Long id, TagType tagType) {
    Tag tag = mock(Tag.class);
    given(tag.getId()).willReturn(id);
    lenient().when(tag.getTagType()).thenReturn(tagType);
    return tag;
  }

  private OnboardingCommand createValidRequest() {
    return new OnboardingCommand(
        COUNTRY_ID,
        CITY_ID,
        null,
        null,
        null,
        null,
        List.of(1L),
        List.of(2L),
        List.of(3L),
        "해령",
        Gender.FEMALE,
        LocalDate.of(2000, 4, 2),
        "같이 여행 다녀요!",
        "https://example.com/profile.png"
    );
  }

  private OnboardingCommand createRequestWithExchangeCountry(
      Long exchangeCountryId,
      LocalDate exchangeStartDate,
      LocalDate exchangeEndDate
  ) {
    return new OnboardingCommand(
        COUNTRY_ID,
        CITY_ID,
        exchangeCountryId,
        "Technical University of Munich",
        exchangeStartDate,
        exchangeEndDate,
        List.of(1L),
        List.of(2L),
        List.of(3L),
        "해령",
        Gender.FEMALE,
        LocalDate.of(2000, 4, 2),
        "같이 여행 다녀요!",
        "https://example.com/profile.png"
    );
  }
}