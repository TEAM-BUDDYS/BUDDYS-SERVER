package org.sopt.buddys.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.comment.entity.CourseComment;
import org.sopt.buddys.domain.comment.repository.CourseCommentRepository;
import org.sopt.buddys.domain.course.code.CourseErrorCode;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class CourseCommentServiceTest {

  @InjectMocks
  private CourseCommentService courseCommentService;

  @Mock
  private CourseCommentRepository courseCommentRepository;

  @Mock
  private CourseRepository courseRepository;

  @Mock
  private UserRepository userRepository;

  @DisplayName("코스가 이미 삭제됐으면 쓰기 락 조회에서 걸러져 댓글을 저장하지 않고 예외가 발생한다")
  @Test
  void createComment_deletedCourse_throwsCourseNotFound() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(mock(User.class)));
    given(courseRepository.findByIdAndDeletedAtIsNullForUpdate(courseId)).willReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> courseCommentService.createComment(userId, courseId, "댓글"))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
    then(courseCommentRepository).should(never()).save(any());
    then(courseRepository).should(never()).increaseCommentCount(any());
  }

  @DisplayName("쓰기 락으로 코스를 잡은 뒤 댓글을 저장하고 commentCount를 증가시킨다")
  @Test
  void createComment_locksCourseThenSavesAndIncreasesCount() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    User author = mock(User.class);
    Course course = mock(Course.class);
    CourseComment savedComment = mock(CourseComment.class);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(author));
    given(courseRepository.findByIdAndDeletedAtIsNullForUpdate(courseId)).willReturn(Optional.of(course));
    given(courseCommentRepository.save(any(CourseComment.class))).willReturn(savedComment);

    // when
    CourseComment result = courseCommentService.createComment(userId, courseId, "댓글");

    // then
    assertThat(result).isEqualTo(savedComment);
    then(courseRepository).should().increaseCommentCount(courseId);
  }
}
