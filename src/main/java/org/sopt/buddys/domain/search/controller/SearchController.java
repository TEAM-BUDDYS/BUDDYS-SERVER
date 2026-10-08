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
import org.sopt.buddys.domain.search.dto.request.SearchSuggestionRequest;
import org.sopt.buddys.domain.search.dto.response.SearchResponse;
import org.sopt.buddys.domain.search.dto.response.SearchSuggestionResponse;
import org.sopt.buddys.domain.search.service.SearchService;
import org.sopt.buddys.domain.search.service.SearchSuggestionService;
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
  private final SearchSuggestionService searchSuggestionService;

  @Operation(
      summary = "통합 검색",
      description = "검색어로 코스, 사용자, 모집 중 동행 게시글을 동시에 검색합니다. "
          + "type을 생략하면 세 영역을 모두 조회하고 반환하며, POST/COURSE/USER를 지정하면 해당 영역만 조회하고 반환합니다. "
          + "선택하지 않은 영역은 응답 필드에서 제외됩니다. 잘못된 type은 400(GLB-E001)을 반환합니다. "
          + "sort=LATEST(최신순, 기본값) 또는 BOOKMARK(전체 사용자의 저장 수 순)를 지원합니다. "
          + "USER는 항상 최신순입니다. 잘못된 sort는 400(GLB-E001)을 반환합니다. "
          + "각 영역에 동일한 페이지 번호와 크기를 적용합니다. "
          + "courses.totalElements와 users.totalElements는 page=0에서만 전체 건수를 제공하며 "
          + "page>0에서는 건수를 조회하지 않고 명시적으로 null을 반환합니다. "
          + "posts.totalElements는 페이지 번호·크기와 무관하게 검색어에 일치하는 "
          + "삭제되지 않은 모집 중 게시글의 전체 수입니다."
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
            request.sizeOrDefault(),
            request.searchType(),
            request.sortOrDefault()
        ))
    );
  }

  @Operation(
      summary = "검색어 자동완성",
      description = "DB에 존재하는 국가, 도시, 장소, 사용자, 코스 및 모집 중 동행 게시글에서 자동완성 검색어를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "자동완성 검색어 조회 성공. 결과가 없으면 빈 목록 반환")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping("/suggestions")
  public BaseResponse<SearchSuggestionResponse> getSearchSuggestions(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @ParameterObject @Valid @ModelAttribute SearchSuggestionRequest request
  ) {
    return BaseResponse.success(
        SearchSuccessCode.SEARCH_SUGGESTIONS_FOUND,
        SearchSuggestionResponse.from(searchSuggestionService.getSuggestions(
            userId,
            request.normalizedKeyword(),
            request.sizeOrDefault()
        ))
    );
  }
}
