package org.sopt.buddys.domain.recommendation.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RecommendationErrorCode implements ErrorCode {

  INTEREST_COUNTRY_NOT_SET("REC-E001", HttpStatus.BAD_REQUEST, "관심 국가를 설정해주세요.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}