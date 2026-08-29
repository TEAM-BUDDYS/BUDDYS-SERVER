package org.sopt.buddys.domain.user.service.command;

import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.user.entity.Gender;

public record UpdateProfileCommand(
    String nickname,
    Gender gender,
    LocalDate birthDate,
    String bio,
    List<Long> activityTagIds,
    List<Long> interestTagIds,
    List<Long> travelStyleTagIds
) {}
