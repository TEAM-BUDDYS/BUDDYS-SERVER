package org.sopt.buddys.domain.image.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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

  private static final long VALID_FILE_SIZE = 204_800L;
  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

  @Mock
  private S3PresignedUrlManager s3PresignedUrlManager;

  @InjectMocks
  private S3PresignedUrlService s3PresignedUrlService;

  @DisplayName("지원하는 Content-Type이면 도메인 폴더/UUID/확장자로 구성된 key로 presigned URL을 발급한다")
  @ParameterizedTest
  @CsvSource({
      "image/jpeg, \\.jpg",
      "image/png, \\.png",
      "image/webp, \\.webp"
  })
  void createUploadUrl_supportedContentType_generatesKeyWithDomainFolderAndExtension(
      String contentType,
      String expectedExtension
  ) {
    // given
    when(s3PresignedUrlManager.createUploadUrl(anyString(), anyString(), anyLong()))
        .thenReturn(new S3PresignedUploadResult("upload-url", "image-url"));

    // when
    S3PresignedUploadResult result =
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, contentType, VALID_FILE_SIZE);

    // then
    assertThat(result.uploadUrl()).isEqualTo("upload-url");
    assertThat(result.imageUrl()).isEqualTo("image-url");

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Long> fileSizeCaptor = ArgumentCaptor.forClass(Long.class);
    verify(s3PresignedUrlManager)
        .createUploadUrl(keyCaptor.capture(), contentTypeCaptor.capture(), fileSizeCaptor.capture());

    assertThat(keyCaptor.getValue())
        .matches("^posts/[0-9a-fA-F-]{36}" + expectedExtension + "$");
    assertThat(contentTypeCaptor.getValue()).isEqualTo(contentType);
    assertThat(fileSizeCaptor.getValue()).isEqualTo(VALID_FILE_SIZE);
  }

  @DisplayName("도메인에 따라 S3 key의 폴더 prefix가 달라진다")
  @Test
  void createUploadUrl_profileDomain_usesProfilesFolder() {
    // given
    when(s3PresignedUrlManager.createUploadUrl(anyString(), anyString(), anyLong()))
        .thenReturn(new S3PresignedUploadResult("upload-url", "image-url"));

    // when
    s3PresignedUrlService.createUploadUrl(1L, ImageDomain.PROFILE, "image/png", VALID_FILE_SIZE);

    // then
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3PresignedUrlManager).createUploadUrl(keyCaptor.capture(), anyString(), anyLong());
    assertThat(keyCaptor.getValue()).startsWith("profiles/");
  }

  @DisplayName("대소문자가 섞인 Content-Type도 정상적으로 처리하고, 소문자로 정규화해서 S3에 전달한다")
  @Test
  void createUploadUrl_mixedCaseContentType_normalizesToLowerCase() {
    // given
    when(s3PresignedUrlManager.createUploadUrl(anyString(), anyString(), anyLong()))
        .thenReturn(new S3PresignedUploadResult("upload-url", "image-url"));

    // when
    s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "image/JPEG", VALID_FILE_SIZE);

    // then
    ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3PresignedUrlManager).createUploadUrl(anyString(), contentTypeCaptor.capture(), anyLong());
    assertThat(contentTypeCaptor.getValue()).isEqualTo("image/jpeg");
  }

  @DisplayName("지원하지 않는 Content-Type이면 UNSUPPORTED_CONTENT_TYPE 예외를 던지고 S3를 호출하지 않는다")
  @Test
  void createUploadUrl_unsupportedContentType_throwsBaseException() {
    // when & then
    assertThatThrownBy(() ->
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "application/pdf", VALID_FILE_SIZE)
    ).isInstanceOf(BaseException.class);

    verify(s3PresignedUrlManager, never()).createUploadUrl(any(), any(), anyLong());
  }

  @DisplayName("파일 크기가 최대 허용치를 초과하면 FILE_TOO_LARGE 예외를 던지고 S3를 호출하지 않는다")
  @Test
  void createUploadUrl_fileSizeExceedsMax_throwsBaseException() {
    // when & then
    assertThatThrownBy(() ->
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "image/png", MAX_FILE_SIZE + 1)
    ).isInstanceOf(BaseException.class);

    verify(s3PresignedUrlManager, never()).createUploadUrl(any(), any(), anyLong());
  }

  @DisplayName("파일 크기가 0 이하이면 FILE_TOO_LARGE 예외를 던진다")
  @ParameterizedTest
  @ValueSource(longs = {0L, -1L})
  void createUploadUrl_nonPositiveFileSize_throwsBaseException(long invalidFileSize) {
    // when & then
    assertThatThrownBy(() ->
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "image/png", invalidFileSize)
    ).isInstanceOf(BaseException.class);

    verify(s3PresignedUrlManager, never()).createUploadUrl(any(), any(), anyLong());
  }

  @DisplayName("파일 크기가 null이면 FILE_TOO_LARGE 예외를 던진다")
  @Test
  void createUploadUrl_nullFileSize_throwsBaseException() {
    // when & then
    assertThatThrownBy(() ->
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "image/png", null)
    ).isInstanceOf(BaseException.class);

    verify(s3PresignedUrlManager, never()).createUploadUrl(any(), any(), anyLong());
  }

  @DisplayName("최대 허용 크기와 정확히 같으면 정상적으로 발급된다")
  @Test
  void createUploadUrl_exactlyMaxFileSize_succeeds() {
    // given
    when(s3PresignedUrlManager.createUploadUrl(anyString(), anyString(), anyLong()))
        .thenReturn(new S3PresignedUploadResult("upload-url", "image-url"));

    // when
    S3PresignedUploadResult result =
        s3PresignedUrlService.createUploadUrl(1L, ImageDomain.POST, "image/png", MAX_FILE_SIZE);

    // then
    assertThat(result.uploadUrl()).isEqualTo("upload-url");
  }
}