package org.sopt.buddys.domain.user.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.dto.response.ProfileEditResponse;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.UserTag;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.service.command.UpdateProfileCommand;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileEditService {

  private static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_user_nickname";
  private static final int MIN_ACTIVITY_TAG_COUNT = 1;
  private static final int MAX_ACTIVITY_TAG_COUNT = 3;
  private static final int MIN_INTEREST_TAG_COUNT = 1;
  private static final int MAX_INTEREST_TAG_COUNT = 3;
  private static final int MIN_TRAVEL_STYLE_TAG_COUNT = 1;
  private static final int MAX_TRAVEL_STYLE_TAG_COUNT = 5;

  private final UserRepository userRepository;
  private final UserTagRepository userTagRepository;
  private final TagRepository tagRepository;

  public ProfileEditResponse getProfile(Long userId) {
    User user = getActiveUser(userId);
    return ProfileEditResponse.of(user, userTagRepository.findAllWithTagByUserId(userId));
  }

  public boolean isNicknameAvailable(Long userId, String nickname) {
    getActiveUser(userId);
    return !userRepository.existsByNicknameAndIdNot(nickname, userId);
  }

  @Transactional
  public ProfileEditResponse updateProfile(Long userId, UpdateProfileCommand command) {
    User user = getActiveUser(userId);
    List<Long> orderedTagIds = command.orderedTagIds();
    Map<Long, Tag> tagsById = getAndValidateTags(orderedTagIds);

    try {
      user.updateProfile(command.nickname(), command.gender(), command.birthDate(), command.bio());
      userRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      if (!isNicknameConflict(exception)) {
        throw exception;
      }
      throw new BaseException(AuthErrorCode.DUPLICATE_NICKNAME, exception);
    }

    userTagRepository.deleteAllByUserId(userId);
    userTagRepository.flush();
    List<UserTag> updatedUserTags = IntStream.range(0, orderedTagIds.size())
        .mapToObj(order -> new UserTag(user, tagsById.get(orderedTagIds.get(order)), order))
        .toList();
    userTagRepository.saveAllAndFlush(updatedUserTags);

    return ProfileEditResponse.of(user, updatedUserTags);
  }

  private User getActiveUser(Long userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
  }

  private Map<Long, Tag> getAndValidateTags(List<Long> orderedTagIds) {
    if (new HashSet<>(orderedTagIds).size() != orderedTagIds.size()) {
      throw new BaseException(UserErrorCode.INVALID_TAG);
    }

    Map<Long, Tag> tagsById = tagRepository.findAllById(orderedTagIds).stream()
        .collect(Collectors.toMap(Tag::getId, Function.identity()));
    if (tagsById.size() != orderedTagIds.size()) {
      throw new BaseException(UserErrorCode.TAG_NOT_FOUND);
    }

    validateTagCounts(tagsById);
    return tagsById;
  }

  private void validateTagCounts(Map<Long, Tag> tagsById) {
    Map<TagType, Long> tagCounts = tagsById.values().stream()
        .collect(Collectors.groupingBy(Tag::getTagType, Collectors.counting()));
    if (!isBetween(tagCounts.getOrDefault(TagType.ACTIVITY, 0L),
        MIN_ACTIVITY_TAG_COUNT, MAX_ACTIVITY_TAG_COUNT)
        || !isBetween(tagCounts.getOrDefault(TagType.INTEREST, 0L),
        MIN_INTEREST_TAG_COUNT, MAX_INTEREST_TAG_COUNT)
        || !isBetween(tagCounts.getOrDefault(TagType.TRAVEL_STYLE, 0L),
        MIN_TRAVEL_STYLE_TAG_COUNT, MAX_TRAVEL_STYLE_TAG_COUNT)) {
      throw new BaseException(UserErrorCode.INVALID_TAG_SELECTION_COUNT);
    }
  }

  private boolean isBetween(long value, int minimum, int maximum) {
    return value >= minimum && value <= maximum;
  }

  private boolean isNicknameConflict(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException constraintViolationException) {
        String constraintName = constraintViolationException.getConstraintName();
        return constraintName != null
            && (constraintName.equals(NICKNAME_UNIQUE_CONSTRAINT)
                || constraintName.endsWith("." + NICKNAME_UNIQUE_CONSTRAINT));
      }
      cause = cause.getCause();
    }
    return false;
  }
}
