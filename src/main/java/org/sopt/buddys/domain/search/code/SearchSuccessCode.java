package org.sopt.buddys.domain.search.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchSuccessCode implements SuccessCode {

  SEARCH_SUCCEEDED("SEARCH-S001", HttpStatus.OK, "검색에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
