package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
import org.sopt.buddys.domain.course.service.CourseBookmarkTransactionService;
import org.sopt.buddys.domain.magazine.repository.MagazineBookmarkRepository;
import org.sopt.buddys.domain.magazine.repository.MagazineRepository;
import org.sopt.buddys.domain.magazine.service.MagazineService;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.repository.PostBookmarkRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.service.PostService;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class WithdrawalBookmarkGuardTest {

  @Mock private UserRepository userRepository;
  @Mock private PostRepository postRepository;
  @Mock private PostBookmarkRepository postBookmarkRepository;
  @Mock private MagazineRepository magazineRepository;
  @Mock private MagazineBookmarkRepository magazineBookmarkRepository;
  @Mock private CourseBookmarkRepository courseBookmarkRepository;
  @InjectMocks private PostService postService;
  @InjectMocks private MagazineService magazineService;
  @InjectMocks private CourseBookmarkTransactionService courseBookmarkTransactionService;

  @DisplayName("탈퇴했거나 존재하지 않는 사용자는 게시글을 저장할 수 없다")
  @Test
  void postBookmark_inactiveUser_rejectsInsert() {
    given(postRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(Optional.of(mock(Post.class)));

    assertUserNotFound(() -> postService.bookmarkPost(1L, 2L));

    verifyNoInteractions(postBookmarkRepository);
  }

  @DisplayName("탈퇴했거나 존재하지 않는 사용자는 매거진을 저장할 수 없다")
  @Test
  void magazineBookmark_inactiveUser_rejectsInsert() {
    given(magazineRepository.existsById(2L)).willReturn(true);

    assertUserNotFound(() -> magazineService.bookmarkMagazine(1L, 2L));

    verifyNoInteractions(magazineBookmarkRepository);
  }

  @DisplayName("사용자 조회 후 탈퇴가 완료되면 코스 저장을 거부한다")
  @Test
  void courseBookmark_userWithdrawnAfterOuterRead_rejectsInsert() {
    CourseBookmark bookmark = courseBookmark();

    assertUserNotFound(() -> courseBookmarkTransactionService.create(bookmark));

    verifyNoInteractions(courseBookmarkRepository);
  }

  @DisplayName("게시글 저장 전에 활성 사용자 잠금을 획득한다")
  @Test
  void postBookmark_locksActiveUserBeforeInsert() {
    given(postRepository.findByIdAndDeletedAtIsNull(2L)).willReturn(Optional.of(mock(Post.class)));
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user()));

    postService.bookmarkPost(1L, 2L);

    var order = inOrder(userRepository, postBookmarkRepository);
    order.verify(userRepository).findActiveByIdForUpdate(1L);
    order.verify(postBookmarkRepository).insertOrKeep(1L, 2L);
  }

  @DisplayName("매거진 저장 전에 활성 사용자 잠금을 획득한다")
  @Test
  void magazineBookmark_locksActiveUserBeforeInsert() {
    given(magazineRepository.existsById(2L)).willReturn(true);
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user()));

    magazineService.bookmarkMagazine(1L, 2L);

    var order = inOrder(userRepository, magazineBookmarkRepository);
    order.verify(userRepository).findActiveByIdForUpdate(1L);
    order.verify(magazineBookmarkRepository).insertOrKeep(1L, 2L);
  }

  @DisplayName("코스 저장 트랜잭션 안에서 활성 사용자 잠금을 획득한다")
  @Test
  void courseBookmark_locksActiveUserInInsertTransaction() {
    CourseBookmark bookmark = courseBookmark();
    given(userRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(user()));

    courseBookmarkTransactionService.create(bookmark);

    var order = inOrder(userRepository, courseBookmarkRepository);
    order.verify(userRepository).findActiveByIdForUpdate(1L);
    order.verify(courseBookmarkRepository).saveAndFlush(bookmark);
  }

  private User user() {
    return User.builder().id(1L).nickname("버디").build();
  }

  private CourseBookmark courseBookmark() {
    Course course = mock(Course.class);
    given(course.getId()).willReturn(2L);
    return new CourseBookmark(user(), course);
  }

  private void assertUserNotFound(Runnable action) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(UserErrorCode.USER_NOT_FOUND);
  }
}
