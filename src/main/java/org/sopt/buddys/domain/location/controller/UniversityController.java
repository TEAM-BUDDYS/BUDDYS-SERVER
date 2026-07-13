package org.sopt.buddys.domain.location.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.controller.swagger.CountryNotFoundResponse;
import org.sopt.buddys.domain.location.dto.response.UniversityListResponse;
import org.sopt.buddys.domain.location.service.UniversityService;
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
@RequestMapping("/api/v1/countries/{countryId}/universities")
@Tag(name = "University", description = "대학교 검색 API")
public class UniversityController {
  private final UniversityService universityService;

  @Operation(
      summary = "대학교 검색",
      description = """
          특정 국가(countryId)에 속한 대학교를 이름으로 검색합니다.

          - 파견국가 선택 화면에서 국가를 먼저 고른 뒤, 그 국가에 속한 대학교만 목록으로 노출할 때 사용합니다.
          - keyword는 대소문자 구분 없이 부분 일치(contains)로 검색되며, 검색 결과는 대학교 이름 오름차순으로 정렬됩니다.
          - 다른 국가에 동명의 대학교가 있어도 결과에 섞이지 않고 countryId로 지정한 국가로만 범위가 제한됩니다.
          - 커서 없는 Slice 기반 페이지네이션을 사용하며, 다음 페이지 존재 여부는 응답의 hasNext로 확인합니다.
          """
  )
  @ApiResponse(
      responseCode = "200",
      description = "검색 성공",
      content = @Content(
          mediaType = "application/json",
          examples = @ExampleObject(value = """
              {
                "success": true,
                "code": "GLB-S001",
                "message": "요청이 성공했습니다.",
                "data": {
                  "universities": [
                    {
                      "id": 1,
                      "name": "Yonsei University",
                      "domain": "yonsei.ac.kr"
                    }
                  ],
                  "page": 0,
                  "size": 20,
                  "hasNext": false
                }
              }
              """)
      )
  )
  @InvalidRequestResponse
  @CountryNotFoundResponse
  @CommonErrorResponses
  @GetMapping("/search")
  public BaseResponse<UniversityListResponse> searchUniversities(
      @Parameter(description = "국가 ID. 존재하지 않는 ID를 전달하면 404가 반환됩니다.", example = "31", required = true)
      @PathVariable Long countryId,
      @Parameter(
          description = "검색 키워드. 대소문자 구분 없이 대학교 이름에 부분 일치합니다. "
              + "생략하거나 빈 문자열/공백만 전달하면 빈 리스트가 반환됩니다.",
          example = "Yonsei"
      )
      @RequestParam(required = false) String keyword,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK,
        UniversityListResponse.from(universityService.searchUniversities(countryId, keyword, page, size))
    );
  }
}