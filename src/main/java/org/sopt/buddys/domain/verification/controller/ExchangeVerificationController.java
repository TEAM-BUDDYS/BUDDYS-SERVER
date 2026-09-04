package org.sopt.buddys.domain.verification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.verification.dto.request.ExchangeDocumentUploadUrlRequest;
import org.sopt.buddys.domain.verification.dto.response.ExchangeDocumentUploadUrlResponse;
import org.sopt.buddys.domain.verification.service.ExchangeDocumentUploadService;
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

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verifications/exchange")
@Tag(name = "Verification", description = "학교 인증 API")
public class ExchangeVerificationController {

  private final ExchangeDocumentUploadService exchangeDocumentUploadService;

  @Operation(
      summary = "파견교 인증 서류 업로드 URL 발급",
      description = """
          클라이언트가 파견교 인증 서류를 S3에 직접 PUT할 수 있는 presigned URL을 발급합니다.

          - presigned URL의 유효 시간은 발급 후 5분입니다.
          - 지원하는 Content-Type: application/pdf, image/jpeg, image/png
          - 최대 파일 크기는 10MB입니다.
          - S3 PUT 요청에는 발급 요청과 동일한 Content-Type을 사용해야 합니다.
          - 업로드 완료 후 documentKey를 파견교 인증 신청 API에 전달해야 합니다.
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "발급 성공")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PostMapping("/upload-url")
  public BaseResponse<ExchangeDocumentUploadUrlResponse> createUploadUrl(
      @Parameter(hidden = true) @LoginUser Long userId,
      @RequestBody @Valid ExchangeDocumentUploadUrlRequest request
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ExchangeDocumentUploadUrlResponse.from(
            exchangeDocumentUploadService.createUploadUrl(
                userId,
                request.contentType(),
                request.fileSize()
            )
        )
    );
  }
}
