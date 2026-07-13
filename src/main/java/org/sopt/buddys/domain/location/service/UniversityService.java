package org.sopt.buddys.domain.location.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.University;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.location.repository.UniversityRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UniversityService {
  private static final int MAX_SEARCH_RESULT_SIZE = 100;

  private final UniversityRepository universityRepository;
  private final CountryRepository countryRepository;

  public Slice<University> searchUniversities(Long countryId, String keyword, int page, int size) {
    validateCountryExists(countryId);
    validatePageRequest(page, size);
    if (keyword == null || keyword.isBlank()) {
      return new SliceImpl<>(List.of(), PageRequest.of(page, size), false);
    }
    return universityRepository.search(countryId, keyword.trim(), PageRequest.of(page, size));
  }

  private void validateCountryExists(Long countryId) {
    if (!countryRepository.existsById(countryId)) {
      throw new BaseException(LocationErrorCode.COUNTRY_NOT_FOUND);
    }
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_SEARCH_RESULT_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}