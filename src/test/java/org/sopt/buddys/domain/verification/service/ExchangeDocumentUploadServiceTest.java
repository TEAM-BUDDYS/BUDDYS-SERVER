package org.sopt.buddys.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.sopt.buddys.domain.verification.code.ExchangeVerificationErrorCode;
import org.sopt.buddys.domain.verification.service.result.ExchangeDocumentUploadUrlResult;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
class ExchangeDocumentUploadServiceTest {

  private static final long USER_ID = 7L;
  private static final long VALID_FILE_SIZE = 823_044L;
  private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

  @Mock
  private S3PresignedUrlManager s3PresignedUrlManager;

  @InjectMocks
  private ExchangeDocumentUploadService exchangeDocumentUploadService;

  @DisplayName("PDF, JPEG, PNG 서류는 사용자별 경로와 알맞은 확장자로 업로드 URL을 발급한다")
  @ParameterizedTest
  @CsvSource({
      "application/pdf, \\.pdf",
      "image/jpeg, \\.jpg",
      "image/png, \\.png"
  })
  void createUploadUrl_supportedType_generatesUserScopedDocumentKey(
      String contentType,
      String expectedExtension
  ) {
    // given
    when(s3PresignedUrlManager.createPutUrl(anyString(), anyString(), anyLong()))
        .thenReturn("upload-url");

    // when
    ExchangeDocumentUploadUrlResult result = exchangeDocumentUploadService.createUploadUrl(
        USER_ID,
        contentType,
        VALID_FILE_SIZE
    );

    // then
    assertThat(result.uploadUrl()).isEqualTo("upload-url");
    assertThat(result.documentKey()).matches(
        "^exchange-verifications/" + USER_ID + "/[0-9a-fA-F-]{36}" + expectedExtension + "$"
    );

    ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3PresignedUrlManager).createPutUrl(
        org.mockito.ArgumentMatchers.eq(result.documentKey()),
        contentTypeCaptor.capture(),
        org.mockito.ArgumentMatchers.eq(VALID_FILE_SIZE)
    );
    assertThat(contentTypeCaptor.getValue()).isEqualTo(contentType);
  }

  @DisplayName("Content-Type은 소문자로 정규화한다")
  @Test
  void createUploadUrl_mixedCaseContentType_normalizesContentType() {
    // given
    when(s3PresignedUrlManager.createPutUrl(anyString(), anyString(), anyLong()))
        .thenReturn("upload-url");

    // when
    exchangeDocumentUploadService.createUploadUrl(USER_ID, "Application/PDF", VALID_FILE_SIZE);

    // then
    verify(s3PresignedUrlManager).createPutUrl(
        anyString(),
        org.mockito.ArgumentMatchers.eq("application/pdf"),
        org.mockito.ArgumentMatchers.eq(VALID_FILE_SIZE)
    );
  }

  @DisplayName("지원하지 않는 파일 형식은 거부한다")
  @Test
  void createUploadUrl_unsupportedType_throwsBaseException() {
    // when & then
    assertThatThrownBy(() ->
        exchangeDocumentUploadService.createUploadUrl(USER_ID, "image/webp", VALID_FILE_SIZE)
    )
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ExchangeVerificationErrorCode.UNSUPPORTED_DOCUMENT_TYPE)
        );

    verify(s3PresignedUrlManager, never()).createPutUrl(anyString(), anyString(), anyLong());
  }

  @DisplayName("허용 범위를 벗어난 파일 크기는 거부한다")
  @ParameterizedTest
  @ValueSource(longs = {0L, -1L, MAX_FILE_SIZE + 1})
  void createUploadUrl_invalidFileSize_throwsBaseException(long fileSize) {
    // when & then
    assertThatThrownBy(() ->
        exchangeDocumentUploadService.createUploadUrl(USER_ID, "application/pdf", fileSize)
    )
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode())
                .isEqualTo(ExchangeVerificationErrorCode.DOCUMENT_FILE_TOO_LARGE)
        );

    verify(s3PresignedUrlManager, never()).createPutUrl(anyString(), anyString(), anyLong());
  }

  @DisplayName("최대 크기인 10MB 파일은 허용한다")
  @Test
  void createUploadUrl_maxFileSize_succeeds() {
    // given
    when(s3PresignedUrlManager.createPutUrl(anyString(), anyString(), anyLong()))
        .thenReturn("upload-url");

    // when
    exchangeDocumentUploadService.createUploadUrl(USER_ID, "application/pdf", MAX_FILE_SIZE);

    // then
    verify(s3PresignedUrlManager).createPutUrl(anyString(), anyString(), anyLong());
  }
}
