package org.sopt.buddys.domain.tag.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.tag.dto.response.TagResponse;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.service.TagService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/tags")
@Tag(name = "Tag", description = "온보딩 태그 API")
public class TagController {
  private final TagService tagService;

  @Operation(summary = "태그 목록 조회", description = "type(ACTIVITY, INTEREST, TRAVEL_STYLE)에 해당하는 태그 목록을 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 태그 타입")
  })

  @GetMapping("/{type}")
  public ResponseEntity<BaseResponse<List<TagResponse>>> getTags(
      @Parameter(description = "태그 타입", example = "ACTIVITY")
      @PathVariable TagType type
  ) {
    return ResponseEntity.ok(BaseResponse.success(GlobalSuccessCode.OK, tagService.getTagsByType(type)));
  }
}
