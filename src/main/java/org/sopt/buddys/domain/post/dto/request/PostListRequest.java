package org.sopt.buddys.domain.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class PostListRequest {

  @Schema(description = "제목 또는 본문 검색어", example = "파리")
  private String keyword;

  @Schema(description = "국가 ID", example = "1")
  private Long countryId;

  @Schema(description = "검색 시작일", example = "2026-07-23")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @Schema(description = "검색 종료일", example = "2026-07-28")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate endDate;

  @Schema(description = "나이 조건 목록")
  private List<AgeCondition> ageConditions;

  @Schema(description = "성별 조건 목록")
  private List<GenderCondition> genderConditions;

  @Schema(description = "동행 유형 목록")
  private List<CompanionType> companionTypes;

  @Schema(description = "태그 ID", example = "1")
  private Long tagId;

  @Schema(description = "페이지 번호. 0 이상입니다.", example = "0")
  @Min(0)
  private int page = 0;

  @Schema(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
  @Max(100)
  @Min(1)
  private int size = 20;

  public PostSearchCondition toCondition() {
    return new PostSearchCondition(
        keyword,
        countryId,
        startDate,
        endDate,
        ageConditions,
        genderConditions,
        companionTypes,
        tagId
    );
  }
}
