package org.sopt.buddys.domain.user.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    List<Long> allTagIds = Stream.of(
                    command.activityTagIds(),
                    command.interestTagIds(),
                    command.travelStyleTagIds()
            )
            .flatMap(List::stream)
            .toList();
    Map<Long, Tag> tagsById = getAndValidateTags(command, allTagIds);

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
    List<UserTag> updatedUserTags = allTagIds.stream()
            .map(tagId -> new UserTag(user, tagsById.get(tagId)))
            .toList();
    userTagRepository.saveAllAndFlush(updatedUserTags);

    return ProfileEditResponse.of(user, updatedUserTags);
  }

  private User getActiveUser(Long userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
  }

  private Map<Long, Tag> getAndValidateTags(UpdateProfileCommand command, List<Long> allTagIds) {
    if (new HashSet<>(allTagIds).size() != allTagIds.size()) {
      throw new BaseException(UserErrorCode.INVALID_TAG);
    }

    Map<Long, Tag> tagsById = tagRepository.findAllById(allTagIds).stream()
            .collect(Collectors.toMap(Tag::getId, Function.identity()));
    if (tagsById.size() != allTagIds.size()) {
      throw new BaseException(UserErrorCode.TAG_NOT_FOUND);
    }

    validateTagType(tagsById, command.activityTagIds(), TagType.ACTIVITY);
    validateTagType(tagsById, command.interestTagIds(), TagType.INTEREST);
    validateTagType(tagsById, command.travelStyleTagIds(), TagType.TRAVEL_STYLE);
    return tagsById;
  }

  private void validateTagType(Map<Long, Tag> tagsById, List<Long> tagIds, TagType expectedType) {
    boolean allMatch = tagIds.stream()
            .map(tagsById::get)
            .allMatch(tag -> tag.getTagType() == expectedType);
    if (!allMatch) {
      throw new BaseException(UserErrorCode.INVALID_TAG);
    }
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
