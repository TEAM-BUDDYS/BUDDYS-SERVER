package org.sopt.buddys.domain.verification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.location.controller.swagger.UniversityNotFoundResponse;
import org.sopt.buddys.domain.verification.dto.request.UniversityVerificationRequest;
import org.sopt.buddys.domain.verification.service.UniversityVerificationService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      summary = "학교 이메일 인증 요청",
      description = """
          학교 이메일 도메인으로 대학교를 찾아 인증 링크를 발송합니다.

          - 이메일 도메인과 일치하는 대학교가 없으면 404(LOC-E003)가 반환됩니다.
          """
  )
  @ApiResponse(responseCode = "200", description = "인증 메일 발송 성공")
  @InvalidRequestResponse
  @UniversityNotFoundResponse
  @CommonErrorResponses
  @PostMapping("/send")
  public BaseResponse<Void> sendVerification(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid UniversityVerificationRequest request
  ) {
    universityVerificationService.sendVerification(userId, request.email());
    return BaseResponse.success(GlobalSuccessCode.OK);
  }

  @Operation(
      summary = "학교 이메일 인증 확인",
      description = "인증 메일의 링크를 클릭하면 호출되는 엔드포인트입니다. 로그인 없이 토큰만으로 인증을 완료하고, 결과를 보여주는 HTML 페이지를 반환합니다."
  )
  @GetMapping(value = "/confirm", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> confirmVerification(
      @RequestParam @NotBlank String token
  ) {
    try {
      universityVerificationService.confirmVerification(token);
      return ResponseEntity.ok(renderResultPage("학교 이메일 인증이 완료되었습니다.", true));
    } catch (BaseException e) {
      log.warn("[UniversityVerificationConfirm] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(renderResultPage(e.getErrorCode().getMessage(), false));
    }
  }

  private String renderResultPage(String message, boolean success) {
    return """
        <!DOCTYPE html>
        <html lang="ko">
        <head>
          <meta charset="UTF-8">
          <title>학교 이메일 인증</title>
          <style>
            body { font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; background: #f7f7f8; }
            .card { text-align: center; padding: 40px; }
            p { font-size: 16px; color: #222; }
          </style>
        </head>
        <body>
          <div class="card">
            <h2>%s</h2>
            <p>%s</p>
          </div>
        </body>
        </html>
        """.formatted(success ? "인증 완료" : "인증 실패", message);
  }
}
