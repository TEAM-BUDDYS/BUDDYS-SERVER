package org.sopt.buddys.domain.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.comment.code.CommentSuccessCode;
import org.sopt.buddys.domain.comment.dto.request.CreateCommentRequest;
import org.sopt.buddys.domain.comment.dto.response.CommentListResponse;
import org.sopt.buddys.domain.comment.dto.response.CreateCommentResponse;
import org.sopt.buddys.domain.comment.service.CommentService;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/comments")
@Tag(name = "Comment", description = "댓글 API")
public class CommentController {

  private final CommentService commentService;

  @Operation(summary = "댓글 목록 조회", description = "게시글에 작성된 댓글 목록을 작성 시간 오름차순으로 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음")
  })
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<CommentListResponse> getComments(
      @Parameter(description = "댓글 목록을 조회할 게시글 ID", example = "1")
      @PathVariable Long postId
  ) {
    return BaseResponse.success(
        CommentSuccessCode.COMMENT_LIST_FOUND,
        commentService.getComments(postId)
    );
  }

  @Operation(summary = "댓글 작성", description = "로그인한 사용자가 게시글에 댓글을 작성합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "작성 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "게시글 또는 사용자를 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PostMapping
  public ResponseEntity<BaseResponse<CreateCommentResponse>> createComment(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "댓글을 작성할 게시글 ID", example = "1")
      @PathVariable Long postId,
      @RequestBody @Valid CreateCommentRequest request
  ) {
    return ResponseEntity
        .status(CommentSuccessCode.COMMENT_CREATED.getHttpStatus())
        .body(BaseResponse.success(
            CommentSuccessCode.COMMENT_CREATED,
            CreateCommentResponse.from(commentService.createComment(userId, postId, request.content()))
        ));
  }
}
