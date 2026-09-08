package org.sopt.buddys.domain.airline.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.airline.dto.response.AirlineListResponse;
import org.sopt.buddys.domain.airline.service.AirlineService;
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
@RequestMapping("/api/v1/airlines")
@Tag(name = "Airline", description = "항공사 검색 API")
public class AirlineController {
  private final AirlineService airlineService;

  @Operation(
      summary = "항공사 검색",
      description = """
          코스 작성 시 항공편 정보를 등록할 수 있도록 항공사명 또는 항공사 코드로 항공사를 검색합니다.

          - keyword는 대소문자 구분 없이 항공사명 또는 항공사 코드(IATA)에 부분 일치(contains)로 검색되며, 검색 결과는 항공사명 오름차순으로 정렬됩니다.
          - keyword를 생략하거나 빈 문자열/공백만 전달하면 빈 리스트가 반환됩니다.
          - 커서 없는 Slice 기반 페이지네이션을 사용하며, 다음 페이지 존재 여부는 응답의 hasNext로 확인합니다.
          """
  )
  @ApiResponse(responseCode = "200", description = "검색 성공")
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping("/search")
  public BaseResponse<AirlineListResponse> searchAirlines(
      @Parameter(
          description = "검색 키워드. 대소문자 구분 없이 항공사명 또는 항공사 코드에 부분 일치합니다. "
              + "생략하거나 빈 문자열/공백만 전달하면 빈 리스트가 반환됩니다.",
          example = "대한항공"
      )
      @RequestParam(required = false) String keyword,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK,
        AirlineListResponse.from(airlineService.searchAirlines(keyword, page, size))
    );
  }
}
