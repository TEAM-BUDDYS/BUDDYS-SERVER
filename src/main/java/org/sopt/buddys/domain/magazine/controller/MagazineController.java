package org.sopt.buddys.domain.magazine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.magazine.code.MagazineSuccessCode;
import org.sopt.buddys.domain.magazine.dto.request.MagazineListRequest;
import org.sopt.buddys.domain.magazine.dto.response.DeleteMagazineBookmarkSuccessResponse;
import org.sopt.buddys.domain.magazine.dto.response.MagazineBookmarkResponse;
import org.sopt.buddys.domain.magazine.dto.response.MagazineBookmarkSuccessResponse;
import org.sopt.buddys.domain.magazine.dto.response.MagazineListResponse;
import org.sopt.buddys.domain.magazine.dto.response.MagazineListSuccessResponse;
import org.sopt.buddys.domain.magazine.service.MagazineService;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/magazines")
@Tag(name = "Magazine", description = "매거진 API")
public class MagazineController {

  private final MagazineService magazineService;

  @Operation(
      summary = "매거진 목록 조회",
      description = "연도와 월을 기준으로 발행된 매거진 목록을 조회합니다. "
          + "year, month를 모두 생략하면 Asia/Seoul 기준 현재 연월을 적용합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "조회 성공",
          content = @Content(schema = @Schema(implementation = MagazineListSuccessResponse.class))
      ),
      @ApiResponse(responseCode = "401", description = "인증 필요")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<MagazineListResponse> getMagazines(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @ParameterObject @Valid @ModelAttribute MagazineListRequest request
  ) {
    return BaseResponse.success(
        MagazineSuccessCode.MAGAZINE_LIST_FOUND,
        MagazineListResponse.from(magazineService.getMagazines(
            userId,
            request.year(),
            request.month(),
            request.pageOrDefault(),
            request.sizeOrDefault()
        ))
    );
  }

  @Operation(
      summary = "매거진 저장",
      description = "로그인한 사용자가 매거진을 저장합니다. 이미 저장한 매거진에 같은 요청을 보내도 성공합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "저장 성공",
          content = @Content(schema = @Schema(implementation = MagazineBookmarkSuccessResponse.class))
      ),
      @ApiResponse(responseCode = "404", description = "매거진을 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PostMapping("/{magazineId}/bookmarks")
  public BaseResponse<MagazineBookmarkResponse> bookmarkMagazine(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "저장할 매거진 ID", example = "1", required = true)
      @PathVariable @Positive Long magazineId
  ) {
    return BaseResponse.success(
        MagazineSuccessCode.MAGAZINE_BOOKMARKED,
        MagazineBookmarkResponse.from(magazineService.bookmarkMagazine(userId, magazineId))
    );
  }

  @Operation(
      summary = "매거진 저장 취소",
      description = "로그인한 사용자가 저장한 매거진을 저장 취소합니다. 저장하지 않은 매거진에 요청해도 성공합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "저장 취소 성공",
          content = @Content(schema = @Schema(implementation = DeleteMagazineBookmarkSuccessResponse.class))
      ),
      @ApiResponse(responseCode = "404", description = "매거진을 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @DeleteMapping("/{magazineId}/bookmarks")
  public BaseResponse<MagazineBookmarkResponse> removeMagazineBookmark(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "저장 취소할 매거진 ID", example = "1", required = true)
      @PathVariable @Positive Long magazineId
  ) {
    return BaseResponse.success(
        MagazineSuccessCode.MAGAZINE_BOOKMARK_REMOVED,
        MagazineBookmarkResponse.from(magazineService.removeMagazineBookmark(userId, magazineId))
    );
  }
}
