package org.sopt.buddys.domain.place.service.result;

/**
 * 장소 사진 조회 결과.
 *
 * @param url           리다이렉트할 이미지 URL
 * @param source        사진 출처
 * @param authorName    작가 이름. Unsplash 사진일 때만 값이 있다.
 * @param authorPageUrl 작가 프로필 URL. Unsplash 사진일 때만 값이 있다.
 */
public record PlacePhoto(
    String url,
    PlacePhotoSource source,
    String authorName,
    String authorPageUrl
) {

  public static PlacePhoto user(String url) {
    return new PlacePhoto(url, PlacePhotoSource.USER, null, null);
  }

  public static PlacePhoto google(String url) {
    return new PlacePhoto(url, PlacePhotoSource.GOOGLE, null, null);
  }

  public static PlacePhoto unsplash(String url, String authorName, String authorPageUrl) {
    return new PlacePhoto(url, PlacePhotoSource.UNSPLASH, authorName, authorPageUrl);
  }
}
