package org.sopt.buddys.domain.location.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CountryService {
  private final CountryRepository countryRepository;
  private static final int MAX_SEARCH_RESULT_SIZE = 100;

  public Slice<Country> searchCountries(String keyword, int page, int size) {
    validatePageRequest(page, size);
    return countryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(
        keyword.trim(), PageRequest.of(page, size)
    );
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_SEARCH_RESULT_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}
