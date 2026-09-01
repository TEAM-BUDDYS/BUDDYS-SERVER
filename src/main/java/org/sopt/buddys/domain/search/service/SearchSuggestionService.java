package org.sopt.buddys.domain.search.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CityRepository.CitySuggestionProjection;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.search.service.result.SearchSuggestionResult;
import org.sopt.buddys.domain.search.service.result.SearchSuggestionResult.SuggestionResult;
import org.sopt.buddys.domain.search.service.result.SearchSuggestionType;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchSuggestionService {

  private static final int MAX_SUGGESTION_SIZE = 20;

  private final CountryRepository countryRepository;
  private final CityRepository cityRepository;
  private final PlaceRepository placeRepository;
  private final UserRepository userRepository;
  private final CourseRepository courseRepository;
  private final PostRepository postRepository;

  public SearchSuggestionResult getSuggestions(Long userId, String keyword, int size) {
    validateRequest(keyword, size);
    SearchPattern pattern = SearchPattern.from(keyword);
    PageRequest limit = PageRequest.of(0, size);
    List<SuggestionResult> candidates = new ArrayList<>();

    addCandidates(candidates, SearchSuggestionType.COUNTRY,
        countryRepository.findSuggestionNames(
            pattern.exactKeyword(), pattern.prefixPattern(), pattern.containsPattern(), limit));
    addCityCandidates(candidates, pattern, limit);
    addCandidates(candidates, SearchSuggestionType.PLACE,
        placeRepository.findSuggestionNames(
            pattern.exactKeyword(), pattern.prefixPattern(), pattern.containsPattern(), limit));
    addCandidates(candidates, SearchSuggestionType.USER,
        userRepository.findSuggestionNicknames(
            pattern.exactKeyword(), pattern.prefixPattern(), pattern.containsPattern(),
            userId, AccountStatus.ACTIVE, limit));
    addCandidates(candidates, SearchSuggestionType.COURSE,
        courseRepository.findSuggestionTitles(
            pattern.exactKeyword(), pattern.prefixPattern(), pattern.containsPattern(), limit));
    addCandidates(candidates, SearchSuggestionType.POST,
        postRepository.findSuggestionTitles(
            pattern.exactKeyword(), pattern.prefixPattern(), pattern.containsPattern(),
            PostStatus.RECRUITING, limit));

    Map<SuggestionKey, SuggestionResult> distinctCandidates = new LinkedHashMap<>();
    candidates.forEach(candidate -> distinctCandidates.putIfAbsent(
        new SuggestionKey(candidate.type(), normalize(candidate.keyword())),
        candidate
    ));

    List<SuggestionResult> suggestions = distinctCandidates.values().stream()
        .sorted(suggestionComparator(pattern.exactKeyword()))
        .limit(size)
        .toList();
    return new SearchSuggestionResult(suggestions);
  }

  private void addCityCandidates(
      List<SuggestionResult> candidates,
      SearchPattern pattern,
      PageRequest limit
  ) {
    cityRepository.findSuggestionCities(
            pattern.exactKeyword(), pattern.prefixPattern(), pattern.containsPattern(), limit)
        .stream()
        .map(city -> toMatchedCityKeyword(city, pattern.exactKeyword()))
        .forEach(keyword -> candidates.add(new SuggestionResult(SearchSuggestionType.CITY, keyword)));
  }

  private String toMatchedCityKeyword(CitySuggestionProjection city, String keyword) {
    return Stream.of(city.getName(), city.getKoreanName())
        .filter(candidate -> candidate != null && normalize(candidate).contains(keyword))
        .min(Comparator
            .comparingInt((String candidate) -> relevance(candidate, keyword))
            .thenComparing(SearchSuggestionService::normalize)
            .thenComparing(Comparator.naturalOrder()))
        .orElseThrow();
  }

  private void addCandidates(
      List<SuggestionResult> candidates,
      SearchSuggestionType type,
      List<String> keywords
  ) {
    keywords.forEach(keyword -> candidates.add(new SuggestionResult(type, keyword)));
  }

  private Comparator<SuggestionResult> suggestionComparator(String keyword) {
    return Comparator
        .comparingInt((SuggestionResult suggestion) -> relevance(suggestion.keyword(), keyword))
        .thenComparing(suggestion -> normalize(suggestion.keyword()))
        .thenComparing(SuggestionResult::type)
        .thenComparing(SuggestionResult::keyword);
  }

  private static int relevance(String candidate, String keyword) {
    String normalizedCandidate = normalize(candidate);
    if (normalizedCandidate.equals(keyword)) {
      return 0;
    }
    if (normalizedCandidate.startsWith(keyword)) {
      return 1;
    }
    return 2;
  }

  private static String normalize(String value) {
    return value.toLowerCase(Locale.ROOT);
  }

  private void validateRequest(String keyword, int size) {
    if (keyword == null || keyword.isBlank() || size < 1 || size > MAX_SUGGESTION_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private record SuggestionKey(SearchSuggestionType type, String normalizedKeyword) {
  }

  private record SearchPattern(
      String exactKeyword,
      String prefixPattern,
      String containsPattern
  ) {

    private static SearchPattern from(String keyword) {
      String normalizedKeyword = normalize(keyword.trim());
      String escapedKeyword = normalizedKeyword
          .replace("!", "!!")
          .replace("%", "!%")
          .replace("_", "!_");
      return new SearchPattern(
          normalizedKeyword,
          escapedKeyword + "%",
          "%" + escapedKeyword + "%"
      );
    }
  }
}
