package org.sopt.buddys.domain.course.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

  TAG_NOT_FOUND("COURSE-E002", HttpStatus.NOT_FOUND, "태그를 찾을 수 없습니다."),
  COMPANION_USER_NOT_FOUND("COURSE-E003", HttpStatus.NOT_FOUND, "함께한 유저를 찾을 수 없습니다."),
  DAY_NUMBER_DUPLICATED("COURSE-E004", HttpStatus.BAD_REQUEST, "일자(dayNumber)가 중복되었습니다."),
  COURSE_NOT_FOUND("COURSE-E005", HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
  ACTIVITY_TAG_REQUIRED("COURSE-E006", HttpStatus.BAD_REQUEST, "활동 태그를 하나 이상 선택해야 합니다."),
  TAG_LIMIT_EXCEEDED("COURSE-E007", HttpStatus.BAD_REQUEST, "태그 선택 개수를 초과했습니다."),
  AUTHOR_CANNOT_BE_COMPANION("COURSE-E008", HttpStatus.BAD_REQUEST, "작성자는 동행자로 추가할 수 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
