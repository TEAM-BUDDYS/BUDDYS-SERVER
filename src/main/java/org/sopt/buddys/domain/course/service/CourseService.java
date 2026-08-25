package org.sopt.buddys.domain.course.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.code.CourseErrorCode;
import org.sopt.buddys.domain.course.entity.Course;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.entity.CourseBookmarkId;
import org.sopt.buddys.domain.course.entity.CourseCompanion;
import org.sopt.buddys.domain.course.entity.CourseDay;
import org.sopt.buddys.domain.course.entity.CourseFlight;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.sopt.buddys.domain.course.entity.CoursePlace;
import org.sopt.buddys.domain.course.entity.CourseTag;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
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
import org.sopt.buddys.domain.course.service.result.CourseDetailResult;
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
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

  private static final int MAX_ACTIVITY_TAG_COUNT = 3;
  private static final int MAX_INTEREST_TAG_COUNT = 2;
  private static final int MAX_TRAVEL_STYLE_TAG_COUNT = 2;

  private final CourseRepository courseRepository;
  private final CourseTagRepository courseTagRepository;
  private final CourseDayRepository courseDayRepository;
  private final CourseImageRepository courseImageRepository;
  private final CoursePlaceRepository coursePlaceRepository;
  private final CourseCompanionRepository courseCompanionRepository;
  private final CourseFlightRepository courseFlightRepository;
  private final CourseBookmarkRepository courseBookmarkRepository;
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

  @Transactional
  public CourseDetailResult getCourseDetail(Long userId, Long courseId) {
    if (courseRepository.increaseViewCount(courseId) == 0) {
      throw new BaseException(CourseErrorCode.COURSE_NOT_FOUND);
    }

    Course course = courseRepository.findDetailById(courseId)
        .orElseThrow(() -> new BaseException(CourseErrorCode.COURSE_NOT_FOUND));

    return toCourseDetailResult(userId, course);
  }

  @Transactional
  public void deleteCourse(Long userId, Long courseId) {
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new BaseException(CourseErrorCode.COURSE_NOT_FOUND));

    if (!course.getAuthor().getId().equals(userId)) {
      throw new BaseException(GlobalErrorCode.FORBIDDEN);
    }

    course.delete();
  }

  @Transactional
  public void bookmarkCourse(Long userId, Long courseId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new BaseException(CourseErrorCode.COURSE_NOT_FOUND));

    try {
      courseBookmarkRepository.saveAndFlush(new CourseBookmark(user, course));
    } catch (DataIntegrityViolationException e) {
    }
  }

  @Transactional
  public void unbookmarkCourse(Long userId, Long courseId) {
    if (!courseRepository.existsByIdAndDeletedAtIsNull(courseId)) {
      throw new BaseException(CourseErrorCode.COURSE_NOT_FOUND);
    }
    courseBookmarkRepository.deleteById(new CourseBookmarkId(userId, courseId));
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
        || command.tagIds() == null || command.tagIds().isEmpty()
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
    Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
    List<Tag> tags = tagRepository.findAllById(distinctTagIds);
    if (tags.size() != distinctTagIds.size()) {
      throw new BaseException(CourseErrorCode.TAG_NOT_FOUND);
    }
    validateTagTypeCounts(tags);

    courseTagRepository.saveAll(tags.stream()
        .map(tag -> new CourseTag(course, tag))
        .toList());
  }

  private void validateTagTypeCounts(List<Tag> tags) {
    long activityTagCount = countTagsByType(tags, TagType.ACTIVITY);
    if (activityTagCount == 0) {
      throw new BaseException(CourseErrorCode.ACTIVITY_TAG_REQUIRED);
    }
    if (activityTagCount > MAX_ACTIVITY_TAG_COUNT
        || countTagsByType(tags, TagType.INTEREST) > MAX_INTEREST_TAG_COUNT
        || countTagsByType(tags, TagType.TRAVEL_STYLE) > MAX_TRAVEL_STYLE_TAG_COUNT) {
      throw new BaseException(CourseErrorCode.TAG_LIMIT_EXCEEDED);
    }
  }

  private long countTagsByType(List<Tag> tags, TagType tagType) {
    return tags.stream()
        .filter(tag -> tag.getTagType() == tagType)
        .count();
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
    List<CourseDay> courseDays = days.stream()
        .map(dayCommand -> courseDayRepository.save(
            new CourseDay(course, dayCommand.dayNumber(), dayCommand.date())))
        .toList();

    for (int i = 0; i < days.size(); i++) {
      saveCourseImages(courseDays.get(i), days.get(i).imageUrls());
    }

    saveCoursePlaces(courseDays, days);
  }

  private void saveCourseImages(CourseDay courseDay, List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }
    courseImageRepository.saveAll(IntStream.range(0, imageUrls.size())
        .mapToObj(index -> new CourseImage(courseDay, imageUrls.get(index).trim(), (short) index))
        .toList());
  }

  private void saveCoursePlaces(List<CourseDay> courseDays, List<CourseDayCommand> days) {
    Map<String, Place> placesByGooglePlaceId = resolvePlaces(days);
    if (placesByGooglePlaceId.isEmpty()) {
      return;
    }

    List<CoursePlace> coursePlaces = new ArrayList<>();
    for (int i = 0; i < days.size(); i++) {
      List<CoursePlaceCommand> placeCommands = days.get(i).places();
      if (placeCommands == null || placeCommands.isEmpty()) {
        continue;
      }
      CourseDay courseDay = courseDays.get(i);
      for (int index = 0; index < placeCommands.size(); index++) {
        CoursePlaceCommand placeCommand = placeCommands.get(index);
        Place place = placesByGooglePlaceId.get(placeCommand.googlePlaceId());
        Short orderNo = placeCommand.orderNo() != null ? placeCommand.orderNo() : (short) index;
        coursePlaces.add(new CoursePlace(courseDay, place, orderNo, placeCommand.memo(), placeCommand.cost()));
      }
    }
    coursePlaceRepository.saveAll(coursePlaces);
  }

  private Map<String, Place> resolvePlaces(List<CourseDayCommand> days) {
    List<CoursePlaceCommand> allPlaceCommands = days.stream()
        .filter(day -> day.places() != null)
        .flatMap(day -> day.places().stream())
        .toList();
    if (allPlaceCommands.isEmpty()) {
      return Map.of();
    }

    allPlaceCommands.forEach(command -> parseCategory(command.category()));

    Map<String, CoursePlaceCommand> firstCommandByGooglePlaceId = new LinkedHashMap<>();
    allPlaceCommands.forEach(command ->
        firstCommandByGooglePlaceId.putIfAbsent(command.googlePlaceId(), command));

    Map<String, Place> resolvedPlaces = new LinkedHashMap<>();
    placeRepository.findByGooglePlaceIdIn(firstCommandByGooglePlaceId.keySet())
        .forEach(place -> resolvedPlaces.put(place.getGooglePlaceId(), place));

    firstCommandByGooglePlaceId.forEach((googlePlaceId, command) -> {
      PlaceCategory category = parseCategory(command.category());
      Place existingPlace = resolvedPlaces.get(googlePlaceId);
      if (existingPlace != null) {
        existingPlace.updateFromCourse(command.name(), category, command.latitude(), command.longitude());
      } else {
        resolvedPlaces.put(googlePlaceId, createPlace(command, category));
      }
    });

    return resolvedPlaces;
  }

  private Place createPlace(CoursePlaceCommand placeCommand, PlaceCategory category) {
    try {
      return placeRepository.save(Place.builder()
          .googlePlaceId(placeCommand.googlePlaceId())
          .name(placeCommand.name())
          .category(category)
          .latitude(placeCommand.latitude())
          .longitude(placeCommand.longitude())
          .build());
    } catch (DataIntegrityViolationException e) {
      return placeRepository.findByGooglePlaceId(placeCommand.googlePlaceId())
          .orElseThrow(() -> e);
    }
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

  private CourseDetailResult toCourseDetailResult(Long userId, Course course) {
    return new CourseDetailResult(
        course.getId(),
        toAuthorResult(course.getAuthor()),
        course.getAuthor().getId().equals(userId),
        course.getTitle(),
        course.getContent(),
        toCourseCountryResult(course.getCountry()),
        toCourseCityResult(course.getCity()),
        course.getStartDate(),
        course.getEndDate(),
        getCourseTagResults(course.getId()),
        getCompanionResults(course.getId()),
        getFlightResults(course.getId()),
        getDayResults(course.getId()),
        course.getViewCount(),
        course.getCreatedAt()
    );
  }

  private CourseDetailResult.AuthorResult toAuthorResult(User author) {
    Country exchangeCountry = author.getExchangeCountry();
    return new CourseDetailResult.AuthorResult(
        author.getId(),
        author.getNickname(),
        author.getProfileImageUrl(),
        exchangeCountry == null ? null : exchangeCountry.getName(),
        toAge(author.getBirthDate()),
        toAgeRange(author.getBirthDate()),
        author.getGender()
    );
  }

  private CourseDetailResult.CountryResult toCourseCountryResult(Country country) {
    return new CourseDetailResult.CountryResult(country.getId(), country.getName());
  }

  private CourseDetailResult.CityResult toCourseCityResult(City city) {
    return new CourseDetailResult.CityResult(city.getId(), city.getName(), getCityKoreanName(city));
  }

  private String getCityKoreanName(City city) {
    if (city.getKoreanName() == null || city.getKoreanName().isBlank()) {
      return city.getName();
    }
    return city.getKoreanName();
  }

  private List<CourseDetailResult.TagResult> getCourseTagResults(Long courseId) {
    return courseTagRepository.findAllByCourseIdWithTag(courseId)
        .stream()
        .map(courseTag -> new CourseDetailResult.TagResult(
            courseTag.getTag().getId(),
            courseTag.getTag().getName(),
            courseTag.getTag().getTagType()
        ))
        .toList();
  }

  private List<CourseDetailResult.CompanionResult> getCompanionResults(Long courseId) {
    return courseCompanionRepository.findAllByCourseIdWithUser(courseId)
        .stream()
        .map(companion -> new CourseDetailResult.CompanionResult(
            companion.getUser().getId(),
            companion.getUser().getNickname(),
            companion.getUser().getProfileImageUrl()
        ))
        .toList();
  }

  private List<CourseDetailResult.FlightResult> getFlightResults(Long courseId) {
    return courseFlightRepository.findAllByCourseIdOrderByOrderNoAsc(courseId)
        .stream()
        .map(flight -> new CourseDetailResult.FlightResult(
            flight.getAirline(),
            flight.getFlightNumber(),
            flight.getDepartureAirport(),
            flight.getDepartureAt(),
            flight.getArrivalAirport(),
            flight.getArrivalAt()
        ))
        .toList();
  }

  private List<CourseDetailResult.DayResult> getDayResults(Long courseId) {
    List<CourseDay> days = courseDayRepository.findAllByCourseIdOrderByDayNumberAsc(courseId);
    List<Long> dayIds = days.stream().map(CourseDay::getId).toList();

    Map<Long, List<String>> imageUrlsByDayId = groupImageUrlsByDayId(dayIds);
    Map<Long, List<CourseDetailResult.PlaceResult>> placesByDayId = groupPlacesByDayId(dayIds);

    return days.stream()
        .map(day -> new CourseDetailResult.DayResult(
            day.getDayNumber(),
            day.getDate(),
            imageUrlsByDayId.getOrDefault(day.getId(), List.of()),
            placesByDayId.getOrDefault(day.getId(), List.of())
        ))
        .toList();
  }

  private Map<Long, List<String>> groupImageUrlsByDayId(List<Long> dayIds) {
    if (dayIds.isEmpty()) {
      return Map.of();
    }
    return courseImageRepository.findAllByCourseDayIdIn(dayIds)
        .stream()
        .collect(Collectors.groupingBy(
            image -> image.getCourseDay().getId(),
            LinkedHashMap::new,
            Collectors.mapping(CourseImage::getImageUrl, Collectors.toList())
        ));
  }

  private Map<Long, List<CourseDetailResult.PlaceResult>> groupPlacesByDayId(List<Long> dayIds) {
    if (dayIds.isEmpty()) {
      return Map.of();
    }
    return coursePlaceRepository.findAllByCourseDayIdInWithPlace(dayIds)
        .stream()
        .collect(Collectors.groupingBy(
            coursePlace -> coursePlace.getCourseDay().getId(),
            LinkedHashMap::new,
            Collectors.mapping(this::toCoursePlaceResult, Collectors.toList())
        ));
  }

  private CourseDetailResult.PlaceResult toCoursePlaceResult(CoursePlace coursePlace) {
    Place place = coursePlace.getPlace();
    return new CourseDetailResult.PlaceResult(
        place.getId(),
        place.getGooglePlaceId(),
        place.getName(),
        place.getCategory(),
        place.getLatitude(),
        place.getLongitude(),
        coursePlace.getMemo(),
        coursePlace.getCost()
    );
  }

  private String toAgeRange(LocalDate birthDate) {
    Integer age = toAge(birthDate);
    if (age == null) {
      return null;
    }
    if (age < 10) {
      return "10대 미만";
    }
    return "%d0대".formatted(age / 10);
  }

  private Integer toAge(LocalDate birthDate) {
    if (birthDate == null) {
      return null;
    }
    return Period.between(birthDate, LocalDate.now()).getYears();
  }
}
