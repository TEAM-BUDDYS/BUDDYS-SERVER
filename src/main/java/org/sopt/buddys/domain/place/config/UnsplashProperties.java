package org.sopt.buddys.domain.place.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Unsplash API 설정. accessKey가 비어 있으면 Unsplash 대체 사진을 쓰지 않는다.
 *
 * @param accessKey Unsplash 애플리케이션 Access Key
 * @param searchUrl 사진 검색 API URL
 * @param appName   작가 링크에 붙는 utm_source 값 (Unsplash 가이드라인)
 */
@Validated
@ConfigurationProperties(prefix = "unsplash")
public record UnsplashProperties(
    String accessKey,
    @NotBlank String searchUrl,
    @NotBlank String appName
) {

  public boolean enabled() {
    return accessKey != null && !accessKey.isBlank();
  }
}
