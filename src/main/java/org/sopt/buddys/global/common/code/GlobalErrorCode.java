package org.sopt.buddys.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

  INVALID_REQUEST(
      "GLB-E001",
      HttpStatus.BAD_REQUEST,
      "잘못된 요청입니다."
  ),

  UNAUTHORIZED(
      "GLB-E002",
      HttpStatus.UNAUTHORIZED,
      "인증이 필요합니다."
  ),

  FORBIDDEN(
      "GLB-E003",
      HttpStatus.FORBIDDEN,
      "접근 권한이 없습니다."
  ),

  RESOURCE_NOT_FOUND(
      "GLB-E004",
      HttpStatus.NOT_FOUND,
      "리소스를 찾을 수 없습니다."
  ),

  INTERNAL_SERVER_ERROR(
      "GLB-E005",
      HttpStatus.INTERNAL_SERVER_ERROR,
      "서버 내부 오류가 발생했습니다."
  ),

  METHOD_NOT_ALLOWED(
      "GLB-E006",
      HttpStatus.METHOD_NOT_ALLOWED,
      "지원하지 않는 HTTP 메서드입니다."
  );

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
