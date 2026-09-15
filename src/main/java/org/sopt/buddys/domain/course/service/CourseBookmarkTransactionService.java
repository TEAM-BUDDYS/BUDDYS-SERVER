package org.sopt.buddys.domain.course.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseBookmarkTransactionService {

  private final CourseBookmarkRepository courseBookmarkRepository;
  private final UserRepository userRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CourseBookmark create(CourseBookmark courseBookmark) {
    userRepository.findActiveByIdForUpdate(courseBookmark.getUser().getId())
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    return courseBookmarkRepository.saveAndFlush(courseBookmark);
  }
}
