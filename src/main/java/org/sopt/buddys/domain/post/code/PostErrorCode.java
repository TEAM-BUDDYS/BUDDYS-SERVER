package org.sopt.buddys.domain.post.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

  CITY_NOT_FOUND("POST-E001", HttpStatus.NOT_FOUND, "도시를 찾을 수 없습니다."),
  CITY_NOT_IN_COUNTRY("POST-E002", HttpStatus.BAD_REQUEST, "해당 국가에 속한 도시가 아닙니다."),
  TAG_NOT_FOUND("POST-E003", HttpStatus.NOT_FOUND, "태그를 찾을 수 없습니다."),
  ACTIVITY_TAG_REQUIRED("POST-E004", HttpStatus.BAD_REQUEST, "활동 태그를 하나 이상 선택해야 합니다."),
  TAG_LIMIT_EXCEEDED("POST-E005", HttpStatus.BAD_REQUEST, "태그 선택 개수를 초과했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
