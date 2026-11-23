package org.sopt.buddys.domain.place.service.result;

public enum PlacePhotoSource {
  /** 사용자가 코스에 올린 사진 */
  USER,
  /** 구글 Places 사진 */
  GOOGLE,
  /** 장소가 속한 도시로 검색한 Unsplash 사진 (구글 한도 초과 또는 구글 사진이 없을 때) */
  UNSPLASH
}
