package org.sopt.buddys.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.aws.s3.S3PresignedUrlManager;
import org.sopt.buddys.global.security.oauth.dto.GoogleUserInfo;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ProfileImageUrlIntegrationTest extends IntegrationTestSupport {

  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private S3PresignedUrlManager s3PresignedUrlManager;

  private Long countryId;
  private Long cityId;
  private Long activityId;
  private Long interestId;
  private Long travelStyleId;

  @Override
  protected void cleanUp() {
    jdbcTemplate.update("DELETE FROM user_tag");
    super.cleanUp();
  }

  @BeforeEach
  void prepareReferences() {
    jdbcTemplate.update("INSERT INTO country (name, iso_code) VALUES ('Test Country', 'ZZ')");
    countryId = jdbcTemplate.queryForObject("SELECT id FROM country WHERE iso_code = 'ZZ'", Long.class);
    jdbcTemplate.update("INSERT INTO city (country_id, name, population) VALUES (?, 'Test City', 1)", countryId);
    cityId = jdbcTemplate.queryForObject("SELECT id FROM city WHERE country_id = ?", Long.class, countryId);
    activityId = insertTag("활동", "ACTIVITY");
    interestId = insertTag("관심", "INTEREST");
    travelStyleId = insertTag("스타일", "TRAVEL_STYLE");
  }

  @DisplayName("온보딩은 512, 513, 2048자 PROFILE URL을 자르지 않고 저장한다")
  @ParameterizedTest
  @ValueSource(ints = {512, 513, 2048})
  void completeOnboarding_profileImageWithinLimit_persists(int length) throws Exception {
    User user = saveUser(null);
    String url = uploadedUrl(length);
    mockMvc.perform(request(true, user, url))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("GLB-S001"));
    assertStoredUrl(user, url);
    mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.profileImageUrl").value(url));
  }

  @DisplayName("프로필 수정은 512, 513, 2048자 PROFILE URL을 저장하고 응답한다")
  @ParameterizedTest
  @ValueSource(ints = {512, 513, 2048})
  void updateProfile_profileImageWithinLimit_persists(int length) throws Exception {
    User user = saveUser("https://social.example/old.jpg");
    String url = uploadedUrl(length);
    mockMvc.perform(request(false, user, url))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("GLB-S001"))
        .andExpect(jsonPath("$.data.profileImageUrl").value(url));
    assertStoredUrl(user, url);
  }

  @DisplayName("온보딩과 프로필 수정은 2049자 URL을 400으로 거부하고 기존 값을 보존한다")
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void changeProfile_imageTooLong_returnsBadRequest(boolean onboarding) throws Exception {
    User user = saveUser("https://social.example/old.jpg");
    mockMvc.perform(request(onboarding, user, uploadedUrl(2049)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
    assertStoredUrl(user, user.getProfileImageUrl());
  }

  @DisplayName("길이가 허용되어도 새 외부 이미지 URL은 거부하고 기존 값을 보존한다")
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void changeProfile_newExternalImage_returnsBadRequest(boolean onboarding) throws Exception {
    User user = saveUser("https://social.example/old.jpg");
    String externalUrl = "https://untrusted.example/" + "a".repeat(500) + ".jpg";
    mockMvc.perform(request(onboarding, user, externalUrl))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("USER-E011"));
    assertStoredUrl(user, user.getProfileImageUrl());
  }

  @DisplayName("현재 설정된 긴 소셜 이미지 URL은 온보딩과 수정에서 유지할 수 있다")
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void changeProfile_currentExternalImage_preservesValue(boolean onboarding) throws Exception {
    String url = "https://social.example/" + "a".repeat(1000) + ".jpg";
    User user = saveUser(url);
    mockMvc.perform(request(onboarding, user, url)).andExpect(status().isOk());
    assertStoredUrl(user, url);
  }

  @DisplayName("프로필 이미지에 null을 전달하면 기존 긴 URL을 삭제한다")
  @Test
  void updateProfile_nullImage_removesImage() throws Exception {
    User user = saveUser(uploadedUrl(2048));
    mockMvc.perform(request(false, user, null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.profileImageUrl").value(org.hamcrest.Matchers.nullValue()));
    assertStoredUrl(user, null);
  }

  @DisplayName("Google 소셜 로그인 경로의 2048자 이미지 URL도 DB에서 손실 없이 재조회된다")
  @Test
  void saveGoogleUser_longImage_roundTrips() {
    String url = "https://social.example/" + "a".repeat(2048 - "https://social.example/".length());
    User user = userRepository.saveAndFlush(User.ofGoogle("google-user",
        new GoogleUserInfo("google-user", "google@test.com", true, "Google", url)));
    assertStoredUrl(user, url);
  }

  @DisplayName("온보딩과 프로필 수정 OpenAPI는 이미지 최대 길이를 2048자로 표시한다")
  @Test
  void openApi_profileImageLength_matchesContract() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.schemas.OnboardingRequest.properties.profileImageUrl.maxLength").value(2048))
        .andExpect(jsonPath("$.components.schemas.UpdateProfileRequest.properties.profileImageUrl.maxLength").value(2048));
  }

  private User saveUser(String url) {
    return userRepository.saveAndFlush(User.builder().email("user@test.com")
        .provider(AuthProvider.KAKAO).providerId("user").nickname("사용자")
        .profileImageUrl(url).build());
  }

  private void assertStoredUrl(User user, String url) {
    entityManager.clear();
    assertThat(userRepository.findById(user.getId()).orElseThrow().getProfileImageUrl()).isEqualTo(url);
    assertThat(jdbcTemplate.queryForObject("SELECT profile_image_url FROM `user` WHERE id = ?",
        String.class, user.getId())).isEqualTo(url);
  }

  private String uploadedUrl(int length) {
    String prefix = s3PresignedUrlManager.buildPublicUrl("profiles/");
    return prefix + "a".repeat(length - prefix.length() - 4) + ".jpg";
  }

  private MockHttpServletRequestBuilder request(boolean onboarding, User user, String url) {
    String jsonUrl = url == null ? "null" : "\"" + url + "\"";
    String fields = onboarding
        ? "\"interestCountryId\": %d, \"interestCityId\": %d, \"activityTagIds\": [%d], \"interestTagIds\": [%d], \"travelStyleTagIds\": [%d]"
            .formatted(countryId, cityId, activityId, interestId, travelStyleId)
        : "\"orderedTagIds\": [%d, %d, %d]".formatted(activityId, interestId, travelStyleId);
    String body = """
        {
          "nickname": "수정닉네임", "gender": "FEMALE", "birthDate": "2000-01-01",
          "profileImageUrl": %s,
          %s
        }
        """.formatted(jsonUrl, fields);
    return (onboarding ? patch("/api/v1/users/onboarding") : put("/api/v1/users/me"))
        .header("Authorization", bearerToken(user.getId()))
        .contentType(MediaType.APPLICATION_JSON).content(body);
  }

  private Long insertTag(String name, String type) {
    jdbcTemplate.update("INSERT INTO tag (name, tag_type) VALUES (?, ?)", name, type);
    return jdbcTemplate.queryForObject("SELECT id FROM tag WHERE name = ?", Long.class, name);
  }
}
