package org.sopt.buddys.domain.course.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import java.sql.SQLException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
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
  private CourseBookmarkTransactionService courseBookmarkTransactionService;

  @DisplayName("북마크 저장이 PRIMARY 키 중복 위반이면 이미 저장된 상태이므로 예외 없이 무시된다")
  @Test
  void bookmarkCourse_duplicateBookmarkRace_isIgnored() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    User user = mockUser(userId);
    Course course = mockCourse(courseId);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(courseRepository.findByIdAndDeletedAtIsNull(courseId)).willReturn(Optional.of(course));
    ConstraintViolationException primaryKeyViolation = new ConstraintViolationException(
        "duplicate", new SQLException("duplicate"), "PRIMARY"
    );
    willThrow(new DataIntegrityViolationException("duplicate", primaryKeyViolation))
        .given(courseBookmarkTransactionService).create(any(CourseBookmark.class));

    // when, then
    assertThatCode(() -> courseService.bookmarkCourse(userId, courseId)).doesNotThrowAnyException();
  }

  @DisplayName("북마크 저장이 PRIMARY 키 위반이 아닌 다른 무결성 위반(예: FK 위반)이면 예외가 그대로 전파된다")
  @Test
  void bookmarkCourse_nonDuplicateIntegrityViolation_propagatesException() {
    // given
    Long userId = 1L;
    Long courseId = 2L;
    User user = mockUser(userId);
    Course course = mockCourse(courseId);

    given(userRepository.findByIdAndDeletedAtIsNull(userId)).willReturn(Optional.of(user));
    given(courseRepository.findByIdAndDeletedAtIsNull(courseId)).willReturn(Optional.of(course));
    ConstraintViolationException fkViolation = new ConstraintViolationException(
        "fk violation", new SQLException("fk violation"), "fk_course_bookmark_course"
    );
    willThrow(new DataIntegrityViolationException("fk violation", fkViolation))
        .given(courseBookmarkTransactionService).create(any(CourseBookmark.class));

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
