package org.sopt.buddys.domain.course.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.code.CourseErrorCode;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseCompanion;
import org.sopt.buddys.domain.course.entity.CourseDay;
import org.sopt.buddys.domain.course.entity.CourseFlight;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.sopt.buddys.domain.course.entity.CoursePlace;
import org.sopt.buddys.domain.course.entity.CourseTag;
import org.sopt.buddys.domain.course.repository.CourseCompanionRepository;
import org.sopt.buddys.domain.course.repository.CourseDayRepository;
import org.sopt.buddys.domain.course.repository.CourseFlightRepository;
import org.sopt.buddys.domain.course.repository.CourseImageRepository;
import org.sopt.buddys.domain.course.repository.CoursePlaceRepository;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.course.repository.CourseTagRepository;
import org.sopt.buddys.domain.course.service.command.CourseDayCommand;
import org.sopt.buddys.domain.course.service.command.CourseFlightCommand;
import org.sopt.buddys.domain.course.service.command.CoursePlaceCommand;
import org.sopt.buddys.domain.course.service.command.CreateCourseCommand;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

  private final CourseRepository courseRepository;
  private final CourseTagRepository courseTagRepository;
  private final CourseDayRepository courseDayRepository;
  private final CourseImageRepository courseImageRepository;
  private final CoursePlaceRepository coursePlaceRepository;
  private final CourseCompanionRepository courseCompanionRepository;
  private final CourseFlightRepository courseFlightRepository;
  private final UserRepository userRepository;
  private final CountryRepository countryRepository;
  private final CityRepository cityRepository;
  private final TagRepository tagRepository;
  private final PlaceRepository placeRepository;

  @Transactional
  public Course createCourse(Long userId, CreateCourseCommand command) {
    validateRequiredFields(command);
    validateDateRanges(command);
    validateDayNumbersUnique(command.days());

    User author = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    Country country = countryRepository.findById(command.countryId())
        .orElseThrow(() -> new BaseException(LocationErrorCode.COUNTRY_NOT_FOUND));
    City city = getCity(command.countryId(), command.cityId());

    Course course = courseRepository.save(new Course(
        author,
        country,
        city,
        command.title().trim(),
        command.content() != null ? command.content().trim() : null,
        command.startDate(),
        command.endDate()
    ));

    saveCourseTags(course, command.tagIds());
    saveCourseCompanions(course, command.companionUserIds());
    saveCourseDays(course, command.days());
    saveCourseFlights(course, command.flights());

    return course;
  }

  private City getCity(Long countryId, Long cityId) {
    City city = cityRepository.findById(cityId)
        .orElseThrow(() -> new BaseException(LocationErrorCode.CITY_NOT_FOUND));
    if (!city.getCountry().getId().equals(countryId)) {
      throw new BaseException(CourseErrorCode.CITY_NOT_IN_COUNTRY);
    }
    return city;
  }

  private void validateRequiredFields(CreateCourseCommand command) {
    if (command.countryId() == null
        || command.cityId() == null
        || command.title() == null || command.title().isBlank()
        || command.startDate() == null
        || command.endDate() == null
        || command.days() == null || command.days().isEmpty()) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
    for (CourseDayCommand day : command.days()) {
      if (day.dayNumber() == null) {
        throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
      }
    }
    if (command.flights() == null) {
      return;
    }
    for (CourseFlightCommand flight : command.flights()) {
      if (flight.airline() == null || flight.airline().isBlank()
          || flight.departureAirport() == null || flight.departureAirport().isBlank()
          || flight.departureAt() == null
          || flight.arrivalAirport() == null || flight.arrivalAirport().isBlank()
          || flight.arrivalAt() == null) {
        throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
      }
    }
  }

  private void validateDateRanges(CreateCourseCommand command) {
    if (command.endDate().isBefore(command.startDate())) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
    if (command.flights() == null) {
      return;
    }
    for (CourseFlightCommand flight : command.flights()) {
      if (flight.arrivalAt().isBefore(flight.departureAt())) {
        throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
      }
    }
  }

  private void validateDayNumbersUnique(List<CourseDayCommand> days) {
    Set<Short> dayNumbers = new HashSet<>();
    for (CourseDayCommand day : days) {
      if (!dayNumbers.add(day.dayNumber())) {
        throw new BaseException(CourseErrorCode.DAY_NUMBER_DUPLICATED);
      }
    }
  }

  private void saveCourseTags(Course course, List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }
    Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
    List<Tag> tags = tagRepository.findAllById(distinctTagIds);
    if (tags.size() != distinctTagIds.size()) {
      throw new BaseException(CourseErrorCode.TAG_NOT_FOUND);
    }
    courseTagRepository.saveAll(tags.stream()
        .map(tag -> new CourseTag(course, tag))
        .toList());
  }

  private void saveCourseCompanions(Course course, List<Long> companionUserIds) {
    if (companionUserIds == null || companionUserIds.isEmpty()) {
      return;
    }
    Set<Long> distinctUserIds = new LinkedHashSet<>(companionUserIds);
    List<User> users = userRepository.findAllById(distinctUserIds);
    if (users.size() != distinctUserIds.size()) {
      throw new BaseException(CourseErrorCode.COMPANION_USER_NOT_FOUND);
    }
    courseCompanionRepository.saveAll(users.stream()
        .map(user -> new CourseCompanion(course, user))
        .toList());
  }

  private void saveCourseDays(Course course, List<CourseDayCommand> days) {
    for (CourseDayCommand dayCommand : days) {
      CourseDay courseDay = courseDayRepository.save(
          new CourseDay(course, dayCommand.dayNumber(), dayCommand.date()));
      saveCourseImages(courseDay, dayCommand.imageUrls());
      saveCoursePlaces(courseDay, dayCommand.places());
    }
  }

  private void saveCourseImages(CourseDay courseDay, List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }
    courseImageRepository.saveAll(IntStream.range(0, imageUrls.size())
        .mapToObj(index -> new CourseImage(courseDay, imageUrls.get(index).trim(), (short) index))
        .toList());
  }

  private void saveCoursePlaces(CourseDay courseDay, List<CoursePlaceCommand> places) {
    if (places == null || places.isEmpty()) {
      return;
    }
    List<CoursePlace> coursePlaces = IntStream.range(0, places.size())
        .mapToObj(index -> {
          CoursePlaceCommand placeCommand = places.get(index);
          Place place = resolvePlace(placeCommand);
          Short orderNo = placeCommand.orderNo() != null ? placeCommand.orderNo() : (short) index;
          return new CoursePlace(courseDay, place, orderNo, placeCommand.memo(), placeCommand.cost());
        })
        .toList();
    coursePlaceRepository.saveAll(coursePlaces);
  }

  private Place resolvePlace(CoursePlaceCommand placeCommand) {
    PlaceCategory category = parseCategory(placeCommand.category());
    return placeRepository.findByGooglePlaceId(placeCommand.googlePlaceId())
        .orElseGet(() -> placeRepository.save(Place.builder()
            .googlePlaceId(placeCommand.googlePlaceId())
            .name(placeCommand.name())
            .category(category)
            .latitude(placeCommand.latitude())
            .longitude(placeCommand.longitude())
            .build()));
  }

  private PlaceCategory parseCategory(String categoryRaw) {
    try {
      return PlaceCategory.valueOf(categoryRaw.trim());
    } catch (IllegalArgumentException e) {
      throw new BaseException(PlaceErrorCode.INVALID_CATEGORY);
    }
  }

  private void saveCourseFlights(Course course, List<CourseFlightCommand> flights) {
    if (flights == null || flights.isEmpty()) {
      return;
    }
    List<CourseFlight> courseFlights = IntStream.range(0, flights.size())
        .mapToObj(index -> {
          CourseFlightCommand flightCommand = flights.get(index);
          return new CourseFlight(
              course,
              flightCommand.airline().trim(),
              flightCommand.flightNumber() != null ? flightCommand.flightNumber().trim() : null,
              flightCommand.departureAirport().trim(),
              flightCommand.departureAt(),
              flightCommand.arrivalAirport().trim(),
              flightCommand.arrivalAt(),
              (short) index
          );
        })
        .toList();
    courseFlightRepository.saveAll(courseFlights);
  }
}
