package org.sopt.buddys.domain.place.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
  GOOGLE_PLACES_UNAVAILABLE("PLACE-E001", HttpStatus.BAD_GATEWAY, "지도 서비스 응답에 실패했습니다."),
  PLACE_PHOTO_NOT_FOUND("PLACE-E002", HttpStatus.NOT_FOUND, "장소 사진을 찾을 수 없습니다."),
  MISSING_QUERY("PLACE-E003", HttpStatus.BAD_REQUEST, "검색어(query)를 입력해주세요."),
  INVALID_CATEGORY("PLACE-E004", HttpStatus.BAD_REQUEST, "유효하지 않은 장소 카테고리입니다."),
  LAT_LNG_MUST_BE_PAIRED("PLACE-E005", HttpStatus.BAD_REQUEST, "위도(lat)와 경도(lng)는 함께 전달해야 합니다."),
  MISSING_COORDINATES("PLACE-E006", HttpStatus.BAD_REQUEST, "위도(lat)와 경도(lng)는 필수입니다."),
  INVALID_RADIUS("PLACE-E007", HttpStatus.BAD_REQUEST, "radius는 1 이상 50000 이하이어야 합니다."),
  INVALID_COORDINATE_RANGE("PLACE-E008", HttpStatus.BAD_REQUEST, "위도는 -90~90, 경도는 -180~180 범위여야 합니다.");

  private final String code;
  private final HttpStatus httpStatus;
  private final String message;
}
