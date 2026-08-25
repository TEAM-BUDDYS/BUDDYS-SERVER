package org.sopt.buddys.domain.course.service.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.Gender;

public record CourseDetailResult(
    Long courseId,
    AuthorResult author,
    boolean isMine,
    String title,
    String content,
    String thumbnailImageUrl,
    CountryResult country,
    CityResult city,
    LocalDate startDate,
    LocalDate endDate,
    List<TagResult> tags,
    List<CompanionResult> companions,
    List<FlightResult> flights,
    List<DayResult> days,
    Long viewCount,
    LocalDateTime createdAt
) {

  public CourseDetailResult {
    tags = List.copyOf(tags);
    companions = List.copyOf(companions);
    flights = List.copyOf(flights);
    days = List.copyOf(days);
  }

  public record AuthorResult(
      Long userId,
      String nickname,
      String profileImageUrl,
      String country,
      Integer age,
      String ageRange,
      Gender gender
  ) {
  }

  public record CountryResult(
      Long countryId,
      String name
  ) {
  }

  public record CityResult(
      Long cityId,
      String name,
      String koreanName
  ) {
  }

  public record TagResult(
      Long tagId,
      String name,
      TagType tagType
  ) {
  }

  public record CompanionResult(
      Long userId,
      String nickname,
      String profileImageUrl
  ) {
  }

  public record FlightResult(
      String airline,
      String flightNumber,
      String departureAirport,
      LocalDateTime departureAt,
      String arrivalAirport,
      LocalDateTime arrivalAt
  ) {
  }

  public record DayResult(
      Short dayNumber,
      LocalDate date,
      List<String> imageUrls,
      List<PlaceResult> places
  ) {

    public DayResult {
      imageUrls = List.copyOf(imageUrls);
      places = List.copyOf(places);
    }
  }

  public record PlaceResult(
      Long placeId,
      String googlePlaceId,
      String name,
      PlaceCategory category,
      BigDecimal latitude,
      BigDecimal longitude,
      String memo,
      BigDecimal cost
  ) {
  }
}
