package org.sopt.buddys.domain.image.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SupportedImageTypeTest {

  @DisplayName("지원하는 Content-Type이면 해당하는 SupportedImageType을 반환한다")
  @ParameterizedTest
  @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
  void fromContentType_supportedType_returnsMatchingType(String contentType) {
    // when
    Optional<SupportedImageType> result = SupportedImageType.fromContentType(contentType);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getContentType()).isEqualTo(contentType);
  }

  @DisplayName("대소문자가 섞인 Content-Type도 대소문자 구분 없이 매칭한다")
  @ParameterizedTest
  @ValueSource(strings = {"image/JPEG", "Image/Png", "IMAGE/WEBP"})
  void fromContentType_mixedCase_matchesIgnoringCase(String contentType) {
    // when
    Optional<SupportedImageType> result = SupportedImageType.fromContentType(contentType);

    // then
    assertThat(result).isPresent();
  }

  @DisplayName("지원하지 않는 Content-Type이면 빈 Optional을 반환한다")
  @Test
  void fromContentType_unsupportedType_returnsEmpty() {
    // when
    Optional<SupportedImageType> result = SupportedImageType.fromContentType("application/pdf");

    // then
    assertThat(result).isEmpty();
  }

  @DisplayName("각 타입의 확장자는 점(.) 없는 순수 확장자 형태다")
  @ParameterizedTest
  @CsvSource({
      "JPEG, jpg",
      "PNG, png",
      "WEBP, webp"
  })
  void extension_isPlainExtensionWithoutDot(String name, String expectedExtension) {
    // given
    SupportedImageType type = SupportedImageType.valueOf(name);

    // then
    assertThat(type.getExtension()).isEqualTo(expectedExtension);
  }
}