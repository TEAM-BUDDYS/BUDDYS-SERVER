package org.sopt.buddys.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.post.dto.request.CreatePostRequest;
import org.sopt.buddys.domain.post.dto.response.CreatePostResponse;
import org.sopt.buddys.domain.post.service.PostService;
import org.sopt.buddys.domain.post.service.command.CreatePostCommand;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@Tag(name = "Post", description = "게시글 API")
public class PostController {

  private final PostService postService;

  @Operation(summary = "게시글 작성", description = "로그인한 사용자가 게시글을 작성합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "작성 성공"),
      @ApiResponse(responseCode = "404", description = "국가, 도시, 태그 또는 사용자를 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PostMapping
  public ResponseEntity<BaseResponse<CreatePostResponse>> createPost(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid CreatePostRequest request
  ) {
    return ResponseEntity
        .status(GlobalSuccessCode.CREATED.getHttpStatus())
        .body(BaseResponse.success(
            GlobalSuccessCode.CREATED,
            CreatePostResponse.from(postService.createPost(userId, toCommand(request)))
        )
    );
  }

  private CreatePostCommand toCommand(CreatePostRequest request) {
    return new CreatePostCommand(
        request.countryId(),
        request.cityId(),
        request.startDate(),
        request.endDate(),
        request.title(),
        request.content(),
        request.ageConditions(),
        request.gender(),
        request.companionType(),
        request.recruitmentCountType(),
        request.tagIds(),
        request.imageUrls()
    );
  }
}
