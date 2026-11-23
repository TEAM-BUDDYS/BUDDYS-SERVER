package org.sopt.buddys.domain.place.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.place.code.PlaceErrorCode;
import org.sopt.buddys.domain.place.service.PlacePhotoService;
import org.sopt.buddys.domain.place.service.result.PlacePhoto;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PlacePhotoControllerTest extends IntegrationTestSupport {

  @MockitoBean
  private PlacePhotoService placePhotoService;

  @DisplayName("구글 사진은 출처 헤더와 private 캐시 헤더를 담아 리다이렉트한다")
  @Test
  void getPlacePhoto_google_redirectsWithSourceAndCacheHeaders() throws Exception {
    given(placePhotoService.getPhoto("place-1", 400))
        .willReturn(PlacePhoto.google("https://lh3.googleusercontent.com/signed-url"));

    mockMvc.perform(get("/api/v1/places/place-1/photo").param("maxWidth", "400"))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, "https://lh3.googleusercontent.com/signed-url"))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=1800, private"))
        .andExpect(header().string("X-Photo-Source", "GOOGLE"))
        .andExpect(header().doesNotExist("X-Photo-Author-Name"))
        .andExpect(header().doesNotExist("X-Photo-Author-Url"));
  }

  @DisplayName("Unsplash 사진은 작가 이름(퍼센트 인코딩)과 프로필 URL 헤더를 함께 내려준다")
  @Test
  void getPlacePhoto_unsplash_includesAttributionHeaders() throws Exception {
    given(placePhotoService.getPhoto("place-1", 400))
        .willReturn(PlacePhoto.unsplash(
            "https://images.unsplash.com/photo-1?w=400",
            "홍 길동",
            "https://unsplash.com/@hong?utm_source=buddys&utm_medium=referral"));

    mockMvc.perform(get("/api/v1/places/place-1/photo"))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, "https://images.unsplash.com/photo-1?w=400"))
        .andExpect(header().string("X-Photo-Source", "UNSPLASH"))
        .andExpect(header().string("X-Photo-Author-Name", "%ED%99%8D%20%EA%B8%B8%EB%8F%99"))
        .andExpect(header().string("X-Photo-Author-Url",
            "https://unsplash.com/@hong?utm_source=buddys&utm_medium=referral"));
  }

  @DisplayName("사진이 없으면 404를 내려준다")
  @Test
  void getPlacePhoto_notFound_returns404() throws Exception {
    given(placePhotoService.getPhoto("place-1", 400))
        .willThrow(new BaseException(PlaceErrorCode.PLACE_PHOTO_NOT_FOUND));

    mockMvc.perform(get("/api/v1/places/place-1/photo"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PLACE-E002"));
  }
}
