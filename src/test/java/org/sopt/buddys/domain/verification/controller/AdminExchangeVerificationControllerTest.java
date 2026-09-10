package org.sopt.buddys.domain.verification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.ExchangeVerificationAdminService;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationDetailResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult.ExchangeVerificationSummaryResult;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class AdminExchangeVerificationControllerTest {

  private static final long ADMIN_USER_ID = 1L;

  private ExchangeVerificationAdminService service;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    service = mock(ExchangeVerificationAdminService.class);
    mockMvc = MockMvcBuilders
        .standaloneSetup(new AdminExchangeVerificationController(service))
        .setCustomArgumentResolvers(new TestLoginUserArgumentResolver())
        .build();
  }

  @DisplayName("처리 상태별 서류 인증 신청 목록을 반환한다")
  @Test
  void getVerifications_returnsFilteredList() throws Exception {
    // given
    LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 30, 14, 20);
    when(service.getVerifications(ADMIN_USER_ID, ExchangeVerificationStatus.PENDING, 0, 20))
        .thenReturn(new ExchangeVerificationListResult(
            List.of(new ExchangeVerificationSummaryResult(
                10L,
                2L,
                "지현",
                submittedAt,
                ExchangeVerificationStatus.PENDING
            )),
            0,
            20,
            false
        ));

    // when & then
    mockMvc.perform(get("/api/v1/admin/verifications/exchange")
            .param("status", "PENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S001"))
        .andExpect(jsonPath("$.data.content[0].verificationId").value(10))
        .andExpect(jsonPath("$.data.content[0].userId").value(2))
        .andExpect(jsonPath("$.data.content[0].nickname").value("지현"))
        .andExpect(jsonPath("$.data.content[0].submittedAt").value("2026-08-30T05:20:00Z"))
        .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false));

    verify(service).getVerifications(ADMIN_USER_ID, ExchangeVerificationStatus.PENDING, 0, 20);
  }

  @DisplayName("서류 인증 신청 상세 정보를 반환한다")
  @Test
  void getVerification_returnsDetail() throws Exception {
    // given
    LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 30, 14, 20);
    when(service.getVerification(ADMIN_USER_ID, 10L))
        .thenReturn(new ExchangeVerificationDetailResult(
            10L,
            2L,
            "지현",
            submittedAt,
            ExchangeVerificationStatus.REJECTED,
            "교환학생 확인서.pdf",
            "https://example.com/presigned-document",
            "서류가 확인되지 않습니다."
        ));

    // when & then
    mockMvc.perform(get("/api/v1/admin/verifications/exchange/{verificationId}", 10L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S001"))
        .andExpect(jsonPath("$.data.verificationId").value(10))
        .andExpect(jsonPath("$.data.userId").value(2))
        .andExpect(jsonPath("$.data.nickname").value("지현"))
        .andExpect(jsonPath("$.data.submittedAt").value("2026-08-30T05:20:00Z"))
        .andExpect(jsonPath("$.data.status").value("REJECTED"))
        .andExpect(jsonPath("$.data.originalFileName").value("교환학생 확인서.pdf"))
        .andExpect(jsonPath("$.data.documentUrl")
            .value("https://example.com/presigned-document"))
        .andExpect(jsonPath("$.data.rejectionReason").value("서류가 확인되지 않습니다."));

    verify(service).getVerification(ADMIN_USER_ID, 10L);
  }

  @DisplayName("서류 인증 신청을 승인한다")
  @Test
  void approveVerification_returnsSuccess() throws Exception {
    // when & then
    mockMvc.perform(patch(
            "/api/v1/admin/verifications/exchange/{verificationId}/approve",
            10L
        ))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S001"));

    verify(service).approveVerification(ADMIN_USER_ID, 10L);
  }

  @DisplayName("반려 사유와 함께 서류 인증 신청을 반려한다")
  @Test
  void rejectVerification_returnsSuccess() throws Exception {
    // when & then
    mockMvc.perform(patch(
            "/api/v1/admin/verifications/exchange/{verificationId}/reject",
            10L
        )
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "rejectionReason": "서류가 확인되지 않습니다."
            }
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.code").value("GLB-S001"));

    verify(service).rejectVerification(
        ADMIN_USER_ID,
        10L,
        "서류가 확인되지 않습니다."
    );
  }

  @DisplayName("반려 사유가 공백이면 잘못된 요청을 반환한다")
  @Test
  void rejectVerification_blankReason_returnsBadRequest() throws Exception {
    // when & then
    mockMvc.perform(patch(
            "/api/v1/admin/verifications/exchange/{verificationId}/reject",
            10L
        )
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "rejectionReason": " "
            }
            """))
        .andExpect(status().isBadRequest());
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
      return ADMIN_USER_ID;
    }
  }
}
