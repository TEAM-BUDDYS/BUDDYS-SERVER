package org.sopt.buddys.domain.user.service.result;

import org.sopt.buddys.domain.user.entity.Gender;

public record AuthorProfile(
    Long userId,
    String nickname,
    String profileImageUrl,
    String country,
    Integer age,
    String ageRange,
    Gender gender
) {
}
