package org.sopt.buddys.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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

  @DisplayName("댓글 저장과 코스 조회 사이에 코스가 삭제되면(commentCount 증가 0건) 예외가 발생한다")
  @Test
  void createComment_courseDeletedBetweenSaveAndCountIncrease_throwsCourseNotFound() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    User author = mock(User.class);
    Course course = mock(Course.class);
    CourseComment savedComment = mock(CourseComment.class);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(author));
    given(courseRepository.findByIdAndDeletedAtIsNull(courseId)).willReturn(Optional.of(course));
    given(courseCommentRepository.save(any(CourseComment.class))).willReturn(savedComment);
    given(courseRepository.increaseCommentCount(courseId)).willReturn(0);

    // when, then
    assertThatThrownBy(() -> courseCommentService.createComment(userId, courseId, "댓글"))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
  }

  @DisplayName("commentCount 증가가 정상적으로 반영되면 댓글이 그대로 반환된다")
  @Test
  void createComment_countIncreased_returnsSavedComment() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    User author = mock(User.class);
    Course course = mock(Course.class);
    CourseComment savedComment = mock(CourseComment.class);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(author));
    given(courseRepository.findByIdAndDeletedAtIsNull(courseId)).willReturn(Optional.of(course));
    given(courseCommentRepository.save(any(CourseComment.class))).willReturn(savedComment);
    given(courseRepository.increaseCommentCount(courseId)).willReturn(1);

    // when
    CourseComment result = courseCommentService.createComment(userId, courseId, "댓글");

    // then
    assertThat(result).isEqualTo(savedComment);
  }
}
