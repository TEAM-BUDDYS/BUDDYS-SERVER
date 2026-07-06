package org.sopt.buddys.domain.location.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CountryService {
  private final CountryRepository countryRepository;

  public List<Country> searchCountries(String keyword) {
    return countryRepository.findByNameContainingIgnoreCaseOrderByNameAsc(keyword);
  }
}
