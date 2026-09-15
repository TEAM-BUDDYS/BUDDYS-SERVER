package org.sopt.buddys.domain.magazine.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

class MagazineMigrationTest {

  @DisplayName("V29는 매거진 카테고리 컬럼을 NOT NULL로 추가한다")
  @Test
  void migrateV29_addsCategoryAsNotNull() throws Exception {
    try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")) {
      mysql.start();
      Flyway.configure()
          .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
          .load()
          .migrate();

      try (Connection connection = DriverManager.getConnection(
          mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
        assertThat(countCategoryColumns(connection)).isOne();
        assertThat(findCategoryNullableSetting(connection)).isEqualTo("NO");
      }
    }
  }

  private long countCategoryColumns(Connection connection) throws Exception {
    try (var statement = connection.prepareStatement("""
        SELECT COUNT(*)
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'magazine'
          AND column_name = 'category'
        """)) {
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getLong(1);
      }
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
