package org.sopt.buddys.domain.course.controller;

import org.sopt.buddys.domain.course.dto.request.CourseDayRequest;
import org.sopt.buddys.domain.course.dto.request.CourseFlightRequest;
import org.sopt.buddys.domain.course.dto.request.CoursePlaceRequest;
import org.sopt.buddys.domain.course.dto.request.CreateCourseRequest;
import org.sopt.buddys.domain.course.dto.request.UpdateCourseRequest;
import org.sopt.buddys.domain.course.service.command.CourseDayCommand;
import org.sopt.buddys.domain.course.service.command.CourseFlightCommand;
import org.sopt.buddys.domain.course.service.command.CoursePlaceCommand;
import org.sopt.buddys.domain.course.service.command.CreateCourseCommand;
import org.sopt.buddys.domain.course.service.command.UpdateCourseCommand;

final class CourseCommandMapper {

  private CourseCommandMapper() {
  }

  static CreateCourseCommand toCommand(CreateCourseRequest request) {
    return new CreateCourseCommand(
        request.countryIds(),
        request.cityIds(),
        request.title(),
        request.content(),
        request.startDate(),
        request.endDate(),
        request.tagIds(),
        request.companionUserIds(),
        request.days() == null ? null : request.days().stream().map(CourseCommandMapper::toCommand).toList()
    );
  }

  static UpdateCourseCommand toCommand(UpdateCourseRequest request) {
    return new UpdateCourseCommand(
        request.countryIds(),
        request.cityIds(),
        request.title(),
        request.content(),
        request.startDate(),
        request.endDate(),
        request.tagIds(),
        request.days() == null ? null : request.days().stream().map(CourseCommandMapper::toCommand).toList()
    );
  }

  private static CourseDayCommand toCommand(CourseDayRequest request) {
    return new CourseDayCommand(
        request.dayNumber(),
        request.date(),
        request.imageUrls(),
        request.memo(),
        request.cost(),
        request.places() == null ? null : request.places().stream().map(CourseCommandMapper::toCommand).toList(),
        request.flights() == null ? null : request.flights().stream().map(CourseCommandMapper::toCommand).toList()
    );
  }

  private static CoursePlaceCommand toCommand(CoursePlaceRequest request) {
    return new CoursePlaceCommand(
        request.googlePlaceId(),
        request.name(),
        request.category(),
        request.latitude(),
        request.longitude(),
        request.orderNo()
    );
  }

  private static CourseFlightCommand toCommand(CourseFlightRequest request) {
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
