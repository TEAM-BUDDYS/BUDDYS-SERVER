package org.sopt.buddys.domain.place.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.SuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceSuccessCode implements SuccessCode {

  PLACE_BOOKMARKED("PLACE-S001", HttpStatus.CREATED, "장소 저장에 성공했습니다."),
  PLACE_BOOKMARK_CANCELED("PLACE-S002", HttpStatus.OK, "장소 저장 취소에 성공했습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
