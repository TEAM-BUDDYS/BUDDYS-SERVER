package org.sopt.buddys.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.course.repository.CourseRepository;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CityRepository.CitySuggestionProjection;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SearchSuggestionServiceTest {

  @InjectMocks
  private SearchSuggestionService searchSuggestionService;

  @Mock
  private CountryRepository countryRepository;

  @Mock
  private CityRepository cityRepository;

  @Mock
  private PlaceRepository placeRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private CourseRepository courseRepository;

  @Mock
  private PostRepository postRepository;

  @DisplayName("도시 후보가 검색어와 불일치하면 서버 내부 오류를 발생시킨다")
  @Test
  void getSuggestions_cityCandidateMismatch_throwsInternalServerError() {
    CitySuggestionProjection city = mock(CitySuggestionProjection.class);
    given(city.getMatchedName()).willReturn("Seoul");
    given(cityRepository.findSuggestionCities(
        anyString(), anyString(), anyString(), any(Pageable.class)))
        .willReturn(List.of(city));

    assertThatThrownBy(() -> searchSuggestionService.getSuggestions(1L, "paris", 8))
        .isInstanceOfSatisfying(BaseException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(GlobalErrorCode.INTERNAL_SERVER_ERROR));
  }
}
