package org.sopt.buddys.domain.course.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.repository.CourseCountryRepository;
import org.sopt.buddys.domain.course.service.result.SecondSectionCountry;
import org.sopt.buddys.domain.course.service.result.SecondSectionCountryReason;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecondSectionCountryService {

  private final CourseCountryRepository courseCountryRepository;
  private final CountryRepository countryRepository;

  // 홈 두 번째 코스 섹션의 국가를 정한다. 첫 번째 섹션은 관심 국가다.
  // 파견 국가가 있고 관심 국가와 다르면 파견 국가를 쓰고,
  // 아니면 두 섹션이 겹치지 않도록 관심 국가를 제외하고 코스가 가장 많은 국가를 쓴다. 없으면 null이다.
  public SecondSectionCountry resolve(Country interestCountry, Country exchangeCountry) {
    if (exchangeCountry != null && !isSameCountry(exchangeCountry, interestCountry)) {
      return new SecondSectionCountry(exchangeCountry, SecondSectionCountryReason.EXCHANGE);
    }
    return findMostCoursesCountry(interestCountry == null ? null : interestCountry.getId());
  }

  private boolean isSameCountry(Country country, Country other) {
    return other != null && country.getId().equals(other.getId());
  }

  private SecondSectionCountry findMostCoursesCountry(Long excludeCountryId) {
    List<Long> countryIds = courseCountryRepository.findCountryIdsOrderByCourseCountDesc(
        excludeCountryId, PageRequest.of(0, 1));
    if (countryIds.isEmpty()) {
      return null;
    }
    return countryRepository.findById(countryIds.get(0))
        .map(country -> new SecondSectionCountry(country, SecondSectionCountryReason.MOST_COURSES))
        .orElse(null);
  }
}
