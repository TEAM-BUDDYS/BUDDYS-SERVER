package org.sopt.buddys.domain.location.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.dto.response.CityListResponse;
import org.sopt.buddys.domain.location.service.CityService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/countries/{countryId}/cities")
@Tag(name = "City", description = "도시 검색 API")
public class CityController {
  private final CityService cityService;

  @Operation(summary = "도시 검색", description = "특정 국가에 속한 도시를 검색합니다.")
  @ApiResponses({ @ApiResponse(responseCode = "200", description = "검색 성공") })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping("/search")
  public BaseResponse<CityListResponse> searchCities(
      @Parameter(description = "국가 ID", example = "32")
      @PathVariable Long countryId,
      @Parameter(description = "검색 키워드", example = "서울")
      @RequestParam @NotBlank String keyword,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK, CityListResponse.from(cityService.searchCities(countryId, keyword, page, size)));
  }
}
