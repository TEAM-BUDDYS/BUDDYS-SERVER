package org.sopt.buddys.domain.place.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.place.client.dto.unsplash.UnsplashImage;
import org.sopt.buddys.domain.place.config.UnsplashProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

class UnsplashClientTest {

  private static final String SEARCH_URL = "https://api.unsplash.com/search/photos";

  private MockRestServiceServer server;
  private UnsplashClient client;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    client = new UnsplashClient(restTemplate, new UnsplashProperties("access-key", SEARCH_URL, "buddys"));
  }

  @DisplayName("검색 결과 첫 사진의 원본 URL과 utm 파라미터가 붙은 작가 링크를 반환한다")
  @Test
  void searchFirstLandscape_returnsImageWithAttribution() {
    server.expect(requestTo(org.hamcrest.Matchers.startsWith(SEARCH_URL)))
        .andExpect(queryParam("query", "Paris"))
        .andExpect(queryParam("per_page", "1"))
        .andExpect(queryParam("orientation", "landscape"))
        .andExpect(header("Authorization", "Client-ID access-key"))
        .andRespond(withSuccess("""
            {
              "results": [
                {
                  "urls": { "raw": "https://images.unsplash.com/photo-1?ixid=abc" },
                  "user": { "name": "Jane Doe", "links": { "html": "https://unsplash.com/@jane" } }
                }
              ]
            }
            """, MediaType.APPLICATION_JSON));

    assertThat(client.searchFirstLandscape("Paris")).contains(new UnsplashImage(
        "https://images.unsplash.com/photo-1?ixid=abc",
        "Jane Doe",
        "https://unsplash.com/@jane?utm_source=buddys&utm_medium=referral"));
    server.verify();
  }

  @DisplayName("검색 결과가 없으면 빈 값을 반환한다")
  @Test
  void searchFirstLandscape_noResults_returnsEmpty() {
    server.expect(requestTo(org.hamcrest.Matchers.startsWith(SEARCH_URL)))
        .andRespond(withSuccess("{\"results\": []}", MediaType.APPLICATION_JSON));

    assertThat(client.searchFirstLandscape("Nowhere")).isEmpty();
  }

  @DisplayName("한도 초과 등 호출 실패는 예외로 알린다")
  @Test
  void searchFirstLandscape_rateLimited_throws() {
    server.expect(requestTo(org.hamcrest.Matchers.startsWith(SEARCH_URL)))
        .andRespond(withStatus(HttpStatus.FORBIDDEN));

    assertThatThrownBy(() -> client.searchFirstLandscape("Paris")).isInstanceOf(RestClientException.class);
  }
}
