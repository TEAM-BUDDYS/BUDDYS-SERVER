package org.sopt.buddys.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.code.CourseSuccessCode;
import org.sopt.buddys.domain.course.dto.request.CourseDayRequest;
import org.sopt.buddys.domain.course.dto.request.CourseFlightRequest;
import org.sopt.buddys.domain.course.dto.request.CourseListRequest;
import org.sopt.buddys.domain.course.dto.request.CoursePlaceRequest;
import org.sopt.buddys.domain.course.dto.request.CreateCourseRequest;
import org.sopt.buddys.domain.course.dto.request.UpdateCourseRequest;
import org.sopt.buddys.domain.course.dto.response.CourseBookmarkResponse;
import org.sopt.buddys.domain.course.dto.response.CourseDetailResponse;
import org.sopt.buddys.domain.course.dto.response.CourseListResponse;
import org.sopt.buddys.domain.course.dto.response.CreateCourseResponse;
import org.sopt.buddys.domain.course.dto.response.UpdateCourseResponse;
import org.sopt.buddys.domain.course.service.CourseService;
import org.sopt.buddys.domain.course.service.command.CourseDayCommand;
import org.sopt.buddys.domain.course.service.command.CourseFlightCommand;
import org.sopt.buddys.domain.course.service.command.CoursePlaceCommand;
import org.sopt.buddys.domain.course.service.command.CreateCourseCommand;
import org.sopt.buddys.domain.course.service.command.UpdateCourseCommand;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.sopt.buddys.global.common.PageConstants.MAX_PAGE_SIZE;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
@Tag(name = "Course", description = "코스 API")
public class CourseController {

  private final CourseService courseService;

