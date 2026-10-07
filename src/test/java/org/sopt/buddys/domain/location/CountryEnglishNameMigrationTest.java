package org.sopt.buddys.domain.location;

import static org.assertj.core.api.Assertions.assertThat;

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
class CountryEnglishNameMigrationTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @DisplayName("마이그레이션 후 모든 시드 국가에 영문 국가명이 채워진다")
  @Test
  void migration_fillsEnglishNameForAllSeededCountries() {
    // when
    Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM country", Integer.class);
    Integer missing = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM country WHERE english_name IS NULL OR english_name = ''",
        Integer.class
    );
    String korea = jdbcTemplate.queryForObject(
        "SELECT english_name FROM country WHERE iso_code = 'KR'",
        String.class
    );

    // then
    assertThat(total).isPositive();
    assertThat(missing).isZero();
    assertThat(korea).isEqualTo("South Korea");
  }
}
