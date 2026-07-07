package org.sopt.buddys.domain.location.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CityService {
  private static final int MAX_SEARCH_RESULT_SIZE = 100;

  private final CityRepository cityRepository;
  private final CountryRepository countryRepository;

  public Slice<City> searchCities(Long countryId, String keyword, int page, int size) {
    validateCountryExists(countryId);
    validateKeyword(keyword);
    validatePageRequest(page, size);
    return cityRepository.search(countryId, keyword.trim(), PageRequest.of(page, size));
  }

  private void validateCountryExists(Long countryId) {
    if (!countryRepository.existsById(countryId)) {
      throw new BaseException(LocationErrorCode.COUNTRY_NOT_FOUND);
    }
  }

  private void validateKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_SEARCH_RESULT_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}
