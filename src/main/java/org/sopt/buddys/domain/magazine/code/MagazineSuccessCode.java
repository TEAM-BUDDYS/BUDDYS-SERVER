package org.sopt.buddys.domain.magazine.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MagazineSuccessCode implements SuccessCode {

  MAGAZINE_LIST_FOUND("MAGAZINE-S001", HttpStatus.OK, "매거진 목록 조회에 성공했습니다."),
  MAGAZINE_BOOKMARKED("MAGAZINE-S002", HttpStatus.OK, "매거진 저장에 성공했습니다."),
  MAGAZINE_BOOKMARK_REMOVED("MAGAZINE-S003", HttpStatus.OK, "매거진 저장 취소에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
