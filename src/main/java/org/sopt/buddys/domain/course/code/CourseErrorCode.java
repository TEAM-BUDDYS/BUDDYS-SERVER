package org.sopt.buddys.domain.course.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CourseErrorCode implements ErrorCode {

  CITY_NOT_IN_COUNTRY("COURSE-E001", HttpStatus.BAD_REQUEST, "해당 국가에 속한 도시가 아닙니다."),
  TAG_NOT_FOUND("COURSE-E002", HttpStatus.NOT_FOUND, "태그를 찾을 수 없습니다."),
  COMPANION_USER_NOT_FOUND("COURSE-E003", HttpStatus.NOT_FOUND, "함께한 유저를 찾을 수 없습니다."),
  DAY_NUMBER_DUPLICATED("COURSE-E004", HttpStatus.BAD_REQUEST, "일자(dayNumber)가 중복되었습니다."),
  COURSE_NOT_FOUND("COURSE-E005", HttpStatus.NOT_FOUND, "코스를 찾을 수 없습니다."),
  ACTIVITY_TAG_REQUIRED("COURSE-E006", HttpStatus.BAD_REQUEST, "활동 태그를 하나 이상 선택해야 합니다."),
  TAG_LIMIT_EXCEEDED("COURSE-E007", HttpStatus.BAD_REQUEST, "태그 선택 개수를 초과했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
