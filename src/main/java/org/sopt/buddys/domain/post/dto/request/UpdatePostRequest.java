package org.sopt.buddys.domain.post.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import lombok.Getter;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.CompanionType;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.RecruitmentCountType;
import org.sopt.buddys.domain.post.service.command.UpdatePostCommand;

@Getter
@Schema(description = "게시글 부분 수정 요청. 전달한 필드만 수정하며, 명시적인 null은 허용하지 않습니다.")
public class UpdatePostRequest {

  @JsonIgnore
  private final EnumSet<Field> providedFields = EnumSet.noneOf(Field.class);

  @Schema(description = "전달하면 국가를 수정하고, 미전달 시 기존 값을 유지합니다. null 불가", example = "1")
  private Long countryId;
  @Schema(description = "전달하면 도시를 수정하고, 미전달 시 기존 값을 유지합니다. null 불가", example = "10")
  private Long cityId;
  @Schema(description = "전달하면 시작일을 수정하고, 미전달 시 기존 값을 유지합니다. null 불가", example = "2026-09-06")
  private LocalDate startDate;
  @Schema(description = "전달하면 종료일을 수정하고, 미전달 시 기존 값을 유지합니다. null 불가", example = "2026-09-19")
  private LocalDate endDate;
  @Schema(description = "전달하면 제목을 수정하고, 미전달 시 기존 값을 유지합니다. null 불가, 최대 120자", example = "주말에 파리 근교 함께 가실 분!")
  private String title;
  @Schema(description = "전달하면 본문을 수정하고, 미전달 시 기존 값을 유지합니다. null 불가", example = "파리 근교 여행 동행을 구합니다.")
  private String content;
  @Schema(description = "전달 시 나이 조건 전체를 교체하고, 미전달 시 유지합니다. null 및 빈 배열 불가")
  private List<AgeCondition> ageConditions;
  @Schema(description = "전달 시 성별 조건 전체를 교체하고, 미전달 시 유지합니다. null 및 빈 배열 불가")
  private List<GenderCondition> genderConditions;
  @Schema(description = "전달하면 동행 유형을 수정하고, 미전달 시 유지합니다. null 불가", example = "FULL_TRIP")
  private CompanionType companionType;
  @Schema(description = "전달하면 모집 인원을 수정하고, 미전달 시 유지합니다. null 불가", example = "TWO")
  private RecruitmentCountType recruitmentCountType;
  @Schema(description = "전달 시 태그 전체를 교체하고, 미전달 시 유지합니다. null 및 빈 배열 불가", example = "[1, 2, 3]")
  private List<Long> tagIds;
  @Schema(description = "전달 시 이미지 전체를 교체하고, 미전달 시 유지합니다. null 불가. 빈 배열은 전체 삭제", example = "[\"https://example.com/post-image.png\"]")
  private List<String> imageUrls;

  public void setCountryId(Long value) { countryId = mark(Field.COUNTRY_ID, value); }
  public void setCityId(Long value) { cityId = mark(Field.CITY_ID, value); }
  public void setStartDate(LocalDate value) { startDate = mark(Field.START_DATE, value); }
  public void setEndDate(LocalDate value) { endDate = mark(Field.END_DATE, value); }
  public void setTitle(String value) { title = mark(Field.TITLE, value); }
  public void setContent(String value) { content = mark(Field.CONTENT, value); }
  public void setAgeConditions(List<AgeCondition> value) { ageConditions = mark(Field.AGE_CONDITIONS, value); }
  public void setGenderConditions(List<GenderCondition> value) { genderConditions = mark(Field.GENDER_CONDITIONS, value); }
  public void setCompanionType(CompanionType value) { companionType = mark(Field.COMPANION_TYPE, value); }
  public void setRecruitmentCountType(RecruitmentCountType value) { recruitmentCountType = mark(Field.RECRUITMENT_COUNT_TYPE, value); }
  public void setTagIds(List<Long> value) { tagIds = mark(Field.TAG_IDS, value); }
  public void setImageUrls(List<String> value) { imageUrls = mark(Field.IMAGE_URLS, value); }

  @JsonIgnore
  public boolean isProvided(Field field) { return providedFields.contains(field); }

  @JsonIgnore
  public boolean isEmpty() { return providedFields.isEmpty(); }

  public UpdatePostCommand toCommand() {
    return new UpdatePostCommand(
        countryId, cityId, startDate, endDate, title, content, ageConditions,
        genderConditions, companionType, recruitmentCountType, tagIds, imageUrls,
        EnumSet.copyOf(providedFields)
    );
  }

  private <T> T mark(Field field, T value) {
    providedFields.add(field);
    return value;
  }

  public enum Field {
    COUNTRY_ID, CITY_ID, START_DATE, END_DATE, TITLE, CONTENT, AGE_CONDITIONS,
    GENDER_CONDITIONS, COMPANION_TYPE, RECRUITMENT_COUNT_TYPE, TAG_IDS, IMAGE_URLS
  }
}
