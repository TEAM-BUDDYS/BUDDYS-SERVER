package org.sopt.buddys.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.code.CourseErrorCode;
import org.sopt.buddys.domain.course.entity.Course;
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
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CourseServiceTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private CourseService courseService;

  @Autowired
  private CourseRepository courseRepository;

  @Autowired
  private CourseCountryRepository courseCountryRepository;

  @Autowired
  private CourseCityRepository courseCityRepository;

  @Autowired
  private CourseTagRepository courseTagRepository;

  @Autowired
  private CourseDayRepository courseDayRepository;

  @Autowired
  private CourseImageRepository courseImageRepository;

  @Autowired
  private CoursePlaceRepository coursePlaceRepository;

  @Autowired
  private CourseCompanionRepository courseCompanionRepository;

  @Autowired
  private CourseFlightRepository courseFlightRepository;

  @Autowired
  private CourseBookmarkRepository courseBookmarkRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlaceRepository placeRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @DisplayName("코스 작성 시 국가/도시(N:M), 태그, 일자, 장소, 사진, 항공편, 동행자가 함께 저장된다")
  @Test
  void createCourse_savesCourseWithAllDetails() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User companion = userRepository.save(createUser("companion@test.com", "provider-companion", "동행자"));
    Long franceId = insertCountry("프랑스", "FR");
    Long belgiumId = insertCountry("벨기에", "BE");
    Long parisId = insertCity(franceId, "Paris", "파리", 2_000_000L);
    Long brusselsId = insertCity(belgiumId, "Brussels", "브뤼셀", 1_200_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(franceId, belgiumId),
        List.of(parisId, brusselsId),
        " 파리 5일 코스 ",
        " 루브르부터... ",
        "https://example.com/thumbnail.jpg",
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 5),
        List.of(tagId),
        List.of(companion.getId()),
        List.of(new CourseDayCommand(
            (short) 1,
            LocalDate.of(2026, 9, 1),
            List.of("https://example.com/a.jpg", "https://example.com/b.jpg"),
            List.of(new CoursePlaceCommand(
                "ChIJ-place-1",
                "루브르 박물관",
                "TOURISM",
                BigDecimal.valueOf(48.8606),
                BigDecimal.valueOf(2.3376),
                (short) 0,
                "예약 필수",
                BigDecimal.valueOf(22000)
            ))
        )),
        List.of(new CourseFlightCommand(
            "대한항공",
            "KE901",
            "ICN",
            LocalDateTime.of(2026, 9, 1, 13, 0),
            "CDG",
            LocalDateTime.of(2026, 9, 1, 18, 30)
        ))
    );

    // when
    Course course = courseService.createCourse(author.getId(), command);

    // then
    Course savedCourse = courseRepository.findById(course.getId()).orElseThrow();
    assertThat(savedCourse.getTitle()).isEqualTo("파리 5일 코스");
    assertThat(savedCourse.getContent()).isEqualTo("루브르부터...");
    assertThat(savedCourse.getThumbnailImageUrl()).isEqualTo("https://example.com/thumbnail.jpg");
    assertThat(courseCountryRepository.findAll()).hasSize(2);
    assertThat(courseCityRepository.findAll()).hasSize(2);
    assertThat(courseTagRepository.findAll()).hasSize(1);
    assertThat(courseCompanionRepository.findAll()).hasSize(1);
    assertThat(courseDayRepository.findAll()).hasSize(1);
    assertThat(courseImageRepository.findAll()).hasSize(2);
    assertThat(coursePlaceRepository.findAll()).hasSize(1);
    assertThat(courseFlightRepository.findAll()).hasSize(1);

    Place place = placeRepository.findByGooglePlaceId("ChIJ-place-1").orElseThrow();
    assertThat(place.getName()).isEqualTo("루브르 박물관");
  }

  @DisplayName("일자에 사진이 하나도 없으면 예외가 발생한다")
  @Test
  void createCourse_dayWithoutImage_throwsException() {
    // given
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), null,
        List.of(new CourseDayCommand((short) 1, null, null, null)),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(1L, command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.INVALID_REQUEST)
        );
  }

  @DisplayName("도착일이 출발일보다 빠르면 예외가 발생한다")
  @Test
  void createCourse_endDateBeforeStartDate_throwsInvalidRequest() {
    // given
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    CreateCourseCommand command = createDefaultCommand(
        countryId, cityId, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 1), tagId);

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(1L, command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.INVALID_REQUEST)
        );
  }

  @DisplayName("일자(dayNumber)가 중복되면 예외가 발생한다")
  @Test
  void createCourse_duplicateDayNumber_throwsException() {
    // given
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), null,
        List.of(
            new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null),
            new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)
        ),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(1L, command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.DAY_NUMBER_DUPLICATED)
        );
  }

  @DisplayName("존재하지 않는 동행자 ID가 있으면 예외가 발생한다")
  @Test
  void createCourse_companionUserNotFound_throwsException() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), List.of(999_999L),
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(author.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COMPANION_USER_NOT_FOUND)
        );
  }

  @DisplayName("존재하지 않는 국가 ID가 있으면 예외가 발생한다")
  @Test
  void createCourse_countryNotFound_throwsException() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId, 999_999L), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(author.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(LocationErrorCode.COUNTRY_NOT_FOUND)
        );
  }

  @DisplayName("존재하지 않는 도시 ID가 있으면 예외가 발생한다")
  @Test
  void createCourse_cityNotFound_throwsException() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId, 999_999L), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(author.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(LocationErrorCode.CITY_NOT_FOUND)
        );
  }

  @DisplayName("활동 태그가 하나도 없으면 예외가 발생한다")
  @Test
  void createCourse_noActivityTag_throwsException() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long interestTagId = insertTag("맛집", "INTEREST");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(interestTagId), null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(author.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.ACTIVITY_TAG_REQUIRED)
        );
  }

  @DisplayName("활동 태그가 3개를 초과하면 예외가 발생한다")
  @Test
  void createCourse_tooManyActivityTags_throwsException() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    List<Long> activityTagIds = List.of(
        insertTag("도보여행", "ACTIVITY"),
        insertTag("맛집투어", "ACTIVITY"),
        insertTag("쇼핑", "ACTIVITY"),
        insertTag("액티비티", "ACTIVITY")
    );

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        activityTagIds, null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(author.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.TAG_LIMIT_EXCEEDED)
        );
  }

  @DisplayName("작성자 본인을 동행자로 추가하면 예외가 발생한다")
  @Test
  void createCourse_authorAsCompanion_throwsException() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(tagId), List.of(author.getId()),
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );

    // when, then
    assertThatThrownBy(() -> courseService.createCourse(author.getId(), command))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.AUTHOR_CANNOT_BE_COMPANION)
        );
  }

  @DisplayName("작성자가 코스를 수정하면 국가/도시/태그/일자/장소/항공편이 대체되고 동행자는 유지된다")
  @Test
  void updateCourse_byAuthor_replacesDetailsAndKeepsCompanions() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User companion = userRepository.save(createUser("companion@test.com", "provider-companion", "동행자"));
    Long franceId = insertCountry("프랑스", "FR");
    Long parisId = insertCity(franceId, "Paris", "파리", 2_000_000L);
    Long activityTagId = insertTag("도보여행", "ACTIVITY");
    Long newActivityTagId = insertTag("맛집투어", "ACTIVITY");
    Long italyId = insertCountry("이탈리아", "IT");
    Long romeId = insertCity(italyId, "Rome", "로마", 2_800_000L);

    CreateCourseCommand createCommand = new CreateCourseCommand(
        List.of(franceId), List.of(parisId), "파리 코스", "설명", "https://example.com/old.jpg",
        LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
        List.of(activityTagId), List.of(companion.getId()),
        List.of(new CourseDayCommand(
            (short) 1, LocalDate.of(2026, 9, 1), List.of("https://example.com/old-day.jpg"),
            List.of(new CoursePlaceCommand(
                "ChIJ-old", "루브르 박물관", "TOURISM", null, null, (short) 0, null, null))
        )),
        List.of(new CourseFlightCommand(
            "대한항공", "KE901", "ICN", LocalDateTime.of(2026, 9, 1, 13, 0),
            "CDG", LocalDateTime.of(2026, 9, 1, 18, 30)))
    );
    Course course = courseService.createCourse(author.getId(), createCommand);

    UpdateCourseCommand updateCommand = new UpdateCourseCommand(
        List.of(italyId), List.of(romeId), " 로마 코스 ", " 새로운 설명 ", "https://example.com/new.jpg",
        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3),
        List.of(newActivityTagId),
        List.of(new CourseDayCommand(
            (short) 1, LocalDate.of(2026, 10, 1), List.of("https://example.com/new-day.jpg"),
            List.of(new CoursePlaceCommand(
                "ChIJ-new", "콜로세움", "TOURISM", null, null, (short) 0, "예약 필수", BigDecimal.valueOf(16000)))
        )),
        List.of(new CourseFlightCommand(
            "아시아나항공", "OZ501", "ICN", LocalDateTime.of(2026, 10, 1, 9, 0),
            "FCO", LocalDateTime.of(2026, 10, 1, 16, 0)))
    );

    // when
    courseService.updateCourse(author.getId(), course.getId(), updateCommand);

    // then
    Course updatedCourse = courseRepository.findById(course.getId()).orElseThrow();
    assertThat(updatedCourse.getTitle()).isEqualTo("로마 코스");
    assertThat(updatedCourse.getContent()).isEqualTo("새로운 설명");
    assertThat(updatedCourse.getThumbnailImageUrl()).isEqualTo("https://example.com/new.jpg");
    assertThat(updatedCourse.getStartDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(updatedCourse.getEndDate()).isEqualTo(LocalDate.of(2026, 10, 3));

    assertThat(courseCountryRepository.findAllByCourseIdWithCountry(course.getId()))
        .extracting(cc -> cc.getCountry().getId()).containsExactly(italyId);
    assertThat(courseCityRepository.findAllByCourseIdWithCity(course.getId()))
        .extracting(cc -> cc.getCity().getId()).containsExactly(romeId);
    assertThat(courseTagRepository.findAllByCourseIdWithTag(course.getId()))
        .extracting(ct -> ct.getTag().getId()).containsExactly(newActivityTagId);
    assertThat(courseFlightRepository.findAllByCourseIdOrderByOrderNoAsc(course.getId()))
        .extracting("airline").containsExactly("아시아나항공");
    assertThat(courseDayRepository.findAllByCourseIdOrderByDayNumberAsc(course.getId())).hasSize(1);
    assertThat(courseImageRepository.findAll())
        .extracting("imageUrl").containsExactly("https://example.com/new-day.jpg");
    assertThat(coursePlaceRepository.findAll())
        .extracting(cp -> cp.getPlace().getName()).containsExactly("콜로세움");
    assertThat(courseCompanionRepository.findAllByCourseIdWithUser(course.getId()))
        .extracting(cc -> cc.getUser().getId()).containsExactly(companion.getId());
  }

  @DisplayName("작성자가 아닌 유저가 코스를 수정하면 예외가 발생한다")
  @Test
  void updateCourse_notAuthor_throwsForbidden() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User other = userRepository.save(createUser("other@test.com", "provider-other", "다른유저"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Course course = courseService.createCourse(
        author.getId(),
        createDefaultCommand(countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId));
    UpdateCourseCommand updateCommand = createDefaultUpdateCommand(
        countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId);

    // when, then
    assertThatThrownBy(() -> courseService.updateCourse(other.getId(), course.getId(), updateCommand))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.FORBIDDEN)
        );
  }

  @DisplayName("존재하지 않는 코스를 수정하면 예외가 발생한다")
  @Test
  void updateCourse_courseNotFound_throwsException() {
    // given
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    UpdateCourseCommand updateCommand = createDefaultUpdateCommand(
        countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId);

    // when, then
    assertThatThrownBy(() -> courseService.updateCourse(1L, 999_999L, updateCommand))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
  }

  @DisplayName("코스 상세 조회 시 국가/도시(N:M), 태그, 일자, 장소, 항공편, 동행자 정보를 함께 반환한다")
  @Test
  void getCourseDetail_returnsCourseWithAllDetails() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User companion = userRepository.save(createUser("companion@test.com", "provider-companion", "동행자"));
    Long franceId = insertCountry("프랑스", "FR");
    Long belgiumId = insertCountry("벨기에", "BE");
    Long parisId = insertCity(franceId, "Paris", "파리", 2_000_000L);
    Long brusselsId = insertCity(belgiumId, "Brussels", "브뤼셀", 1_200_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");

    CreateCourseCommand command = new CreateCourseCommand(
        List.of(franceId, belgiumId),
        List.of(parisId, brusselsId),
        " 파리 5일 코스 ",
        " 루브르부터... ",
        "https://example.com/thumbnail.jpg",
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 5),
        List.of(tagId),
        List.of(companion.getId()),
        List.of(new CourseDayCommand(
            (short) 1,
            LocalDate.of(2026, 9, 1),
            List.of("https://example.com/a.jpg"),
            List.of(new CoursePlaceCommand(
                "ChIJ-place-1",
                "루브르 박물관",
                "TOURISM",
                BigDecimal.valueOf(48.8606),
                BigDecimal.valueOf(2.3376),
                (short) 0,
                "예약 필수",
                BigDecimal.valueOf(22000)
            ))
        )),
        List.of(new CourseFlightCommand(
            "대한항공",
            "KE901",
            "ICN",
            LocalDateTime.of(2026, 9, 1, 13, 0),
            "CDG",
            LocalDateTime.of(2026, 9, 1, 18, 30)
        ))
    );
    Course course = courseService.createCourse(author.getId(), command);

    // when
    CourseDetailResult result = courseService.getCourseDetail(author.getId(), course.getId());

    // then
    assertThat(result.title()).isEqualTo("파리 5일 코스");
    assertThat(result.content()).isEqualTo("루브르부터...");
    assertThat(result.thumbnailImageUrl()).isEqualTo("https://example.com/thumbnail.jpg");
    assertThat(result.isMine()).isTrue();
    assertThat(result.author().userId()).isEqualTo(author.getId());
    assertThat(result.countries()).extracting("countryId").containsExactlyInAnyOrder(franceId, belgiumId);
    assertThat(result.cities()).extracting("cityId").containsExactlyInAnyOrder(parisId, brusselsId);
    assertThat(result.tags()).hasSize(1);
    assertThat(result.companions()).extracting("userId").containsExactly(companion.getId());
    assertThat(result.flights()).hasSize(1);
    assertThat(result.days()).hasSize(1);
    assertThat(result.days().get(0).imageUrls()).containsExactly("https://example.com/a.jpg");
    assertThat(result.days().get(0).places()).hasSize(1);
    assertThat(result.days().get(0).places().get(0).name()).isEqualTo("루브르 박물관");
    assertThat(result.viewCount()).isEqualTo(1L);
  }

  @DisplayName("존재하지 않는 코스를 조회하면 예외가 발생한다")
  @Test
  void getCourseDetail_courseNotFound_throwsException() {
    // when, then
    assertThatThrownBy(() -> courseService.getCourseDetail(1L, 999_999L))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
  }

  @DisplayName("작성자가 코스를 삭제하면 상세 조회에서 더 이상 조회되지 않는다")
  @Test
  void deleteCourse_byAuthor_softDeletesCourse() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Course course = courseService.createCourse(
        author.getId(),
        createDefaultCommand(countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId));

    // when
    courseService.deleteCourse(author.getId(), course.getId());

    // then
    assertThatThrownBy(() -> courseService.getCourseDetail(author.getId(), course.getId()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
  }

  @DisplayName("작성자가 아닌 유저가 삭제하면 예외가 발생한다")
  @Test
  void deleteCourse_notAuthor_throwsForbidden() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User other = userRepository.save(createUser("other@test.com", "provider-other", "다른유저"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Course course = courseService.createCourse(
        author.getId(),
        createDefaultCommand(countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId));

    // when, then
    assertThatThrownBy(() -> courseService.deleteCourse(other.getId(), course.getId()))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.FORBIDDEN)
        );
  }

  @DisplayName("존재하지 않는 코스를 삭제하면 예외가 발생한다")
  @Test
  void deleteCourse_courseNotFound_throwsException() {
    // when, then
    assertThatThrownBy(() -> courseService.deleteCourse(1L, 999_999L))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
  }

  @DisplayName("코스를 저장하면 북마크가 생성되고, 같은 유저가 다시 저장해도 중복 생성되지 않는다")
  @Test
  void bookmarkCourse_savesBookmarkIdempotently() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Course course = courseService.createCourse(
        author.getId(),
        createDefaultCommand(countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId));

    // when
    courseService.bookmarkCourse(viewer.getId(), course.getId());
    courseService.bookmarkCourse(viewer.getId(), course.getId());

    // then
    assertThat(courseBookmarkRepository.findAll()).hasSize(1);
  }

  @DisplayName("존재하지 않는 코스를 저장하면 예외가 발생한다")
  @Test
  void bookmarkCourse_courseNotFound_throwsException() {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    assertThatThrownBy(() -> courseService.bookmarkCourse(viewer.getId(), 999_999L))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
  }

  @DisplayName("저장한 코스를 저장 취소하면 북마크가 삭제된다")
  @Test
  void unbookmarkCourse_removesBookmark() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Course course = courseService.createCourse(
        author.getId(),
        createDefaultCommand(countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId));
    courseService.bookmarkCourse(viewer.getId(), course.getId());

    // when
    courseService.unbookmarkCourse(viewer.getId(), course.getId());

    // then
    assertThat(courseBookmarkRepository.findAll()).isEmpty();
  }

  @DisplayName("저장하지 않은 코스를 저장 취소해도 예외 없이 처리된다")
  @Test
  void unbookmarkCourse_notBookmarked_doesNothing() {
    // given
    User author = userRepository.save(createUser("author@test.com", "provider-author", "작성자"));
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));
    Long countryId = insertCountry("프랑스", "FR");
    Long cityId = insertCity(countryId, "Paris", "파리", 2_000_000L);
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Course course = courseService.createCourse(
        author.getId(),
        createDefaultCommand(countryId, cityId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5), tagId));

    // when, then
    courseService.unbookmarkCourse(viewer.getId(), course.getId());
    assertThat(courseBookmarkRepository.findAll()).isEmpty();
  }

  @DisplayName("존재하지 않는 코스를 저장 취소하면 예외가 발생한다")
  @Test
  void unbookmarkCourse_courseNotFound_throwsException() {
    // given
    User viewer = userRepository.save(createUser("viewer@test.com", "provider-viewer", "조회자"));

    // when, then
    assertThatThrownBy(() -> courseService.unbookmarkCourse(viewer.getId(), 999_999L))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(CourseErrorCode.COURSE_NOT_FOUND)
        );
  }

  private CreateCourseCommand createDefaultCommand(
      Long countryId, Long cityId, LocalDate startDate, LocalDate endDate, Long tagId
  ) {
    return new CreateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        startDate, endDate,
        List.of(tagId), null,
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );
  }

  private UpdateCourseCommand createDefaultUpdateCommand(
      Long countryId, Long cityId, LocalDate startDate, LocalDate endDate, Long tagId
  ) {
    return new UpdateCourseCommand(
        List.of(countryId), List.of(cityId), "파리 코스", null, null,
        startDate, endDate,
        List.of(tagId),
        List.of(new CourseDayCommand((short) 1, null, List.of("https://example.com/day1.jpg"), null)),
        null
    );
  }

  private User createUser(String email, String providerId, String nickname) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .build();
  }

  private Long insertCountry(String name, String isoCode) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO country (name, iso_code) VALUES (?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, isoCode);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private Long insertCity(Long countryId, String name, String koreanName, Long population) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO city (country_id, name, korean_name, population) VALUES (?, ?, ?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setLong(1, countryId);
          preparedStatement.setString(2, name);
          preparedStatement.setString(3, koreanName);
          preparedStatement.setLong(4, population);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private Long insertTag(String name, String tagType) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
          var preparedStatement = connection.prepareStatement(
              "INSERT INTO tag (name, tag_type) VALUES (?, ?)",
              Statement.RETURN_GENERATED_KEYS
          );
          preparedStatement.setString(1, name);
          preparedStatement.setString(2, tagType);
          return preparedStatement;
        },
        keyHolder
    );
    return keyHolder.getKey().longValue();
  }

  private void cleanUp() {
    jdbcTemplate.update("DELETE FROM course_bookmark");
    jdbcTemplate.update("DELETE FROM course_flight");
    jdbcTemplate.update("DELETE FROM course_companion");
    jdbcTemplate.update("DELETE FROM course_place");
    jdbcTemplate.update("DELETE FROM course_image");
    jdbcTemplate.update("DELETE FROM course_day");
    jdbcTemplate.update("DELETE FROM course_tag");
    jdbcTemplate.update("DELETE FROM course_country");
    jdbcTemplate.update("DELETE FROM course_city");
    jdbcTemplate.update("DELETE FROM course");
    jdbcTemplate.update("DELETE FROM place_bookmark");
    jdbcTemplate.update("DELETE FROM place");
    jdbcTemplate.update("DELETE FROM tag");
    jdbcTemplate.update("DELETE FROM `user`");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
  }
}
