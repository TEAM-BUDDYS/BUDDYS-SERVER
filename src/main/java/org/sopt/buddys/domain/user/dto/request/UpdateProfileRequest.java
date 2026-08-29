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

    @Schema(
        description = "드래그앤드롭으로 정렬한 전체 선택 태그 ID 목록. 카테고리와 무관하게 상위 3개가 대표 태그로 노출됩니다. "
            + "서버에서 활동 1~3개, 관심사 1~3개, 여행 스타일 1~5개인지 검증합니다.",
        example = "[27, 1, 15, 3, 18, 30, 34]"
    )
    @NotEmpty @Size(min = 3, max = 11) List<@NotNull Long> orderedTagIds
) {

  public UpdateProfileCommand toCommand() {
    return new UpdateProfileCommand(
        nickname, gender, birthDate, bio, orderedTagIds
    );
  }
}
