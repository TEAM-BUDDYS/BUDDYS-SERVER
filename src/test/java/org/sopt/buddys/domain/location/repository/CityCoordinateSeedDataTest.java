package org.sopt.buddys.domain.location.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class CityCoordinateSeedDataTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @DisplayName("서울, 파리 등 주요 도시는 V35 마이그레이션으로 위도/경도가 채워져 있다")
  @Test
  void citySeedData_knownCities_haveCoordinates() {
    assertCityCoordinates("KR", "Seoul", 37.566, 126.978);
    assertCityCoordinates("FR", "Paris", 48.856, 2.352);
    assertCityCoordinates("JP", "Tokyo", 35.689, 139.692);
    assertCityCoordinates("US", "New York City", 40.712, -74.006);
  }

  @DisplayName("전체 도시 중 대부분(99% 이상)은 위도/경도가 채워져 있다")
  @Test
  void citySeedData_mostCities_haveCoordinates() {
    Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM city", Long.class);
    Long withCoordinates = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM city WHERE latitude IS NOT NULL AND longitude IS NOT NULL", Long.class
    );

    assertThat(total).isGreaterThan(30_000);
    assertThat((double) withCoordinates / total).isGreaterThan(0.99);
  }

  private void assertCityCoordinates(String countryIsoCode, String cityName, double expectedLat, double expectedLng) {
    Map<String, Object> row = jdbcTemplate.queryForMap(
        """
        SELECT c.latitude, c.longitude
        FROM city c
        JOIN country co ON co.id = c.country_id
        WHERE co.iso_code = ? AND c.name = ?
        """,
        countryIsoCode, cityName
    );

    BigDecimal latitude = (BigDecimal) row.get("latitude");
    BigDecimal longitude = (BigDecimal) row.get("longitude");

    assertThat(latitude).isNotNull();
    assertThat(longitude).isNotNull();
    assertThat(latitude.doubleValue()).isCloseTo(expectedLat, org.assertj.core.data.Offset.offset(0.05));
    assertThat(longitude.doubleValue()).isCloseTo(expectedLng, org.assertj.core.data.Offset.offset(0.05));
  }
}
