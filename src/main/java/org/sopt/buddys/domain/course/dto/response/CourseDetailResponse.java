package org.sopt.buddys.domain.course.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.course.service.result.CourseDetailResult;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.user.entity.Gender;

public record CourseDetailResponse(
    @Schema(description = "코스 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    Long courseId,

    @Schema(description = "작성자 정보", requiredMode = Schema.RequiredMode.REQUIRED)
    AuthorResponse author,

    @Schema(description = "로그인 사용자의 코스 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean isMine,

    @Schema(description = "코스 제목", example = "파리 5일 코스", requiredMode = Schema.RequiredMode.REQUIRED)
    String title,

    @Schema(description = "코스 소개", example = "루브르부터...", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    String content,

    @Schema(description = "대표 사진 URL", example = "https://example.com/thumbnail.jpg", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
    String thumbnailImageUrl,

    @Schema(description = "여행 국가 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CourseCountryResponse> countries,

    @Schema(description = "여행 도시 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CourseCityResponse> cities,

    @Schema(description = "출발일", example = "2026-09-01", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate startDate,

    @Schema(description = "도착일", example = "2026-09-05", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate endDate,

    @Schema(description = "연결된 태그 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CourseTagResponse> tags,

    @Schema(description = "함께한 유저 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CompanionResponse> companions,

    @Schema(description = "항공편 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<FlightResponse> flights,

    @Schema(description = "일자별 코스 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<DayResponse> days,

    @Schema(description = "조회수", example = "11", requiredMode = Schema.RequiredMode.REQUIRED)
    Long viewCount,

    @Schema(description = "코스 생성일시", example = "2026-08-20T14:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt
) {

  public CourseDetailResponse {
    countries = List.copyOf(countries);
    cities = List.copyOf(cities);
    tags = List.copyOf(tags);
    companions = List.copyOf(companions);
    flights = List.copyOf(flights);
    days = List.copyOf(days);
  }

  public static CourseDetailResponse from(CourseDetailResult result) {
    return new CourseDetailResponse(
        result.courseId(),
        AuthorResponse.from(result.author()),
        result.isMine(),
        result.title(),
        result.content(),
        result.thumbnailImageUrl(),
        result.countries().stream().map(CourseCountryResponse::from).toList(),
        result.cities().stream().map(CourseCityResponse::from).toList(),
        result.startDate(),
        result.endDate(),
        result.tags().stream().map(CourseTagResponse::from).toList(),
        result.companions().stream().map(CompanionResponse::from).toList(),
        result.flights().stream().map(FlightResponse::from).toList(),
        result.days().stream().map(DayResponse::from).toList(),
        result.viewCount(),
        result.createdAt()
    );
  }

  public record AuthorResponse(
      @Schema(description = "작성자 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
      Long userId,

      @Schema(description = "작성자 닉네임", example = "버디즈", requiredMode = Schema.RequiredMode.REQUIRED)
      String nickname,

      @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.png", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String profileImageUrl,

      @Schema(description = "작성자 국가", example = "대한민국", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String country,

      @Schema(description = "작성자 나이", example = "24", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      Integer age,

      @Schema(description = "작성자 나이대", example = "20대", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String ageRange,

      @Schema(description = "작성자 성별", example = "FEMALE", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      Gender gender
  ) {

    private static AuthorResponse from(CourseDetailResult.AuthorResult author) {
      return new AuthorResponse(
          author.userId(),
          author.nickname(),
          author.profileImageUrl(),
          author.country(),
          author.age(),
          author.ageRange(),
          author.gender()
      );
    }
  }

  public record CourseCountryResponse(
      @Schema(description = "국가 ID", example = "240", requiredMode = Schema.RequiredMode.REQUIRED)
      Long countryId,

      @Schema(description = "국가 이름", example = "France", requiredMode = Schema.RequiredMode.REQUIRED)
      String name
  ) {

    private static CourseCountryResponse from(CourseDetailResult.CountryResult country) {
      return new CourseCountryResponse(country.countryId(), country.name());
    }
  }

  public record CourseCityResponse(
      @Schema(description = "도시 ID", example = "11160", requiredMode = Schema.RequiredMode.REQUIRED)
      Long cityId,

      @Schema(description = "도시 이름", example = "Paris", requiredMode = Schema.RequiredMode.REQUIRED)
      String name,

      @Schema(description = "도시 한글 이름", example = "파리", requiredMode = Schema.RequiredMode.REQUIRED)
      String koreanName
  ) {

    private static CourseCityResponse from(CourseDetailResult.CityResult city) {
      return new CourseCityResponse(city.cityId(), city.name(), city.koreanName());
    }
  }

  public record CourseTagResponse(
      @Schema(description = "태그 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
      Long tagId,

      @Schema(description = "태그 이름", example = "맛집", requiredMode = Schema.RequiredMode.REQUIRED)
      String name
  ) {

    private static CourseTagResponse from(CourseDetailResult.TagResult tag) {
      return new CourseTagResponse(tag.tagId(), tag.name());
    }
  }

  public record CompanionResponse(
      @Schema(description = "동행 유저 ID", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
      Long userId,

      @Schema(description = "동행 유저 닉네임", example = "이버디", requiredMode = Schema.RequiredMode.REQUIRED)
      String nickname,

      @Schema(description = "동행 유저 프로필 이미지 URL", example = "https://example.com/profile2.png", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String profileImageUrl
  ) {

    private static CompanionResponse from(CourseDetailResult.CompanionResult companion) {
      return new CompanionResponse(companion.userId(), companion.nickname(), companion.profileImageUrl());
    }
  }

  public record FlightResponse(
      @Schema(description = "항공사", example = "대한항공", requiredMode = Schema.RequiredMode.REQUIRED)
      String airline,

      @Schema(description = "항공편명", example = "KE901", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String flightNumber,

      @Schema(description = "출발 공항", example = "ICN", requiredMode = Schema.RequiredMode.REQUIRED)
      String departureAirport,

      @Schema(description = "출발일시", example = "2026-09-01T13:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
      LocalDateTime departureAt,

      @Schema(description = "도착 공항", example = "CDG", requiredMode = Schema.RequiredMode.REQUIRED)
      String arrivalAirport,

      @Schema(description = "도착일시", example = "2026-09-01T18:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
      LocalDateTime arrivalAt
  ) {

    private static FlightResponse from(CourseDetailResult.FlightResult flight) {
      return new FlightResponse(
          flight.airline(),
          flight.flightNumber(),
          flight.departureAirport(),
          flight.departureAt(),
          flight.arrivalAirport(),
          flight.arrivalAt()
      );
    }
  }

  public record DayResponse(
      @Schema(description = "일차 (1부터 시작)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
      Short dayNumber,

      @Schema(description = "해당 일자의 실제 날짜", example = "2026-09-01", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      LocalDate date,

      @Schema(description = "해당 일자의 사진 목록", requiredMode = Schema.RequiredMode.REQUIRED)
      List<String> imageUrls,

      @Schema(description = "해당 일자에 방문한 장소 목록", requiredMode = Schema.RequiredMode.REQUIRED)
      List<PlaceResponse> places
  ) {

    public DayResponse {
      imageUrls = List.copyOf(imageUrls);
      places = List.copyOf(places);
    }

    private static DayResponse from(CourseDetailResult.DayResult day) {
      return new DayResponse(
          day.dayNumber(),
          day.date(),
          day.imageUrls(),
          day.places().stream().map(PlaceResponse::from).toList()
      );
    }
  }

  public record PlaceResponse(
      @Schema(description = "장소 ID", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
      Long placeId,

      @Schema(description = "구글 장소 ID", example = "ChIJ-test-place", requiredMode = Schema.RequiredMode.REQUIRED)
      String googlePlaceId,

      @Schema(description = "장소 이름", example = "루브르 박물관", requiredMode = Schema.RequiredMode.REQUIRED)
      String name,

      @Schema(description = "장소 카테고리", example = "TOURISM", requiredMode = Schema.RequiredMode.REQUIRED)
      PlaceCategory category,

      @Schema(description = "위도", example = "48.8606", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      BigDecimal latitude,

      @Schema(description = "경도", example = "2.3376", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      BigDecimal longitude,

      @Schema(description = "메모", example = "예약 필수", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      String memo,

      @Schema(description = "비용", example = "22000", requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
      BigDecimal cost
  ) {

    private static PlaceResponse from(CourseDetailResult.PlaceResult place) {
      return new PlaceResponse(
          place.placeId(),
          place.googlePlaceId(),
          place.name(),
          place.category(),
          place.latitude(),
          place.longitude(),
          place.memo(),
          place.cost()
      );
    }
  }
}
