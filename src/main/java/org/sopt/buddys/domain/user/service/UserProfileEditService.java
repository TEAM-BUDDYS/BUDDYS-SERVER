package org.sopt.buddys.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.dto.response.ProfileEditResponse;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileEditService {

  private final UserRepository userRepository;
  private final UserTagRepository userTagRepository;

  public ProfileEditResponse getProfile(Long userId) {
    User user = getActiveUser(userId);
    return ProfileEditResponse.of(user, userTagRepository.findAllWithTagByUserId(userId));
  }

  public boolean isNicknameAvailable(Long userId, String nickname) {
    getActiveUser(userId);
    return !userRepository.existsByNicknameAndIdNot(nickname, userId);
  }

  private User getActiveUser(Long userId) {
    return userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
  }
}
