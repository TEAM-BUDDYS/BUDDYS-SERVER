package org.sopt.buddys.domain.airline.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.airline.entity.Airline;
import org.sopt.buddys.domain.airline.repository.AirlineRepository;
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
public class AirlineService {

  private static final int MAX_SEARCH_RESULT_SIZE = 100;
  private static final String LIKE_ESCAPE_CHAR = "\\";

  private final AirlineRepository airlineRepository;

  public Slice<Airline> searchAirlines(String keyword, int page, int size) {
    validatePageRequest(page, size);

    if (keyword == null || keyword.isBlank()) {
      return new SliceImpl<>(List.of(), PageRequest.of(page, size), false);
    }

    String escapedKeyword = escapeLikeWildcards(keyword.trim());
    return airlineRepository.search(escapedKeyword, PageRequest.of(page, size));
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_SEARCH_RESULT_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private String escapeLikeWildcards(String keyword) {
    return keyword
        .replace(LIKE_ESCAPE_CHAR, LIKE_ESCAPE_CHAR + LIKE_ESCAPE_CHAR)
        .replace("%", LIKE_ESCAPE_CHAR + "%")
        .replace("_", LIKE_ESCAPE_CHAR + "_");
  }
}
