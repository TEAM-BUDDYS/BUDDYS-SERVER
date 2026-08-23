package org.sopt.buddys.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.dto.request.CourseDayRequest;
import org.sopt.buddys.domain.course.dto.request.CourseFlightRequest;
import org.sopt.buddys.domain.course.dto.request.CoursePlaceRequest;
import org.sopt.buddys.domain.course.dto.request.CreateCourseRequest;
import org.sopt.buddys.domain.course.dto.response.CreateCourseResponse;
import org.sopt.buddys.domain.course.service.CourseService;
import org.sopt.buddys.domain.course.service.command.CourseDayCommand;
import org.sopt.buddys.domain.course.service.command.CourseFlightCommand;
import org.sopt.buddys.domain.course.service.command.CoursePlaceCommand;
import org.sopt.buddys.domain.course.service.command.CreateCourseCommand;
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
@RequestMapping("/api/v1/courses")
@Tag(name = "Course", description = "코스 API")
public class CourseController {

  private final CourseService courseService;

  @Operation(summary = "코스 게시글 작성", description = "로그인한 사용자가 여행 코스 게시글을 작성합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "작성 성공"),
      @ApiResponse(responseCode = "404", description = "국가, 도시, 태그 또는 사용자를 찾을 수 없음")
  })
  @InvalidRequestResponse
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

  private CreateCourseCommand toCommand(CreateCourseRequest request) {
    return new CreateCourseCommand(
        request.countryId(),
        request.cityId(),
        request.title(),
        request.content(),
        request.startDate(),
        request.endDate(),
        request.tagIds(),
        request.companionUserIds(),
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
