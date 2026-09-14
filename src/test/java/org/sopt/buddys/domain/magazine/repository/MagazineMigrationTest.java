package org.sopt.buddys.domain.magazine.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

class MagazineMigrationTest {

  @DisplayName("V28은 기존 매거진 카테고리를 보정하고 컬럼을 NOT NULL로 변경한다")
  @Test
  void migrateV28_backfillsCategoryAndMakesColumnNotNull() throws Exception {
    try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")) {
      mysql.start();
      Flyway.configure()
          .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
          .target(MigrationVersion.fromVersion("27"))
          .load()
          .migrate();

      try (Connection connection = DriverManager.getConnection(
          mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
        insertMagazine(connection, "교환학생 여행 안내", "비자와 관광 명소", "travel");
        insertMagazine(connection, "비자 준비", "출국 전 확인", "departure");
        insertMagazine(connection, "현지 은행 계좌", "정착 정보", "settlement");
        insertMagazine(connection, "장학금 지원", "교환학생 혜택", "support");
        insertMagazine(connection, "새로운 소식", "분류 키워드 없음", "fallback");

        Flyway.configure()
            .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
            .load()
            .migrate();

        assertThat(findCategories(connection)).containsExactlyInAnyOrderEntriesOf(Map.of(
            "travel", "TRAVEL",
            "departure", "DEPARTURE_PREP",
            "settlement", "LOCAL_SETTLEMENT",
            "support", "SUPPORT",
            "fallback", "SUPPORT"
        ));
        assertThat(countNullCategories(connection)).isZero();
        assertThat(findCategoryNullableSetting(connection)).isEqualTo("NO");
      }
    }
  }

  private void insertMagazine(Connection connection, String title, String summary, String key)
      throws Exception {
    try (var statement = connection.prepareStatement("""
        INSERT INTO magazine (
            title, summary, thumbnail_image_url, external_url, published_at,
            created_at, updated_at, category
        ) VALUES (?, ?, ?, ?, CURRENT_DATE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), NULL)
        """)) {
      statement.setString(1, title);
      statement.setString(2, summary);
      statement.setString(3, "https://example.com/thumbnail.png");
      statement.setString(4, "https://example.com/" + key);
      statement.executeUpdate();
    }
  }

  private Map<String, String> findCategories(Connection connection) throws Exception {
    Map<String, String> categories = new LinkedHashMap<>();
    try (Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(
             "SELECT external_url, category FROM magazine ORDER BY id")) {
      while (resultSet.next()) {
        String key = resultSet.getString("external_url").replace("https://example.com/", "");
        categories.put(key, resultSet.getString("category"));
      }
    }
    return categories;
  }

  private long countNullCategories(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery(
             "SELECT COUNT(*) FROM magazine WHERE category IS NULL")) {
      resultSet.next();
      return resultSet.getLong(1);
    }
  }

  private String findCategoryNullableSetting(Connection connection) throws Exception {
    try (var statement = connection.prepareStatement("""
        SELECT IS_NULLABLE
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'magazine'
          AND column_name = 'category'
        """)) {
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getString(1);
      }
    }
  }
}
