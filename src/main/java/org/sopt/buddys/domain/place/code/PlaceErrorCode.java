package org.sopt.buddys.domain.place.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
  GOOGLE_PLACES_UNAVAILABLE("PLACE-E001", HttpStatus.BAD_GATEWAY, "지도 서비스 응답에 실패했습니다."),
  PLACE_PHOTO_NOT_FOUND("PLACE-E002", HttpStatus.NOT_FOUND, "장소 사진을 찾을 수 없습니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}