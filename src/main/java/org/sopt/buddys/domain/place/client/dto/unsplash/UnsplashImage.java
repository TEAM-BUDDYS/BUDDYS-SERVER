package org.sopt.buddys.domain.place.client.dto.unsplash;

/**
 * Unsplash 검색 결과 중 화면 표시에 필요한 값만 추린 것.
 *
 * @param rawUrl        크기 파라미터(w 등)를 붙여 쓰는 원본 이미지 URL. Unsplash 가이드라인상 핫링크로 사용한다.
 * @param authorName    사진 작가 이름
 * @param authorPageUrl 사진 작가 Unsplash 프로필 URL (utm 파라미터 포함)
 */
public record UnsplashImage(
    String rawUrl,
    String authorName,
    String authorPageUrl
) {
}
