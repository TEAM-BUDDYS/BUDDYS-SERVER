package org.sopt.buddys.domain.course.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.entity.CourseBookmarkId;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class CourseServiceUnitTest {

  @InjectMocks
  private CourseService courseService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CourseRepository courseRepository;

  @Mock
  private CourseBookmarkRepository courseBookmarkRepository;

  @DisplayName("북마크 저장이 중복 제약 위반이면 이미 저장된 상태이므로 예외 없이 무시된다")
  @Test
  void bookmarkCourse_duplicateBookmarkRace_isIgnored() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    User user = mockUser(userId);
    Course course = mockCourse(courseId);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(courseRepository.findByIdAndDeletedAtIsNull(courseId)).willReturn(Optional.of(course));
    willThrow(new DataIntegrityViolationException("duplicate"))
        .given(courseBookmarkRepository).saveAndFlush(any(CourseBookmark.class));
    given(courseBookmarkRepository.existsById(new CourseBookmarkId(userId, courseId))).willReturn(true);

    // when, then
    assertThatCode(() -> courseService.bookmarkCourse(userId, courseId)).doesNotThrowAnyException();
  }

  @DisplayName("북마크 저장이 중복 제약이 아닌 다른 무결성 위반이면 예외가 그대로 전파된다")
  @Test
  void bookmarkCourse_nonDuplicateIntegrityViolation_propagatesException() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    User user = mockUser(userId);
    Course course = mockCourse(courseId);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(courseRepository.findByIdAndDeletedAtIsNull(courseId)).willReturn(Optional.of(course));
    willThrow(new DataIntegrityViolationException("fk violation"))
        .given(courseBookmarkRepository).saveAndFlush(any(CourseBookmark.class));
    given(courseBookmarkRepository.existsById(new CourseBookmarkId(userId, courseId))).willReturn(false);

    // when, then
    assertThatThrownBy(() -> courseService.bookmarkCourse(userId, courseId))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private User mockUser(Long userId) {
    User user = mock(User.class);
    given(user.getId()).willReturn(userId);
    return user;
  }

  private Course mockCourse(Long courseId) {
    Course course = mock(Course.class);
    given(course.getId()).willReturn(courseId);
    return course;
  }
}
