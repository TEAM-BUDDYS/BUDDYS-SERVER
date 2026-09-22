package org.sopt.buddys.domain.verification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.verification.dto.request.ExchangeVerificationRejectRequest;
import org.sopt.buddys.domain.verification.dto.response.ExchangeVerificationDetailResponse;
import org.sopt.buddys.domain.verification.dto.response.ExchangeVerificationListResponse;
import org.sopt.buddys.domain.verification.entity.ExchangeVerificationStatus;
import org.sopt.buddys.domain.verification.service.ExchangeVerificationAdminService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/verifications/exchange")
@Tag(name = "Admin Verification", description = "관리자 서류 인증 API")
public class AdminExchangeVerificationController {

  private final ExchangeVerificationAdminService exchangeVerificationAdminService;

  @Operation(
      summary = "서류 인증 신청 상세 조회",
      description = "관리자가 서류 인증 신청 정보와 5분 동안 유효한 서류 열람 URL을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
      @ApiResponse(responseCode = "404", description = "인증 신청 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping("/{verificationId}")
  public BaseResponse<ExchangeVerificationDetailResponse> getVerification(
      @Parameter(hidden = true) @LoginUser Long adminUserId,
      @Parameter(description = "서류 인증 신청 ID", example = "1")
      @PathVariable @Positive Long verificationId
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ExchangeVerificationDetailResponse.from(
            exchangeVerificationAdminService.getVerification(adminUserId, verificationId)
        )
    );
  }

  @Operation(
      summary = "서류 인증 신청 승인",
      description = "관리자가 대기 중인 서류 인증 신청을 승인합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "승인 성공"),
      @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
      @ApiResponse(responseCode = "404", description = "인증 신청 없음"),
      @ApiResponse(responseCode = "409", description = "이미 처리된 인증 신청")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PatchMapping("/{verificationId}/approve")
  public BaseResponse<Void> approveVerification(
      @Parameter(hidden = true) @LoginUser Long adminUserId,
      @Parameter(description = "서류 인증 신청 ID", example = "1")
      @PathVariable @Positive Long verificationId
  ) {
    exchangeVerificationAdminService.approveVerification(adminUserId, verificationId);
    return BaseResponse.success(GlobalSuccessCode.OK);
  }

  @Operation(
      summary = "서류 인증 신청 반려",
      description = "관리자가 대기 중인 서류 인증 신청을 사유와 함께 반려합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "반려 성공"),
      @ApiResponse(responseCode = "400", description = "반려 사유 누락 또는 형식 오류"),
      @ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
      @ApiResponse(responseCode = "404", description = "인증 신청 없음"),
      @ApiResponse(responseCode = "409", description = "이미 처리된 인증 신청")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PatchMapping("/{verificationId}/reject")
  public BaseResponse<Void> rejectVerification(
      @Parameter(hidden = true) @LoginUser Long adminUserId,
      @Parameter(description = "서류 인증 신청 ID", example = "1")
      @PathVariable @Positive Long verificationId,
      @RequestBody @Valid ExchangeVerificationRejectRequest request
  ) {
    exchangeVerificationAdminService.rejectVerification(
        adminUserId,
        verificationId,
        request.rejectionReason()
    );
    return BaseResponse.success(GlobalSuccessCode.OK);
  }

  @Operation(
      summary = "서류 인증 신청 목록 조회",
      description = "관리자가 전체 신청 또는 처리 상태별 신청 목록을 최신순으로 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "403", description = "관리자 권한 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<ExchangeVerificationListResponse> getVerifications(
      @Parameter(hidden = true) @LoginUser Long adminUserId,
      @Parameter(description = "처리 상태. 생략하면 전체를 조회합니다.", example = "PENDING")
      @RequestParam(required = false) ExchangeVerificationStatus status,
      @Parameter(description = "페이지 번호. 0부터 시작합니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ExchangeVerificationListResponse.from(
            exchangeVerificationAdminService.getVerifications(adminUserId, status, page, size)
        )
    );
  }
}
