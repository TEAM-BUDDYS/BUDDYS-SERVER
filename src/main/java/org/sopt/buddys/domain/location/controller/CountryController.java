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
import org.sopt.buddys.domain.location.dto.response.CountryListResponse;
import org.sopt.buddys.domain.location.service.CountryService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/countries")
@Tag(name = "Country", description = "국가 검색 API")
public class CountryController {
  private static final int MAX_SIZE = 100;
  private final CountryService countryService;

  @Operation(summary = "국가 검색", description = "검색 키워드에 해당하는 국가를 검색합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "검색 성공"),
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping("/search")
  public BaseResponse<CountryListResponse> searchCountries(
      @Parameter(description = "검색 키워드", example = "대한민국")
      @RequestParam @NotBlank String keyword,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 "+ MAX_SIZE + "이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_SIZE) int size
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK,
        CountryListResponse.from(countryService.searchCountries(keyword, page, size))
    );
  }

  @Operation(summary = "국가 목록 조회", description = "전체 국가 목록을 ㄱㄴㄷ순으로 무한 스크롤 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<CountryListResponse> getCountries(
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 " + MAX_SIZE + "이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_SIZE) int size
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK,
        CountryListResponse.from(countryService.getCountries(page, size))
    );
  }
}
