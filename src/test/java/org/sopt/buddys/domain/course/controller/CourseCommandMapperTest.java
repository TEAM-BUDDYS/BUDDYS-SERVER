package org.sopt.buddys.domain.course.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.dto.request.CourseDayRequest;
import org.sopt.buddys.domain.course.dto.request.CourseFlightRequest;
import org.sopt.buddys.domain.course.dto.request.CoursePlaceRequest;
import org.sopt.buddys.domain.course.dto.request.CreateCourseRequest;
import org.sopt.buddys.domain.course.dto.request.UpdateCourseRequest;
import org.sopt.buddys.domain.course.service.command.CourseDayCommand;
import org.sopt.buddys.domain.course.service.command.CreateCourseCommand;
import org.sopt.buddys.domain.course.service.command.UpdateCourseCommand;

class CourseCommandMapperTest {

  @DisplayName("CreateCourseRequest의 모든 필드(날짜/장소/항공편/메모/비용 포함)가 커맨드로 그대로 매핑된다")
  @Test
  void toCommand_createCourseRequest_mapsAllFields() {
    // given
    CreateCourseRequest request = new CreateCourseRequest(
        List.of(1L, 2L),
        List.of(10L, 20L),
        "파리 코스",
        "설명",
        LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 5),
        List.of(100L),
        List.of(200L),
        List.of(new CourseDayRequest(
            (short) 1,
            LocalDate.of(2026, 9, 1),
            List.of("https://example.com/a.jpg"),
            "예약 필수",
            BigDecimal.valueOf(22000),
            List.of(new CoursePlaceRequest(
                "ChIJ-place", "루브르 박물관", "TOURISM",
                BigDecimal.valueOf(48.8606), BigDecimal.valueOf(2.3376), (short) 0
            )),
            List.of(new CourseFlightRequest(
                "대한항공", "KE901", "ICN",
                LocalDateTime.of(2026, 9, 1, 13, 0),
                "CDG", LocalDateTime.of(2026, 9, 1, 18, 30)
            ))
        ))
    );

    // when
    CreateCourseCommand command = CourseCommandMapper.toCommand(request);

    // then
    assertThat(command.countryIds()).containsExactly(1L, 2L);
    assertThat(command.cityIds()).containsExactly(10L, 20L);
    assertThat(command.title()).isEqualTo("파리 코스");
    assertThat(command.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(command.endDate()).isEqualTo(LocalDate.of(2026, 9, 5));
    assertThat(command.companionUserIds()).containsExactly(200L);

    CourseDayCommand dayCommand = command.days().get(0);
    assertThat(dayCommand.dayNumber()).isEqualTo((short) 1);
    assertThat(dayCommand.memo()).isEqualTo("예약 필수");
    assertThat(dayCommand.cost()).isEqualByComparingTo(BigDecimal.valueOf(22000));
    assertThat(dayCommand.places().get(0).googlePlaceId()).isEqualTo("ChIJ-place");
    assertThat(dayCommand.flights().get(0).airline()).isEqualTo("대한항공");
  }

  @DisplayName("days가 null이면 커맨드의 days도 null이다")
  @Test
  void toCommand_createCourseRequest_nullDays_mapsToNull() {
    // given
    CreateCourseRequest request = new CreateCourseRequest(
        List.of(1L), List.of(10L), "제목", null,
        null, null, List.of(100L), null, null
    );

    // when
    CreateCourseCommand command = CourseCommandMapper.toCommand(request);

    // then
    assertThat(command.days()).isNull();
  }

  @DisplayName("UpdateCourseRequest의 필드(메모/비용 포함)가 커맨드로 그대로 매핑된다")
  @Test
  void toCommand_updateCourseRequest_mapsAllFields() {
    // given
    UpdateCourseRequest request = new UpdateCourseRequest(
        List.of(1L), List.of(10L), "수정된 코스", "수정된 설명",
        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3),
        List.of(100L),
        List.of(new CourseDayRequest(
            (short) 1, null, List.of("https://example.com/a.jpg"),
            "메모", BigDecimal.valueOf(5000), null, null
        ))
    );

    // when
    UpdateCourseCommand command = CourseCommandMapper.toCommand(request);

    // then
    assertThat(command.title()).isEqualTo("수정된 코스");
    assertThat(command.days().get(0).memo()).isEqualTo("메모");
    assertThat(command.days().get(0).cost()).isEqualByComparingTo(BigDecimal.valueOf(5000));
  }
}
