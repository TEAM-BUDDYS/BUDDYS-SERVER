package org.sopt.buddys.domain.verification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.location.controller.swagger.UniversityNotFoundResponse;
import org.sopt.buddys.domain.verification.controller.swagger.VerificationCodeInvalidResponse;
import org.sopt.buddys.domain.verification.dto.request.UniversityVerificationConfirmRequest;
import org.sopt.buddys.domain.verification.dto.request.UniversityVerificationRequest;
import org.sopt.buddys.domain.verification.service.UniversityVerificationService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verifications/university/email")
@Tag(name = "Verification", description = "학교 인증 API")
public class UniversityVerificationController {

  private final UniversityVerificationService universityVerificationService;

  @Operation(
      summary = "학교 이메일 인증 코드 발송",
      description = """
          학교 이메일 도메인으로 대학교를 찾아 6자리 인증 코드(영어 대문자 + 숫자)를 해당 이메일로 발송합니다.

          - 이메일 도메인과 일치하는 대학교가 없으면 404(LOC-E003)가 반환됩니다.
          - 같은 사용자가 다시 요청하면 이전 코드는 폐기되고 새 코드가 발급됩니다.
          """
  )
  @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공")
  @InvalidRequestResponse
  @UniversityNotFoundResponse
  @CommonErrorResponses
  @PostMapping
  public BaseResponse<Void> sendVerification(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid UniversityVerificationRequest request
  ) {
    universityVerificationService.sendVerification(userId, request.email());
    return BaseResponse.success(GlobalSuccessCode.OK);
  }

  @Operation(
      summary = "학교 이메일 인증 코드 확인",
      description = """
          앱 화면에서 입력한 인증 코드를 검증합니다. 일치하면 로그인 사용자의 학교 인증을 완료하고 코드를 폐기합니다.

          - 코드가 없거나 틀리거나 만료됐으면 400(UNIV-E001)이 반환됩니다.
          """
  )
  @ApiResponse(responseCode = "200", description = "학교 인증 완료")
  @InvalidRequestResponse
  @VerificationCodeInvalidResponse
  @UniversityNotFoundResponse
  @CommonErrorResponses
  @PostMapping("/confirm")
  public BaseResponse<Void> confirmVerification(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid UniversityVerificationConfirmRequest request
  ) {
    universityVerificationService.confirmVerification(userId, request.code());
    return BaseResponse.success(GlobalSuccessCode.OK);
  }
}
