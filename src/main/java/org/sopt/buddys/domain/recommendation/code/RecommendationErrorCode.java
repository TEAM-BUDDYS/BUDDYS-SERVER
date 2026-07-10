package org.sopt.buddys.domain.recommendation.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

  INTEREST_COUNTRY_NOT_SET("REC-E001", HttpStatus.BAD_REQUEST, "관심 국가를 설정해주세요."),
  EXCHANGE_COUNTRY_NOT_SET("REC-E002", HttpStatus.BAD_REQUEST, "파견 국가 정보가 설정되어 있지 않습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}