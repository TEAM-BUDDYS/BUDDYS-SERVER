package org.sopt.buddys.domain.user.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.UserTag;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.service.command.OnboardingCommand;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserOnboardingService {

  private static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_user_nickname";
  private static final String USER_TAG_PRIMARY_KEY_CONSTRAINT = "PRIMARY";

  private final UserRepository userRepository;
  private final UserTagRepository userTagRepository;
  private final CountryRepository countryRepository;
  private final CityRepository cityRepository;
  private final TagRepository tagRepository;

  @Transactional
  public User completeOnboarding(Long userId, OnboardingCommand request) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    validateNotAlreadyOnboarded(userId);
    City interestCity = validateAndGetCity(request.interestCountryId(), request.interestCityId());
    Country interestCountry = interestCity.getCountry();

    validateExchangeFieldsConsistency(request);
    Country exchangeCountry = null;
    if (request.exchangeCountryId() != null) {
      exchangeCountry = countryRepository.findById(request.exchangeCountryId())
          .orElseThrow(() -> new BaseException(LocationErrorCode.COUNTRY_NOT_FOUND));
    }
    validateExchangeDates(request.exchangeStartDate(), request.exchangeEndDate());

    List<Long> allTagIds = Stream.of(request.activityTagIds(), request.interestTagIds(), request.travelStyleTagIds())
        .flatMap(List::stream)
        .toList();
    Map<Long, Tag> tagsById = tagRepository.findAllById(allTagIds).stream()
        .collect(Collectors.toMap(Tag::getId, Function.identity()));
    validateTagsExist(tagsById, allTagIds);
    validateTagType(tagsById, request.activityTagIds(), TagType.ACTIVITY);
    validateTagType(tagsById, request.interestTagIds(), TagType.INTEREST);
    validateTagType(tagsById, request.travelStyleTagIds(), TagType.TRAVEL_STYLE);

    try {
      user.completeOnboarding(
          request.nickname(), request.gender(), request.birthDate(),
          request.bio(), request.profileImageUrl(),
          interestCountry, interestCity,
          exchangeCountry, request.exchangeUniversity(),
          request.exchangeStartDate(), request.exchangeEndDate()
      );
      userRepository.flush();
    } catch (DataIntegrityViolationException e) {
      if (!isNicknameConflict(e)) {
        throw e;
      }
      throw new BaseException(AuthErrorCode.DUPLICATE_NICKNAME, e);
    }

    List<Long> orderedTagIds = interleaveByCategory(
        request.activityTagIds(), request.interestTagIds(), request.travelStyleTagIds());
    List<UserTag> userTags = IntStream.range(0, orderedTagIds.size())
        .mapToObj(order -> new UserTag(user, tagsById.get(orderedTagIds.get(order)), order))
        .toList();
    try {
      userTagRepository.saveAll(userTags);
      userTagRepository.flush();
    } catch (DataIntegrityViolationException e) {
      if (!isConstraintViolation(e, USER_TAG_PRIMARY_KEY_CONSTRAINT)) {
        throw e;
      }
      throw new BaseException(UserErrorCode.ONBOARDING_ALREADY_COMPLETED, e);
    }

    return user;
  }

  /**
   * 온보딩에는 드래그앤드롭 정렬이 없으므로, 카테고리를 번갈아 배치해 상위 3개(=대표 태그)가
   * 카테고리별 1개씩 노출되도록 순서를 만든다.
   */
  @SafeVarargs
  private List<Long> interleaveByCategory(List<Long>... tagIdGroups) {
    List<Long> ordered = new ArrayList<>();
    int maxSize = Stream.of(tagIdGroups).mapToInt(List::size).max().orElse(0);
    for (int index = 0; index < maxSize; index++) {
      for (List<Long> group : tagIdGroups) {
        if (index < group.size()) {
          ordered.add(group.get(index));
        }
      }
    }
    return ordered;
  }

  private void validateNotAlreadyOnboarded(Long userId) {
    if (userTagRepository.existsByUserId(userId)) {
      throw new BaseException(UserErrorCode.ONBOARDING_ALREADY_COMPLETED);
    }
  }

  private City validateAndGetCity(Long countryId, Long cityId) {
    City city = cityRepository.findById(cityId)
        .orElseThrow(() -> new BaseException(LocationErrorCode.CITY_NOT_FOUND));
    if (!city.getCountry().getId().equals(countryId)) {
      throw new BaseException(UserErrorCode.INTEREST_CITY_COUNTRY_MISMATCH);
    }
    return city;
  }

  private void validateExchangeDates(YearMonth start, YearMonth end) {
    if (start != null && end != null && start.isAfter(end)) {
      throw new BaseException(UserErrorCode.INVALID_EXCHANGE_PERIOD);
    }
  }

  private void validateExchangeFieldsConsistency(OnboardingCommand request) {
    boolean anyPresent = request.exchangeCountryId() != null
        || request.exchangeUniversity() != null
        || request.exchangeStartDate() != null
        || request.exchangeEndDate() != null;
    boolean allPresent = request.exchangeCountryId() != null
        && request.exchangeUniversity() != null
        && request.exchangeStartDate() != null
        && request.exchangeEndDate() != null;
    if (anyPresent && !allPresent) {
      throw new BaseException(UserErrorCode.EXCHANGE_INFO_INCOMPLETE);
    }
  }

  private boolean isNicknameConflict(DataIntegrityViolationException exception) {
    return isConstraintViolation(exception, NICKNAME_UNIQUE_CONSTRAINT);
  }

  private boolean isConstraintViolation(DataIntegrityViolationException exception, String constraintName) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolationException) {
        String actualConstraintName = constraintViolationException.getConstraintName();
        return actualConstraintName != null
            && (actualConstraintName.equals(constraintName)
                || actualConstraintName.endsWith("." + constraintName));
      }
      cause = cause.getCause();
    }
    return false;
  }

  private void validateTagsExist(Map<Long, Tag> tagsById, List<Long> allTagIds) {
    if (tagsById.size() != allTagIds.size()) {
      throw new BaseException(UserErrorCode.TAG_NOT_FOUND);
    }
  }

  private void validateTagType(Map<Long, Tag> tagsById, List<Long> tagIds, TagType expectedType) {
    boolean allMatch = tagIds.stream()
        .map(tagsById::get)
        .allMatch(tag -> tag.getTagType() == expectedType);
    if (!allMatch) {
      throw new BaseException(UserErrorCode.INVALID_TAG);
    }
  }
}
