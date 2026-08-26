package org.sopt.buddys.domain.comment.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.comment.entity.CourseComment;
import org.sopt.buddys.domain.comment.repository.CourseCommentRepository;
import org.sopt.buddys.domain.comment.service.result.CourseCommentListResult;
import org.sopt.buddys.domain.comment.service.result.CourseCommentListResult.CourseCommentResult;
import org.sopt.buddys.domain.course.code.CourseErrorCode;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.TimeAgoFormatter;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.sopt.buddys.global.common.PageConstants.MAX_PAGE_SIZE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseCommentService {

  private final CourseCommentRepository courseCommentRepository;
  private final CourseRepository courseRepository;
  private final UserRepository userRepository;

  public CourseCommentListResult getComments(Long courseId, int page, int size) {
    validatePageRequest(page, size);
    if (!courseRepository.existsByIdAndDeletedAtIsNull(courseId)) {
      throw new BaseException(CourseErrorCode.COURSE_NOT_FOUND);
    }

    LocalDateTime now = LocalDateTime.now();
    Slice<CourseComment> commentSlice = courseCommentRepository.findAllByCourseIdWithAuthorOrderByCreatedAtAsc(
        courseId,
        PageRequest.of(page, size)
    );
    List<CourseCommentResult> comments = commentSlice.getContent()
        .stream()
        .map(comment -> new CourseCommentResult(
            comment.getId(),
            comment.getAuthor().getId(),
            comment.getAuthor().getNickname(),
            comment.getAuthor().getProfileImageUrl(),
            comment.getContent(),
            comment.getCreatedAt(),
            TimeAgoFormatter.format(comment.getCreatedAt(), now)
        ))
        .toList();

    return new CourseCommentListResult(
        comments,
        commentSlice.getNumber(),
        commentSlice.getSize(),
        commentSlice.hasNext()
    );
  }

  @Transactional
  public CourseComment createComment(
      Long userId,
      Long courseId,
      String content
  ) {
    User author = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new BaseException(CourseErrorCode.COURSE_NOT_FOUND));

    CourseComment comment = courseCommentRepository.save(new CourseComment(
        course,
        author,
        content.trim()
    ));
    courseRepository.increaseCommentCount(courseId);
    return comment;
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}
