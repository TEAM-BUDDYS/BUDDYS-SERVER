package org.sopt.buddys.domain.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.search.code.SearchSuccessCode;
import org.sopt.buddys.domain.search.dto.request.SearchRequest;
import org.sopt.buddys.domain.search.dto.response.SearchResponse;
import org.sopt.buddys.domain.search.service.SearchService;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "통합 검색 API")
public class SearchController {

  private final SearchService searchService;

  @Operation(
      summary = "통합 검색",
      description = "검색어로 코스, 사용자, 모집 중 동행 게시글을 동시에 검색합니다. 각 영역에 동일한 페이지 번호와 크기를 적용합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "검색 성공. 결과가 없는 영역은 빈 목록 반환")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<SearchResponse> search(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @ParameterObject @Valid @ModelAttribute SearchRequest request
  ) {
    return BaseResponse.success(
        SearchSuccessCode.SEARCH_SUCCEEDED,
        SearchResponse.from(searchService.search(
            userId,
            request.normalizedKeyword(),
            request.pageOrDefault(),
            request.sizeOrDefault()
        ))
    );
  }
}
