package org.sopt.buddys.domain.course.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseSuccessCode implements SuccessCode {

  COURSE_DETAIL_FOUND("COURSE-S001", HttpStatus.OK, "코스 상세 조회에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
