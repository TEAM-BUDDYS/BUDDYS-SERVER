package org.sopt.buddys.domain.user.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.sopt.buddys.domain.user.dto.request.OnboardingRequest;
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

    List<UserTag> userTags = allTagIds.stream()
        .map(tagId -> new UserTag(user, tagsById.get(tagId)))
        .toList();
    try {
      userTagRepository.saveAll(userTags);
    } catch (DataIntegrityViolationException e) {
      throw new BaseException(UserErrorCode.ONBOARDING_ALREADY_COMPLETED);
    }

    return user;
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

  private void validateExchangeDates(LocalDate start, LocalDate end) {
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
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolationException) {
        return NICKNAME_UNIQUE_CONSTRAINT.equals(constraintViolationException.getConstraintName());
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
