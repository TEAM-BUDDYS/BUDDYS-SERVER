package org.sopt.buddys.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.image.entity.ImageDomain;
import org.sopt.buddys.global.aws.s3.S3PresignedUploadResult;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class S3PresignedUrlServiceTest {

  @Mock
  private S3PresignedUrlManager s3PresignedUrlManager;

  @InjectMocks
  private S3PresignedUrlService s3PresignedUrlService;

  @DisplayName("지원하는 Content-Type이면 도메인 폴더/UUID/확장자로 구성된 key로 presigned URL을 발급한다")
  @ParameterizedTest
  @CsvSource({
      "image/jpeg, jpg",
      "image/png, png",
      "image/webp, webp"
  })
  void createUploadUrl_supportedContentType_generatesKeyWithDomainFolderAndExtension(
      String contentType,
      String expectedExtension
  ) {
    // given
    when(s3PresignedUrlManager.createUploadUrl(anyString(), anyString()))
        .thenReturn(new S3PresignedUploadResult("upload-url", "image-url"));

    // when
    S3PresignedUploadResult result =
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, contentType);

    // then
    assertThat(result.uploadUrl()).isEqualTo("upload-url");
    assertThat(result.imageUrl()).isEqualTo("image-url");

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3PresignedUrlManager).createUploadUrl(keyCaptor.capture(), contentTypeCaptor.capture());

    assertThat(keyCaptor.getValue())
        .matches("^posts/[0-9a-fA-F-]{36}" + expectedExtension + "$");
    assertThat(contentTypeCaptor.getValue()).isEqualTo(contentType);
  }

  @DisplayName("도메인에 따라 S3 key의 폴더 prefix가 달라진다")
  @Test
  void createUploadUrl_profileDomain_usesProfilesFolder() {
    // given
    when(s3PresignedUrlManager.createUploadUrl(anyString(), anyString()))
        .thenReturn(new S3PresignedUploadResult("upload-url", "image-url"));

    // when
    s3PresignedUrlService.createUploadUrl(1L, ImageDomain.PROFILE, "image/png");

    // then
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3PresignedUrlManager).createUploadUrl(keyCaptor.capture(), anyString());
    assertThat(keyCaptor.getValue()).startsWith("profiles/");
  }

  @DisplayName("대소문자가 섞인 Content-Type도 정상적으로 처리하고, 소문자로 정규화해서 S3에 전달한다")
  @Test
  void createUploadUrl_mixedCaseContentType_normalizesToLowerCase() {
    // given
    when(s3PresignedUrlManager.createUploadUrl(anyString(), anyString()))
        .thenReturn(new S3PresignedUploadResult("upload-url", "image-url"));

    // when
    s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "image/JPEG");

    // then
    ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3PresignedUrlManager).createUploadUrl(anyString(), contentTypeCaptor.capture());
    assertThat(contentTypeCaptor.getValue()).isEqualTo("image/jpeg");
  }

  @DisplayName("지원하지 않는 Content-Type이면 UNSUPPORTED_CONTENT_TYPE 예외를 던지고 S3를 호출하지 않는다")
  @Test
  void createUploadUrl_unsupportedContentType_throwsBaseException() {
    // when & then
    assertThatThrownBy(() ->
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "application/pdf")
    ).isInstanceOf(BaseException.class);

    verify(s3PresignedUrlManager, org.mockito.Mockito.never())
        .createUploadUrl(any(), any());
  }
}