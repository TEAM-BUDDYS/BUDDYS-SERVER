package org.sopt.buddys.global.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalSuccessCode implements SuccessCode {

  OK("GLB-S001", HttpStatus.OK, "요청이 성공했습니다."),
  CREATED("GLB-S002", HttpStatus.CREATED, "리소스가 생성되었습니다."),
  NO_CONTENT("GLB-S003", HttpStatus.NO_CONTENT, "요청이 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}