package org.sopt.buddys.domain.location.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.dto.response.CountryResponse;
import org.sopt.buddys.domain.location.service.CountryService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
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
  private final CountryService countryService;

  @Operation(summary = "국가 검색", description = "검색 키워드에 해당하는 국가를 검색합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "검색 성공"),
      @ApiResponse(responseCode = "400", description = "검색 키워드가 비어있음")
  })
  @CommonErrorResponses
  @GetMapping("/search")
  public BaseResponse<List<CountryResponse>> searchCountries(
      @Parameter(description = "검색 키워드", example = "대한민국")
      @RequestParam @NotBlank String keyword
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK, countryService.searchCountries(keyword).stream()
        .map(CountryResponse::from).toList()
    );
  }

}
