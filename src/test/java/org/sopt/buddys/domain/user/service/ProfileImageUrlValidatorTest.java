package org.sopt.buddys.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class ProfileImageUrlValidatorTest {

  private static final String UPLOAD_URL_PREFIX =
      "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/";
  private static final String CURRENT_URL = "http://k.kakaocdn.net/profile/current.jpg";

  @InjectMocks
  private ProfileImageUrlValidator profileImageUrlValidator;

  @Mock
  private S3PresignedUrlManager s3PresignedUrlManager;

  @BeforeEach
  void setUp() {
    lenient().when(s3PresignedUrlManager.buildPublicUrl("profiles/")).thenReturn(UPLOAD_URL_PREFIX);
  }

  @DisplayName("프로필 이미지 업로드 경로의 URL은 허용한다")
  @Test
  void validate_uploadedProfileImageUrl_passes() {
    // given
    String requestedUrl = UPLOAD_URL_PREFIX + "0b6f2c1e-7a55-4a52-9d0c-3f1f6f2f9f10.png";

    // when & then
    assertThatCode(() -> profileImageUrlValidator.validate(requestedUrl, CURRENT_URL))
        .doesNotThrowAnyException();
  }

  @DisplayName("현재 설정된 프로필 이미지 URL은 외부 도메인이어도 그대로 허용한다")
  @Test
  void validate_sameAsCurrentUrl_passes() {
    // when & then
    assertThatCode(() -> profileImageUrlValidator.validate(CURRENT_URL, CURRENT_URL))
        .doesNotThrowAnyException();
    then(s3PresignedUrlManager).shouldHaveNoInteractions();
  }

  @DisplayName("프로필 이미지를 삭제하는 null은 허용한다")
  @Test
  void validate_null_passes() {
    // when & then
    assertThatCode(() -> profileImageUrlValidator.validate(null, CURRENT_URL))
        .doesNotThrowAnyException();
  }

  @DisplayName("업로드 경로가 아닌 URL은 거부한다")
  @ParameterizedTest
  @ValueSource(strings = {
      "https://attacker.example/pixel.png",
      "javascript:alert(1)",
      "",
      "   ",
      "http://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/a.png",
      "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/posts/a.png",
      "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/",
      "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/../posts/a.png",
      "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com/profiles/a.png?x=1",
      "https://buddys-test-bucket.s3.ap-northeast-2.amazonaws.com.attacker.example/profiles/a.png"
  })
  void validate_urlOutsideUploadPath_throws(String requestedUrl) {
    // when & then
    assertThatThrownBy(() -> profileImageUrlValidator.validate(requestedUrl, CURRENT_URL))
        .isInstanceOf(BaseException.class)
        .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
            .isEqualTo(UserErrorCode.INVALID_PROFILE_IMAGE_URL));
  }
}
