package org.sopt.buddys.domain.course.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseSuccessCode implements SuccessCode {

  COURSE_DETAIL_FOUND("COURSE-S001", HttpStatus.OK, "코스 상세 조회에 성공했습니다."),
  COURSE_BOOKMARKED("COURSE-S002", HttpStatus.OK, "코스가 저장되었습니다."),
  COURSE_BOOKMARK_CANCELLED("COURSE-S003", HttpStatus.OK, "코스 저장이 취소되었습니다."),
  COURSE_UPDATED("COURSE-S004", HttpStatus.OK, "코스가 수정되었습니다."),
  COURSE_LIST_FOUND("COURSE-S005", HttpStatus.OK, "코스 목록 조회에 성공했습니다."),
  COURSE_BOOKMARK_LIST_FOUND("COURSE-S006", HttpStatus.OK, "저장한 코스 목록 조회에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
