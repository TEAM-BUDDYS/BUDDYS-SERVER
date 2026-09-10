package org.sopt.buddys.domain.verification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.verification.code.ExchangeVerificationSuccessCode;
import org.sopt.buddys.domain.verification.dto.request.ExchangeDocumentUploadUrlRequest;
import org.sopt.buddys.domain.verification.dto.request.ExchangeVerificationSubmitRequest;
import org.sopt.buddys.domain.verification.dto.response.ExchangeDocumentUploadUrlResponse;
import org.sopt.buddys.domain.verification.dto.response.ExchangeVerificationSubmitResponse;
import org.sopt.buddys.domain.verification.service.ExchangeDocumentUploadService;
import org.sopt.buddys.domain.verification.service.ExchangeVerificationService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.http.ResponseEntity;
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
  private final ExchangeVerificationService exchangeVerificationService;

  @Operation(
      summary = "파견교 인증 서류 업로드 URL 발급",
      description = """
          클라이언트가 파견교 인증 서류를 S3에 직접 PUT할 수 있는 presigned URL을 발급합니다.

          - presigned URL의 유효 시간은 발급 후 5분입니다.
          - 지원하는 Content-Type: application/pdf, image/jpeg, image/png
          - 최대 파일 크기는 10MB입니다.
          - S3 PUT 요청에는 발급 요청과 동일한 Content-Type을 사용해야 합니다.
          - 요청할 때마다 새로운 객체 키와 presigned URL이 발급됩니다.
          - 업로드 완료 후 documentKey를 파견교 인증 신청 API에 전달해야 합니다.
          - 새 신청이 정상 접수되면 기존에 접수된 서류 객체는 S3에서 삭제됩니다.
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

  @Operation(
      summary = "파견교 인증 신청",
      description = "presigned URL로 S3 업로드를 완료한 뒤 서류 정보를 전달해 인증 신청을 접수합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "신청 접수 성공"),
      @ApiResponse(responseCode = "400", description = "서류가 없거나 업로드 정보가 일치하지 않음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PostMapping
  public ResponseEntity<BaseResponse<ExchangeVerificationSubmitResponse>> submit(
      @Parameter(hidden = true) @LoginUser Long userId,
      @RequestBody @Valid ExchangeVerificationSubmitRequest request
  ) {
    return ResponseEntity.status(ExchangeVerificationSuccessCode.VERIFICATION_SUBMITTED.getHttpStatus())
        .body(BaseResponse.success(
            ExchangeVerificationSuccessCode.VERIFICATION_SUBMITTED,
            ExchangeVerificationSubmitResponse.from(exchangeVerificationService.submit(
                userId,
                request.documentKey(),
                request.originalFileName(),
                request.contentType(),
                request.fileSize()
            ))
        ));
  }
}
