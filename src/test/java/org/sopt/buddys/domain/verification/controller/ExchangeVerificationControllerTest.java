package org.sopt.buddys.domain.verification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.ExchangeDocumentUploadService;
import org.sopt.buddys.domain.verification.service.ExchangeVerificationService;
import org.sopt.buddys.domain.verification.service.result.ExchangeDocumentUploadUrlResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationSubmitResult;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class ExchangeVerificationControllerTest {

  private static final long LOGIN_USER_ID = 7L;

  private ExchangeDocumentUploadService exchangeDocumentUploadService;
  private ExchangeVerificationService exchangeVerificationService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    exchangeDocumentUploadService = mock(ExchangeDocumentUploadService.class);
    exchangeVerificationService = mock(ExchangeVerificationService.class);
    mockMvc = MockMvcBuilders
        .standaloneSetup(new ExchangeVerificationController(
            exchangeDocumentUploadService,
            exchangeVerificationService
        ))
        .setCustomArgumentResolvers(new TestLoginUserArgumentResolver())
        .build();
  }

  @DisplayName("S3 업로드가 완료된 서류의 파견교 인증 신청을 접수한다")
  @Test
  void submit_returnsCreatedVerification() throws Exception {
    // given
    String documentKey = "exchange-verifications/7/123e4567-e89b-12d3-a456-426614174000.pdf";
    when(exchangeVerificationService.submit(
        LOGIN_USER_ID,
        documentKey,
        "교환학생_입학허가서.pdf",
        "application/pdf",
        823_044L
    )).thenReturn(new ExchangeVerificationSubmitResult(
        12L,
        ExchangeVerificationStatus.PENDING
    ));

    // when & then
    mockMvc.perform(post("/api/v1/verifications/exchange")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "documentKey": "%s",
                  "originalFileName": "교환학생_입학허가서.pdf",
                  "contentType": "application/pdf",
                  "fileSize": 823044
                }
                """.formatted(documentKey)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("EXCH-S001"))
        .andExpect(jsonPath("$.data.verificationId").value(12))
        .andExpect(jsonPath("$.data.status").value("PENDING"));

    verify(exchangeVerificationService).submit(
        LOGIN_USER_ID,
        documentKey,
        "교환학생_입학허가서.pdf",
        "application/pdf",
        823_044L
    );
  }

  @DisplayName("파견교 인증 서류 업로드 URL과 documentKey를 반환한다")
  @Test
  void createUploadUrl_returnsUploadUrlAndDocumentKey() throws Exception {
    // given
    String documentKey = "exchange/7/document-id.pdf";
    when(exchangeDocumentUploadService.createUploadUrl(
        LOGIN_USER_ID,
        "application/pdf",
        823_044L
    )).thenReturn(new ExchangeDocumentUploadUrlResult("https://upload-url", documentKey));

    // when & then
    mockMvc.perform(post("/api/v1/verifications/exchange/upload-url")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "contentType": "application/pdf",
                  "fileSize": 823044
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S001"))
        .andExpect(jsonPath("$.data.uploadUrl").value("https://upload-url"))
        .andExpect(jsonPath("$.data.documentKey").value(documentKey));

    verify(exchangeDocumentUploadService).createUploadUrl(
        LOGIN_USER_ID,
        "application/pdf",
        823_044L
    );
  }

  private static class TestLoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(LoginUser.class)
          && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {
      return LOGIN_USER_ID;
    }
  }
}