  @Operation(summary = "코스 목록 조회", description = "여행 코스 게시글 목록을 국가와 태그로 필터링하여 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공")
  })
  @CommonErrorResponses
  @GetMapping
  public BaseResponse<CourseListResponse> getCourses(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @ParameterObject @Valid @ModelAttribute CourseListRequest request
  ) {
    return BaseResponse.success(
        CourseSuccessCode.COURSE_LIST_FOUND,
        CourseListResponse.from(
            courseService.getCourses(userId, request.toCondition(), request.pageOrDefault(), request.sizeOrDefault()))
    );
  }

  @Operation(summary = "저장한 코스 목록 조회", description = "로그인한 사용자가 저장한 코스 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공")
  })
  @CommonErrorResponses
  @GetMapping("/bookmarks")
  public BaseResponse<CourseListResponse> getBookmarkedCourses(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size
  ) {
    return BaseResponse.success(
        CourseSuccessCode.COURSE_BOOKMARK_LIST_FOUND,
        CourseListResponse.from(courseService.getBookmarkedCourses(userId, page, size))
    );
  }

  @Operation(summary = "코스 게시글 작성", description = "로그인한 사용자가 여행 코스 게시글을 작성합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "작성 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BaseResponse.class),
              examples = {
                  @ExampleObject(
                      name = "잘못된 요청",
                      value = """
                          {
                            "success": false,
                            "code": "GLB-E001",
                            "message": "잘못된 요청입니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "일자(dayNumber) 중복",
                      value = """
                          {
                            "success": false,
                            "code": "COURSE-E004",
                            "message": "일자(dayNumber)가 중복되었습니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "활동 태그 누락",
                      value = """
                          {
                            "success": false,
                            "code": "COURSE-E006",
                            "message": "활동 태그를 하나 이상 선택해야 합니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "태그 개수 초과",
                      value = """
                          {
                            "success": false,
                            "code": "COURSE-E007",
                            "message": "태그 선택 개수를 초과했습니다.",
                            "data": null
                          }
                          """
                  )
              }
          )
      ),
      @ApiResponse(responseCode = "404", description = "국가, 도시, 태그 또는 사용자를 찾을 수 없음")
  })
  @CommonErrorResponses
  @PostMapping
  public ResponseEntity<BaseResponse<CreateCourseResponse>> createCourse(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid CreateCourseRequest request
  ) {
    return ResponseEntity
        .status(GlobalSuccessCode.CREATED.getHttpStatus())
        .body(BaseResponse.success(
            GlobalSuccessCode.CREATED,
            CreateCourseResponse.from(courseService.createCourse(userId, toCommand(request)))
        ));
  }

  @Operation(summary = "코스 수정", description = "코스 작성자가 코스 정보를 수정합니다. 요청 본문으로 국가/도시/날짜/제목/내용/태그/일자별 사진·장소·메모·비용/항공편 정보 전체를 대체합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "수정 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BaseResponse.class),
              examples = {
                  @ExampleObject(
                      name = "잘못된 요청",
                      value = """
                          {
                            "success": false,
                            "code": "GLB-E001",
                            "message": "잘못된 요청입니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "일자(dayNumber) 중복",
                      value = """
                          {
                            "success": false,
                            "code": "COURSE-E004",
                            "message": "일자(dayNumber)가 중복되었습니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "활동 태그 누락",
                      value = """
                          {
                            "success": false,
                            "code": "COURSE-E006",
                            "message": "활동 태그를 하나 이상 선택해야 합니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "태그 개수 초과",
                      value = """
                          {
                            "success": false,
                            "code": "COURSE-E007",
                            "message": "태그 선택 개수를 초과했습니다.",
                            "data": null
                          }
                          """
                  )
              }
          )
      ),
      @ApiResponse(responseCode = "403", description = "코스 작성자가 아님"),
      @ApiResponse(responseCode = "404", description = "코스, 국가, 도시 또는 태그를 찾을 수 없음")
  })
  @CommonErrorResponses
  @PutMapping("/{courseId}")
  public BaseResponse<UpdateCourseResponse> updateCourse(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "수정할 코스 ID", example = "1")
      @PathVariable @Positive Long courseId,
      @RequestBody @Valid UpdateCourseRequest request
  ) {
    return BaseResponse.success(
        CourseSuccessCode.COURSE_UPDATED,
        UpdateCourseResponse.from(courseService.updateCourse(userId, courseId, toCommand(request)))
    );
  }

  @Operation(summary = "코스 상세 조회", description = "여행 코스 게시글의 상세 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
  })
  @CommonErrorResponses
  @GetMapping("/{courseId}")
  public BaseResponse<CourseDetailResponse> getCourseDetail(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "조회할 코스 ID", example = "1")
      @PathVariable @Positive Long courseId
  ) {
    return BaseResponse.success(
        CourseSuccessCode.COURSE_DETAIL_FOUND,
        CourseDetailResponse.from(courseService.getCourseDetail(userId, courseId))
    );
  }

  @Operation(summary = "코스 삭제", description = "코스 작성자가 코스를 삭제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "삭제 성공"),
      @ApiResponse(responseCode = "403", description = "코스 작성자가 아님"),
      @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
  })
  @CommonErrorResponses
  @DeleteMapping("/{courseId}")
  public BaseResponse<Void> deleteCourse(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "삭제할 코스 ID", example = "1")
      @PathVariable @Positive Long courseId
  ) {
    courseService.deleteCourse(userId, courseId);
    return BaseResponse.success(GlobalSuccessCode.OK);
  }

  @Operation(summary = "코스 저장", description = "로그인한 사용자가 코스를 저장합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "저장 성공"),
      @ApiResponse(responseCode = "404", description = "존재하지 않거나 삭제된 코스")
  })
  @CommonErrorResponses
  @PostMapping("/{courseId}/bookmark")
  public BaseResponse<CourseBookmarkResponse> bookmarkCourse(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "저장할 코스 ID", example = "1")
      @PathVariable @Positive Long courseId
  ) {
    courseService.bookmarkCourse(userId, courseId);
    return BaseResponse.success(
        CourseSuccessCode.COURSE_BOOKMARKED,
        new CourseBookmarkResponse(courseId, true)
    );
  }

  @Operation(summary = "코스 저장 취소", description = "로그인한 사용자가 코스 저장을 취소합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "저장 취소 성공"),
      @ApiResponse(responseCode = "404", description = "존재하지 않거나 삭제된 코스")
  })
  @CommonErrorResponses
  @DeleteMapping("/{courseId}/bookmark")
  public BaseResponse<CourseBookmarkResponse> unbookmarkCourse(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "저장 취소할 코스 ID", example = "1")
      @PathVariable @Positive Long courseId
  ) {
    courseService.unbookmarkCourse(userId, courseId);
    return BaseResponse.success(
        CourseSuccessCode.COURSE_BOOKMARK_CANCELLED,
        new CourseBookmarkResponse(courseId, false)
    );
  }

  private CreateCourseCommand toCommand(CreateCourseRequest request) {
    return new CreateCourseCommand(
        request.countryIds(),
        request.cityIds(),
        request.title(),
        request.content(),
        request.thumbnailImageUrl(),
        request.startDate(),
        request.endDate(),
        request.tagIds(),
        request.companionUserIds(),
        request.days() == null ? null : request.days().stream().map(this::toCommand).toList(),
        request.flights() == null ? null : request.flights().stream().map(this::toCommand).toList()
    );
  }

  private UpdateCourseCommand toCommand(UpdateCourseRequest request) {
    return new UpdateCourseCommand(
        request.countryIds(),
        request.cityIds(),
        request.title(),
        request.content(),
        request.thumbnailImageUrl(),
        request.startDate(),
        request.endDate(),
        request.tagIds(),
        request.days() == null ? null : request.days().stream().map(this::toCommand).toList(),
        request.flights() == null ? null : request.flights().stream().map(this::toCommand).toList()
    );
  }

  private CourseDayCommand toCommand(CourseDayRequest request) {
    return new CourseDayCommand(
        request.dayNumber(),
        request.date(),
        request.imageUrls(),
        request.places() == null ? null : request.places().stream().map(this::toCommand).toList()
    );
  }

  private CoursePlaceCommand toCommand(CoursePlaceRequest request) {
    return new CoursePlaceCommand(
        request.googlePlaceId(),
        request.name(),
        request.category(),
        request.latitude(),
        request.longitude(),
        request.orderNo(),
        request.memo(),
        request.cost()
    );
  }

  private CourseFlightCommand toCommand(CourseFlightRequest request) {
    return new CourseFlightCommand(
        request.airline(),
        request.flightNumber(),
        request.departureAirport(),
        request.departureAt(),
        request.arrivalAirport(),
        request.arrivalAt()
    );
  }
}
