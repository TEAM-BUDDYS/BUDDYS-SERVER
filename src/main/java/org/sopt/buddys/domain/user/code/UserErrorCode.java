package org.sopt.buddys.domain.user.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND("USER-E001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  INVALID_EXCHANGE_PERIOD("USER-E002", HttpStatus.BAD_REQUEST, "교환학생 시작일은 종료일보다 이전이어야 합니다."),
  INTEREST_CITY_COUNTRY_MISMATCH("USER-E003", HttpStatus.BAD_REQUEST, "관심 도시가 관심 국가에 속하지 않습니다."),
  INVALID_TAG("USER-E004", HttpStatus.BAD_REQUEST, "태그 카테고리가 일치하지 않습니다."),
  ONBOARDING_ALREADY_COMPLETED("USER-E005", HttpStatus.BAD_REQUEST, "이미 온보딩을 완료한 사용자입니다."),
  TAG_NOT_FOUND("USER-E006", HttpStatus.BAD_REQUEST, "존재하지 않는 태그가 포함되어 있습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
