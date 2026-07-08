package org.sopt.buddys.domain.user.service.command;

import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.user.entity.Gender;

public record OnboardingCommand(
    Long interestCountryId,
    Long interestCityId,
    Long exchangeCountryId,
    String exchangeUniversity,
    LocalDate exchangeStartDate,
    LocalDate exchangeEndDate,
    List<Long> activityTagIds,
    List<Long> interestTagIds,
    List<Long> travelStyleTagIds,
    String nickname,
    Gender gender,
    LocalDate birthDate,
    String bio,
    String profileImageUrl
) {}
