package org.sopt.buddys.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.user.entity.Gender;
import org.sopt.buddys.domain.user.service.command.OnboardingCommand;

public record OnboardingRequest(
    @Schema(description = "관심 국가 ID", example = "31")
    @NotNull Long interestCountryId,

    @Schema(description = "관심 도시 ID", example = "20692")
    @NotNull Long interestCityId,

    @Schema(description = "교환학생 국가 ID", example = "35", nullable = true)
    Long exchangeCountryId,

    @Schema(description = "교환학생 대학교", example = "Technical University of Munich", nullable = true)
    @Size(max = 100)
    String exchangeUniversity,

    @Schema(description = "교환학생 시작일", example = "2027-03-01", nullable = true)
    LocalDate exchangeStartDate,

    @Schema(description = "교환학생 종료일", example = "2027-08-31", nullable = true)
    LocalDate exchangeEndDate,

    @Schema(description = "활동 태그 ID 목록. 1~3개", example = "[1, 2, 3]")
    @NotEmpty @Size(min = 1, max = 3) List<Long> activityTagIds,

    @Schema(description = "관심사 태그 ID 목록. 1~3개", example = "[13, 26]")
    @NotEmpty @Size(min = 1, max = 3) List<Long> interestTagIds,

    @Schema(description = "여행 스타일 태그 ID 목록. 1~5개", example = "[27, 30]")
    @NotEmpty @Size(min = 1, max = 5) List<Long> travelStyleTagIds,

    @Schema(description = "닉네임. 최대 14자", example = "버디즈")
    @NotBlank @Size(max = 14) String nickname,

    @Schema(description = "성별", example = "FEMALE")
    @NotNull Gender gender,

    @Schema(description = "생년월일", example = "2000-04-02")
    @NotNull @Past LocalDate birthDate,

    @Schema(description = "자기소개. 최대 69자", example = "버디즈 화이팅!", nullable = true)
    @Size(max = 69) String bio,

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
    @Size(max = 512) String profileImageUrl
) {
    public OnboardingCommand toCommand() {
        return new OnboardingCommand(
            interestCountryId, interestCityId, exchangeCountryId, exchangeUniversity,
            exchangeStartDate, exchangeEndDate, activityTagIds, interestTagIds, travelStyleTagIds,
            nickname, gender, birthDate, bio, profileImageUrl
        );
    }
}