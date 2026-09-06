package org.sopt.buddys.domain.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.comment.code.CommentSuccessCode;
import org.sopt.buddys.domain.comment.dto.request.CreateCommentRequest;
import org.sopt.buddys.domain.comment.dto.response.CourseCommentListResponse;
import org.sopt.buddys.domain.comment.dto.response.CreateCourseCommentResponse;
import org.sopt.buddys.domain.comment.service.CourseCommentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.sopt.buddys.global.common.PageConstants.MAX_PAGE_SIZE;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses/{courseId}/comments")
@Tag(name = "Course Comment", description = "코스 댓글 API")
public class CourseCommentController {

  private final CourseCommentService courseCommentService;

  @Operation(summary = "코스 댓글 목록 조회", description = "코스에 작성된 댓글 목록을 작성 시간 오름차순으로 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<CourseCommentListResponse> getComments(
      @Parameter(description = "댓글 목록을 조회할 코스 ID", example = "1")
      @PathVariable @Positive Long courseId,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size
  ) {
    return BaseResponse.success(
        CommentSuccessCode.COMMENT_LIST_FOUND,
        CourseCommentListResponse.from(courseCommentService.getComments(courseId, page, size))
    );
  }

  @Operation(summary = "코스 댓글 작성", description = "로그인한 사용자가 코스에 댓글을 작성합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "작성 성공"),
      @ApiResponse(responseCode = "401", description = "인증 필요"),
      @ApiResponse(responseCode = "404", description = "코스 또는 사용자를 찾을 수 없음")
  })
  @InvalidRequestResponse
  @CommonErrorResponses
  @PostMapping
  public ResponseEntity<BaseResponse<CreateCourseCommentResponse>> createComment(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "댓글을 작성할 코스 ID", example = "1")
      @PathVariable @Positive Long courseId,
      @RequestBody @Valid CreateCommentRequest request
  ) {
    return ResponseEntity
        .status(CommentSuccessCode.COMMENT_CREATED.getHttpStatus())
        .body(BaseResponse.success(
            CommentSuccessCode.COMMENT_CREATED,
            CreateCourseCommentResponse.from(courseCommentService.createComment(userId, courseId, request.content()))
        ));
  }
}
