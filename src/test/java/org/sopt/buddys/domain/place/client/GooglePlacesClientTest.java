package org.sopt.buddys.domain.place.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.place.client.dto.GoogleDisplayName;
import org.sopt.buddys.domain.place.client.dto.GooglePlace;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class GooglePlacesClientTest {

  private static final String GOOGLE_PLACE_ID = "ChIJN1t_tDeuEmsRUsoyG83frY4";

  @InjectMocks
  private GooglePlacesClient googlePlacesClient;

  @Mock
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(googlePlacesClient, "apiKey", "test-api-key");
    ReflectionTestUtils.setField(googlePlacesClient, "baseUrl", "https://places.googleapis.com/v1");
  }

  @DisplayName("장소 상세 조회는 한국어 표기를 요청한다")
  @Test
  void getPlace_requestsKoreanLanguage() {
    // given
    GooglePlace googlePlace = new GooglePlace(
        GOOGLE_PLACE_ID,
        new GoogleDisplayName("루브르 박물관", "ko"),
        "art_gallery",
        List.of("art_gallery"),
        "프랑스 파리",
        null,
        null,
        null
    );
    given(restTemplate.exchange(
        any(URI.class),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(GooglePlace.class)
    )).willReturn(ResponseEntity.ok(googlePlace));

    // when
    GooglePlace result = googlePlacesClient.getPlace(GOOGLE_PLACE_ID);

    // then
    ArgumentCaptor<URI> captor = ArgumentCaptor.forClass(URI.class);
    then(restTemplate).should().exchange(
        captor.capture(),
        eq(HttpMethod.GET),
        any(HttpEntity.class),
        eq(GooglePlace.class)
    );
    assertThat(captor.getValue()).hasToString(
        "https://places.googleapis.com/v1/places/" + GOOGLE_PLACE_ID + "?languageCode=ko"
    );
    assertThat(result).isEqualTo(googlePlace);
  }
}
