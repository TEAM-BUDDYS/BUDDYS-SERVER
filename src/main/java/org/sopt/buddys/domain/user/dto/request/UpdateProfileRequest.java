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
import org.sopt.buddys.domain.user.service.command.UpdateProfileCommand;

public record UpdateProfileRequest(
    @Schema(description = "닉네임. 최대 14자", example = "정바미")
    @NotBlank @Size(max = 14) String nickname,

    @Schema(description = "성별", example = "FEMALE")
    @NotNull Gender gender,

    @Schema(description = "생년월일", example = "2004-10-24")
    @NotNull @Past LocalDate birthDate,

    @Schema(description = "자기소개. 최대 69자", example = "안녕하세요 김버디입니다~~", nullable = true)
    @Size(max = 69) String bio,

    @Schema(description = "활동 태그 ID 목록. 1~3개", example = "[1, 3]")
    @NotEmpty @Size(min = 1, max = 3) List<Long> activityTagIds,

    @Schema(description = "관심사 태그 ID 목록. 1~3개", example = "[15, 18]")
    @NotEmpty @Size(min = 1, max = 3) List<Long> interestTagIds,

    @Schema(description = "여행 스타일 태그 ID 목록. 1~5개", example = "[27, 30, 34]")
    @NotEmpty @Size(min = 1, max = 5) List<Long> travelStyleTagIds
) {

  public UpdateProfileCommand toCommand() {
    return new UpdateProfileCommand(
        nickname, gender, birthDate, bio,
        activityTagIds, interestTagIds, travelStyleTagIds
    );
  }
}
