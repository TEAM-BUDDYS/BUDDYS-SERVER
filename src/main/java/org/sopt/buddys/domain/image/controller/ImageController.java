package org.sopt.buddys.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.image.controller.swagger.UnsupportedContentTypeResponse;
import org.sopt.buddys.domain.image.dto.request.PresignedUrlRequest;
import org.sopt.buddys.domain.image.dto.response.PresignedUrlResponse;
import org.sopt.buddys.domain.image.service.S3PresignedUrlService;
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

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
@Tag(name = "Image", description = "이미지 업로드 API")
public class ImageController {

  private final S3PresignedUrlService s3PresignedUrlService;

  @Operation(
      summary = "이미지 업로드용 presigned URL 발급",
      description = """
          클라이언트가 S3에 직접 PUT할 수 있는 presigned URL을 발급합니다.

          - presigned URL의 유효 시간은 발급 후 5분입니다.
          - 지원하는 Content-Type: image/jpeg, image/png, image/webp
          - 응답의 uploadUrl로 이미지 바이너리를 PUT 요청하면 업로드가 완료되며,
            imageUrl은 업로드 완료 후 게시글/프로필 등록 시 사용하는 최종 이미지 URL입니다.
          """
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "발급 성공")
  })
  @InvalidRequestResponse
  @UnsupportedContentTypeResponse
  @CommonErrorResponses
  @PostMapping("/presigned-url")
  public BaseResponse<PresignedUrlResponse> createPresignedUrl(
      @Parameter(hidden = true) @LoginUser Long userId,
      @RequestBody @Valid PresignedUrlRequest request
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        PresignedUrlResponse.from(
            s3PresignedUrlService.createUploadUrl(
                userId, request.imageDomain(), request.contentType(), request.fileSize()
            )
        )
    );
  }
}