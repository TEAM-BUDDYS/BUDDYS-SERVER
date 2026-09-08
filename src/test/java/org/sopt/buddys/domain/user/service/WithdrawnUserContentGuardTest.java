package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.course.service.CourseService;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.service.PostService;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class WithdrawnUserContentGuardTest {

  @Mock private UserRepository userRepository;
  @Mock private PostRepository postRepository;
  @Mock private CourseRepository courseRepository;
  @InjectMocks private PostService postService;
  @InjectMocks private CourseService courseService;

  @DisplayName("탈퇴했거나 존재하지 않는 사용자는 게시글을 수정할 수 없다")
  @Test
  void updatePost_inactiveUser_rejectedBeforeContentAccess() {
    assertRejected(() -> postService.updatePost(1L, 2L, null));
  }

  @DisplayName("탈퇴했거나 존재하지 않는 사용자는 모집 상태를 변경할 수 없다")
  @Test
  void updatePostStatus_inactiveUser_rejectedBeforeContentAccess() {
    assertRejected(() -> postService.updatePostStatus(1L, 2L, PostStatus.RECRUITING));
  }

  @DisplayName("탈퇴했거나 존재하지 않는 사용자는 게시글을 삭제할 수 없다")
  @Test
  void deletePost_inactiveUser_rejectedBeforeContentAccess() {
    assertRejected(() -> postService.deletePost(1L, 2L));
  }

  @DisplayName("탈퇴했거나 존재하지 않는 사용자는 코스를 수정할 수 없다")
  @Test
  void updateCourse_inactiveUser_rejectedBeforeContentAccess() {
    assertRejected(() -> courseService.updateCourse(1L, 2L, null));
  }

  @DisplayName("탈퇴했거나 존재하지 않는 사용자는 코스를 삭제할 수 없다")
  @Test
  void deleteCourse_inactiveUser_rejectedBeforeContentAccess() {
    assertRejected(() -> courseService.deleteCourse(1L, 2L));
  }

  private void assertRejected(Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    verify(userRepository).findActiveByIdForUpdate(1L);
    verifyNoInteractions(postRepository, courseRepository);
  }
}
