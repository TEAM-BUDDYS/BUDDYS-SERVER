package org.sopt.buddys.domain.verification.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.ExchangeVerificationAdminService;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult;
import org.sopt.buddys.domain.verification.service.result.ExchangeVerificationListResult.ExchangeVerificationSummaryResult;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.core.MethodParameter;
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
