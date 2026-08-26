package org.sopt.buddys.domain.course.service;

import java.time.LocalDate;
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
import org.sopt.buddys.domain.course.entity.CourseCity;
import org.sopt.buddys.domain.course.entity.CourseCompanion;
import org.sopt.buddys.domain.course.entity.CourseCountry;
import org.sopt.buddys.domain.course.entity.CourseDay;
import org.sopt.buddys.domain.course.entity.CourseFlight;
import org.sopt.buddys.domain.course.entity.CourseImage;
import org.sopt.buddys.domain.course.entity.CoursePlace;
import org.sopt.buddys.domain.course.entity.CourseTag;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
import org.sopt.buddys.domain.course.repository.CourseCityRepository;
import org.sopt.buddys.domain.course.repository.CourseCompanionRepository;
import org.sopt.buddys.domain.course.repository.CourseCountryRepository;
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
import org.sopt.buddys.domain.course.service.command.UpdateCourseCommand;
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
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.tag.service.TagTypeCountValidator;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.service.AuthorProfileMapper;
import org.sopt.buddys.domain.user.service.result.AuthorProfile;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

  private final CourseRepository courseRepository;
  private final CourseCountryRepository courseCountryRepository;
  private final CourseCityRepository courseCityRepository;
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
    validateRequiredFields(command.countryIds(), command.cityIds(), command.title(), command.startDate(),
        command.endDate(), command.tagIds(), command.days(), command.flights());
    validateDateRanges(command.startDate(), command.endDate(), command.flights());
    validateDayNumbersUnique(command.days());

    User author = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    Course course = courseRepository.save(new Course(
        author,
        command.title().trim(),
        command.content() != null ? command.content().trim() : null,
        command.thumbnailImageUrl() != null ? command.thumbnailImageUrl().trim() : null,
        command.startDate(),
        command.endDate()
    ));

    saveCourseCountries(course, command.countryIds());
    saveCourseCities(course, command.cityIds());
    saveCourseTags(course, command.tagIds());
    saveCourseCompanions(course, command.companionUserIds());
    saveCourseDays(course, command.days());
    saveCourseFlights(course, command.flights());

    return course;
  }

  @Transactional
  public Course updateCourse(Long userId, Long courseId, UpdateCourseCommand command) {
    Course course = courseRepository.findByIdAndDeletedAtIsNull(courseId)
        .orElseThrow(() -> new BaseException(CourseErrorCode.COURSE_NOT_FOUND));

    if (!course.getAuthor().getId().equals(userId)) {
      throw new BaseException(GlobalErrorCode.FORBIDDEN);
    }

    validateRequiredFields(command.countryIds(), command.cityIds(), command.title(), command.startDate(),
        command.endDate(), command.tagIds(), command.days(), command.flights());
    validateDateRanges(command.startDate(), command.endDate(), command.flights());
    validateDayNumbersUnique(command.days());

    course.update(
        command.title().trim(),
        command.content() != null ? command.content().trim() : null,
        command.thumbnailImageUrl() != null ? command.thumbnailImageUrl().trim() : null,
        command.startDate(),
        command.endDate()
    );

    courseCountryRepository.deleteAllByCourseId(courseId);
    courseCityRepository.deleteAllByCourseId(courseId);
    courseTagRepository.deleteAllByCourseId(courseId);
    courseFlightRepository.deleteAllByCourseId(courseId);
    coursePlaceRepository.deleteAllByCourseId(courseId);
    courseImageRepository.deleteAllByCourseId(courseId);
    courseDayRepository.deleteAllByCourseId(courseId);

    saveCourseCountries(course, command.countryIds());
    saveCourseCities(course, command.cityIds());
    saveCourseTags(course, command.tagIds());
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
      if (!courseBookmarkRepository.existsById(new CourseBookmarkId(userId, courseId))) {
        throw e;
      }
    }
  }

  @Transactional
  public void unbookmarkCourse(Long userId, Long courseId) {
    if (!courseRepository.existsByIdAndDeletedAtIsNull(courseId)) {
      throw new BaseException(CourseErrorCode.COURSE_NOT_FOUND);
    }
    courseBookmarkRepository.deleteById(new CourseBookmarkId(userId, courseId));
  }

  private void validateRequiredFields(
      List<Long> countryIds,
      List<Long> cityIds,
      String title,
      LocalDate startDate,
      LocalDate endDate,
      List<Long> tagIds,
      List<CourseDayCommand> days,
      List<CourseFlightCommand> flights
  ) {
    if (countryIds == null || countryIds.isEmpty()
        || cityIds == null || cityIds.isEmpty()
        || title == null || title.isBlank()
        || startDate == null
        || endDate == null
        || tagIds == null || tagIds.isEmpty()
        || days == null || days.isEmpty()) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
    for (CourseDayCommand day : days) {
      if (day.dayNumber() == null
          || day.imageUrls() == null || day.imageUrls().isEmpty()) {
        throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
      }
    }
    if (flights == null) {
      return;
    }
    for (CourseFlightCommand flight : flights) {
      if (flight.airline() == null || flight.airline().isBlank()
          || flight.departureAirport() == null || flight.departureAirport().isBlank()
          || flight.departureAt() == null
          || flight.arrivalAirport() == null || flight.arrivalAirport().isBlank()
          || flight.arrivalAt() == null) {
        throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
      }
    }
  }

  private void validateDateRanges(LocalDate startDate, LocalDate endDate, List<CourseFlightCommand> flights) {
    if (endDate.isBefore(startDate)) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
    if (flights == null) {
      return;
    }
    for (CourseFlightCommand flight : flights) {
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

  private void saveCourseCountries(Course course, List<Long> countryIds) {
    Set<Long> distinctCountryIds = new LinkedHashSet<>(countryIds);
    List<Country> countries = countryRepository.findAllById(distinctCountryIds);
    if (countries.size() != distinctCountryIds.size()) {
      throw new BaseException(LocationErrorCode.COUNTRY_NOT_FOUND);
    }
    courseCountryRepository.saveAll(countries.stream()
        .map(country -> new CourseCountry(course, country))
        .toList());
  }

  private void saveCourseCities(Course course, List<Long> cityIds) {
    Set<Long> distinctCityIds = new LinkedHashSet<>(cityIds);
    List<City> cities = cityRepository.findAllById(distinctCityIds);
    if (cities.size() != distinctCityIds.size()) {
      throw new BaseException(LocationErrorCode.CITY_NOT_FOUND);
    }
    courseCityRepository.saveAll(cities.stream()
        .map(city -> new CourseCity(course, city))
        .toList());
  }

  private void saveCourseTags(Course course, List<Long> tagIds) {
    Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
    List<Tag> tags = tagRepository.findAllById(distinctTagIds);
    if (tags.size() != distinctTagIds.size()) {
      throw new BaseException(CourseErrorCode.TAG_NOT_FOUND);
    }
    TagTypeCountValidator.validate(tags, CourseErrorCode.ACTIVITY_TAG_REQUIRED, CourseErrorCode.TAG_LIMIT_EXCEEDED);

    courseTagRepository.saveAll(tags.stream()
        .map(tag -> new CourseTag(course, tag))
        .toList());
  }

  private void saveCourseCompanions(Course course, List<Long> companionUserIds) {
    if (companionUserIds == null || companionUserIds.isEmpty()) {
      return;
    }
    Set<Long> distinctUserIds = new LinkedHashSet<>(companionUserIds);
    if (distinctUserIds.contains(course.getAuthor().getId())) {
      throw new BaseException(CourseErrorCode.AUTHOR_CANNOT_BE_COMPANION);
    }
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
    for (CoursePlaceCommand command : allPlaceCommands) {
      CoursePlaceCommand firstCommand = firstCommandByGooglePlaceId.putIfAbsent(command.googlePlaceId(), command);
      if (firstCommand != null) {
        validateSamePlaceInfo(firstCommand, command);
      }
    }

    Map<String, Place> resolvedPlaces = new LinkedHashMap<>();
    placeRepository.findByGooglePlaceIdIn(firstCommandByGooglePlaceId.keySet())
        .forEach(place -> resolvedPlaces.put(place.getGooglePlaceId(), place));

    firstCommandByGooglePlaceId.forEach((googlePlaceId, command) -> {
      if (!resolvedPlaces.containsKey(googlePlaceId)) {
        resolvedPlaces.put(googlePlaceId, createPlace(command, parseCategory(command.category())));
      }
    });

    return resolvedPlaces;
  }

  private void validateSamePlaceInfo(CoursePlaceCommand first, CoursePlaceCommand other) {
    if (!first.name().equals(other.name())
        || parseCategory(first.category()) != parseCategory(other.category())) {
      throw new BaseException(CourseErrorCode.PLACE_INFO_CONFLICT);
    }
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
    CourseBookmarkRepository.BookmarkSummary bookmarkSummary =
        courseBookmarkRepository.findBookmarkSummary(userId, course.getId());

    return new CourseDetailResult(
        course.getId(),
        toAuthorResult(course.getAuthor()),
        course.getAuthor().getId().equals(userId),
        bookmarkSummary.getBookmarkedCount() > 0,
        course.getTitle(),
        course.getContent(),
        course.getThumbnailImageUrl(),
        getCourseCountryResults(course.getId()),
        getCourseCityResults(course.getId()),
        course.getStartDate(),
        course.getEndDate(),
        getCourseTagResults(course.getId()),
        getCompanionResults(course.getId()),
        getFlightResults(course.getId()),
        getDayResults(course.getId()),
        course.getViewCount(),
        course.getCommentCount(),
        bookmarkSummary.getTotalCount(),
        course.getCreatedAt()
    );
  }

  private CourseDetailResult.AuthorResult toAuthorResult(User author) {
    AuthorProfile profile = AuthorProfileMapper.toAuthorProfile(author);
    return new CourseDetailResult.AuthorResult(
        profile.userId(),
        profile.nickname(),
        profile.profileImageUrl(),
        profile.country(),
        profile.age(),
        profile.ageRange(),
        profile.gender()
    );
  }

  private List<CourseDetailResult.CountryResult> getCourseCountryResults(Long courseId) {
    return courseCountryRepository.findAllByCourseIdWithCountry(courseId)
        .stream()
        .map(courseCountry -> new CourseDetailResult.CountryResult(
            courseCountry.getCountry().getId(),
            courseCountry.getCountry().getName()
        ))
        .toList();
  }

  private List<CourseDetailResult.CityResult> getCourseCityResults(Long courseId) {
    return courseCityRepository.findAllByCourseIdWithCity(courseId)
        .stream()
        .map(courseCity -> new CourseDetailResult.CityResult(
            courseCity.getCity().getId(),
            courseCity.getCity().getName(),
            getCityKoreanName(courseCity.getCity())
        ))
        .toList();
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
        .map(companion -> toCompanionResult(companion.getUser()))
        .toList();
  }

  private CourseDetailResult.CompanionResult toCompanionResult(User user) {
    if (user.getDeletedAt() != null) {
      return new CourseDetailResult.CompanionResult(user.getId(), AuthorProfileMapper.WITHDRAWN_USER_NICKNAME, null);
    }
    return new CourseDetailResult.CompanionResult(user.getId(), user.getNickname(), user.getProfileImageUrl());
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
}
