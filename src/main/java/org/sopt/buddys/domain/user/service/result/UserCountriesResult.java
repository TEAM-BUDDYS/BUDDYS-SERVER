package org.sopt.buddys.domain.user.service.result;

import org.sopt.buddys.domain.course.service.result.SecondSectionCountry;
import org.sopt.buddys.domain.location.entity.Country;

public record UserCountriesResult(
    Country interestCountry,
    Country exchangeCountry,
    SecondSectionCountry secondSectionCountry
) {}
