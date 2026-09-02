package org.sopt.buddys.domain.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.post.code.PostSuccessCode;
import org.sopt.buddys.domain.post.dto.request.CreatePostRequest;
import org.sopt.buddys.domain.post.dto.request.PostListRequest;
import org.sopt.buddys.domain.post.dto.request.UpdatePostStatusRequest;
import org.sopt.buddys.domain.post.dto.request.UpdatePostRequest;
import org.sopt.buddys.domain.post.dto.response.ClosingSoonPostResponse;
import org.sopt.buddys.domain.post.dto.response.PostListResponse;
import org.sopt.buddys.domain.post.dto.response.PostBookmarkResponse;
import org.sopt.buddys.domain.post.dto.response.PostBookmarkSuccessResponse;
import org.sopt.buddys.domain.post.dto.response.CreatePostResponse;
import org.sopt.buddys.domain.post.dto.response.PostDetailResponse;
import org.sopt.buddys.domain.post.dto.response.UpdatePostStatusResponse;
import org.sopt.buddys.domain.post.dto.response.UpdatePostResponse;
import org.sopt.buddys.domain.post.dto.response.UpdatePostSuccessResponse;
import org.sopt.buddys.domain.post.dto.response.DeletePostResponse;
import org.sopt.buddys.domain.post.dto.response.DeletePostBookmarkSuccessResponse;
import org.sopt.buddys.domain.post.dto.response.DeletePostSuccessResponse;
import org.sopt.buddys.domain.post.service.PostService;
import org.sopt.buddys.domain.post.service.command.CreatePostCommand;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  @Operation(summary = "동행 게시글 목록 조회", description = "모집중인 동행 게시글 목록을 조건에 따라 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<PostListResponse> getPosts(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @ParameterObject @Valid @ModelAttribute PostListRequest request
  ) {
    return BaseResponse.success(
        PostSuccessCode.POST_LIST_FOUND,
        PostListResponse.from(
            postService.getPosts(userId, request.toCondition(), request.pageOrDefault(), request.sizeOrDefault()))
    );
  }

  @Operation(
      summary = "마감 임박 동행 게시물 조회",
      description = "동행 시작일이 오늘이고 모집 상태가 RECRUITING인 활성 게시물을 "
          + "생성일 기준 오래된 순(createdAt ASC)으로 최대 4개 조회합니다. "
          + "인증이 필요하며 조회 결과가 없어도 빈 목록과 함께 200 응답을 반환합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "마감 임박 게시글 조회 성공. 결과가 없으면 빈 목록 반환")
  })
  @CommonErrorResponses
  @GetMapping("/closing-soon")
  public BaseResponse<ClosingSoonPostResponse> getClosingSoonPosts(
      @Parameter(hidden = true)
      @LoginUser Long userId
  ) {
    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ClosingSoonPostResponse.from(postService.getClosingSoonPosts(userId))
    );
  }

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

  @Operation(summary = "게시글 상세 조회", description = "동행 모집 게시글의 상세 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
  })
  @CommonErrorResponses
  @GetMapping("/{postId}")
  public BaseResponse<PostDetailResponse> getPostDetail(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "조회할 게시글 ID", example = "1")
      @PathVariable @Positive Long postId
  ) {
    return BaseResponse.success(
        PostSuccessCode.POST_DETAIL_FOUND,
        PostDetailResponse.from(postService.getPostDetail(userId, postId))
    );
  }

  @Operation(summary = "모집 상태 변경", description = "게시글 작성자가 모집 상태를 변경합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "변경 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "403", description = "게시글 작성자가 아님"),
      @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PatchMapping("/{postId}/status")
  public BaseResponse<UpdatePostStatusResponse> updatePostStatus(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "모집 상태를 변경할 게시글 ID", example = "1")
      @PathVariable Long postId,
      @RequestBody @Valid UpdatePostStatusRequest request
  ) {
    return BaseResponse.success(
        PostSuccessCode.POST_STATUS_UPDATED,
        UpdatePostStatusResponse.from(postService.updatePostStatus(userId, postId, request.status()))
    );
  }

  @Operation(summary = "동행 게시글 수정", description = "작성자가 전달한 필드만 부분 수정합니다. 모집 중과 모집 완료 게시글 모두 수정할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "수정 성공",
          content = @Content(schema = @Schema(implementation = UpdatePostSuccessResponse.class))
      ),
      @ApiResponse(responseCode = "403", description = "게시글 작성자가 아님"),
      @ApiResponse(responseCode = "404", description = "국가, 도시, 태그 또는 게시글을 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PatchMapping("/{postId}")
  public BaseResponse<UpdatePostResponse> updatePost(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "수정할 게시글 ID", example = "1", required = true)
      @PathVariable @Positive Long postId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          description = "게시글 부분 수정 요청. 하나 이상의 필드를 전달해야 합니다."
      )
      @RequestBody UpdatePostRequest request
  ) {
    return BaseResponse.success(
        PostSuccessCode.POST_UPDATED,
        UpdatePostResponse.from(postService.updatePost(userId, postId, request.toCommand()))
    );
  }

  @Operation(summary = "동행 게시글 삭제", description = "작성자의 게시글을 소프트 삭제합니다. 연관 데이터와 이미지 파일은 삭제하지 않습니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "삭제 성공",
          content = @Content(schema = @Schema(implementation = DeletePostSuccessResponse.class))
      ),
      @ApiResponse(responseCode = "403", description = "게시글 작성자가 아님"),
      @ApiResponse(responseCode = "404", description = "게시글이 존재하지 않거나 이미 삭제됨")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @DeleteMapping("/{postId}")
  public BaseResponse<DeletePostResponse> deletePost(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "삭제할 게시글 ID", example = "1", required = true)
      @PathVariable @Positive Long postId
  ) {
    return BaseResponse.success(
        PostSuccessCode.POST_DELETED,
        DeletePostResponse.from(postService.deletePost(userId, postId))
    );
  }

  @Operation(summary = "동행 게시글 저장", description = "로그인한 사용자가 동행 게시글을 저장합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "저장 성공",
          content = @Content(schema = @Schema(implementation = PostBookmarkSuccessResponse.class))
      ),
      @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PostMapping("/{postId}/bookmarks")
  public BaseResponse<PostBookmarkResponse> bookmarkPost(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "저장할 게시글 ID", example = "1", required = true)
      @PathVariable @Positive Long postId
  ) {
    return BaseResponse.success(
        PostSuccessCode.POST_BOOKMARKED,
        PostBookmarkResponse.from(postService.bookmarkPost(userId, postId))
    );
  }

  @Operation(summary = "동행 게시글 저장 취소", description = "로그인한 사용자가 저장한 동행 게시글을 저장 취소합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "저장 취소 성공",
          content = @Content(schema = @Schema(implementation = DeletePostBookmarkSuccessResponse.class))
      ),
      @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @DeleteMapping("/{postId}/bookmarks")
  public BaseResponse<PostBookmarkResponse> removePostBookmark(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "저장 취소할 게시글 ID", example = "1", required = true)
      @PathVariable @Positive Long postId
  ) {
    return BaseResponse.success(
        PostSuccessCode.POST_BOOKMARK_REMOVED,
        PostBookmarkResponse.from(postService.removePostBookmark(userId, postId))
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
        request.genderConditions(),
        request.companionType(),
        request.recruitmentCountType(),
        request.tagIds(),
        request.imageUrls()
    );
  }
}
