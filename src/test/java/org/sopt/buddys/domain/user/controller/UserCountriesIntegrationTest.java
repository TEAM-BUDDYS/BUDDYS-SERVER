package org.sopt.buddys.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

class UserCountriesIntegrationTest extends IntegrationTestSupport {

  @Autowired
  private UserRepository userRepository;

  @DisplayName("파견 국가가 관심 국가와 다르면 두 번째 섹션 국가는 파견 국가다")
  @Test
  void getMyCountries_differentCountries_secondSectionIsExchangeCountry() throws Exception {
    User user = userRepository.save(createUser());
    Long spainId = insertCountry("스페인", "ES");
    Long franceId = insertCountry("프랑스", "FR");
    setUserCountries(user, spainId, franceId);

    mockMvc.perform(get("/api/v1/users/me/countries")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.interestCountry.id").value(spainId))
        .andExpect(jsonPath("$.data.exchangeCountry.id").value(franceId))
        .andExpect(jsonPath("$.data.secondSectionCountry.id").value(franceId))
        .andExpect(jsonPath("$.data.secondSectionCountry.name").value("프랑스"))
        .andExpect(jsonPath("$.data.secondSectionCountry.code").value("FR"))
        .andExpect(jsonPath("$.data.secondSectionCountry.reason").value("EXCHANGE"));
  }

  @DisplayName("파견 국가가 관심 국가와 같으면 관심 국가를 제외하고 삭제되지 않은 코스가 가장 많은 국가다")
  @Test
  void getMyCountries_sameCountries_secondSectionIsMostCoursesCountryExceptInterest() throws Exception {
    User user = userRepository.save(createUser());
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Long spainId = insertCountry("스페인", "ES");
    Long franceId = insertCountry("프랑스", "FR");
    Long italyId = insertCountry("이탈리아", "IT");
    createCourses(user, spainId, tagId, 3);   // 관심 국가: 코스가 가장 많아도 제외
    createCourses(user, franceId, tagId, 3);  // 2개를 삭제해서 1개만 남김
    createCourses(user, italyId, tagId, 2);
    jdbcTemplate.update("""
        UPDATE course SET deleted_at = NOW()
        WHERE id IN (SELECT course_id FROM (
          SELECT course_id FROM course_country WHERE country_id = ? ORDER BY course_id LIMIT 2
        ) deleted)
        """, franceId);
    setUserCountries(user, spainId, spainId);

    mockMvc.perform(get("/api/v1/users/me/countries")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.interestCountry.id").value(spainId))
        .andExpect(jsonPath("$.data.exchangeCountry.id").value(spainId))
        .andExpect(jsonPath("$.data.secondSectionCountry.id").value(italyId))
        .andExpect(jsonPath("$.data.secondSectionCountry.reason").value("MOST_COURSES"));
  }

  @DisplayName("파견 국가가 없으면 코스가 가장 많은 국가이고, 동점이면 국가 ID가 작은 국가다")
  @Test
  void getMyCountries_noExchangeCountry_secondSectionIsMostCoursesCountryWithIdTieBreak() throws Exception {
    User user = userRepository.save(createUser());
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Long spainId = insertCountry("스페인", "ES");
    Long franceId = insertCountry("프랑스", "FR");
    Long italyId = insertCountry("이탈리아", "IT");
    createCourses(user, italyId, tagId, 1);
    createCourses(user, franceId, tagId, 1);
    setUserCountries(user, spainId, null);

    mockMvc.perform(get("/api/v1/users/me/countries")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.exchangeCountry").doesNotExist())
        .andExpect(jsonPath("$.data.secondSectionCountry.id").value(franceId))
        .andExpect(jsonPath("$.data.secondSectionCountry.reason").value("MOST_COURSES"));
  }

  @DisplayName("관심 국가 외에 코스가 있는 국가가 없으면 두 번째 섹션 국가는 null이다")
  @Test
  void getMyCountries_noOtherCountryCourses_secondSectionIsNull() throws Exception {
    User user = userRepository.save(createUser());
    Long tagId = insertTag("도보여행", "ACTIVITY");
    Long spainId = insertCountry("스페인", "ES");
    createCourses(user, spainId, tagId, 2);
    setUserCountries(user, spainId, spainId);

    mockMvc.perform(get("/api/v1/users/me/countries")
            .header(HttpHeaders.AUTHORIZATION, bearerToken(user.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.interestCountry.id").value(spainId))
        .andExpect(jsonPath("$.data.secondSectionCountry").doesNotExist());
  }

  private void createCourses(User author, Long countryId, Long tagId, int count) throws Exception {
    Long cityId = insertCity(countryId, "City-" + countryId, "도시-" + countryId);
    for (int i = 0; i < count; i++) {
      mockMvc.perform(post("/api/v1/courses")
              .header(HttpHeaders.AUTHORIZATION, bearerToken(author.getId()))
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {
                    "countryIds": [%d],
                    "cityIds": [%d],
                    "title": "코스 %d-%d",
                    "tagIds": [%d],
                    "days": [ { "dayNumber": 1, "imageUrls": ["https://example.com/day1.jpg"] } ]
                  }
                  """.formatted(countryId, cityId, countryId, i, tagId)))
          .andExpect(status().isCreated());
    }
  }

  private void setUserCountries(User user, Long interestCountryId, Long exchangeCountryId) {
    jdbcTemplate.update("UPDATE `user` SET interest_country_id = ?, exchange_country_id = ? WHERE id = ?",
        interestCountryId, exchangeCountryId, user.getId());
  }

  private User createUser() {
    return User.builder()
        .email("user@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("provider-user")
        .nickname("사용자")
        .build();
  }

  private Long insertCountry(String name, String isoCode) {
    return insert("INSERT INTO country (name, iso_code) VALUES (?, ?)", name, isoCode);
  }

  private Long insertCity(Long countryId, String name, String koreanName) {
    return insert("INSERT INTO city (country_id, name, korean_name, population) VALUES (?, ?, ?, ?)",
        countryId, name, koreanName, 1_000_000L);
  }

  private Long insertTag(String name, String tagType) {
    return insert("INSERT INTO tag (name, tag_type) VALUES (?, ?)", name, tagType);
  }

  private Long insert(String sql, Object... params) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> {
      var preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < params.length; i++) {
        preparedStatement.setObject(i + 1, params[i]);
      }
      return preparedStatement;
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }
}
