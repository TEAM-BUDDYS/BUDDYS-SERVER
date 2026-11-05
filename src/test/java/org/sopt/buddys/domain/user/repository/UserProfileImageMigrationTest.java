package org.sopt.buddys.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

class UserProfileImageMigrationTest {

  @DisplayName("V39는 기존 URL과 null을 보존하고 nullable VARCHAR(2048)로 확장한다")
  @Test
  void migrateV39_preservesDataAndExpandsNullableColumn() throws Exception {
    try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")) {
      mysql.start();
      Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
          .target("38").load().migrate();
      try (var connection = DriverManager.getConnection(
          mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
        String oldUrl = "https://social.example/" + "a".repeat(512 - "https://social.example/".length());
        try (var insert = connection.prepareStatement("""
            INSERT INTO `user` (email, provider, provider_id, nickname, profile_image_url, created_at, updated_at)
            VALUES (?, 'KAKAO', ?, ?, ?, NOW(), NOW())
            """)) {
          for (String name : new String[]{"existing", "empty"}) {
            insert.setString(1, name + "@test.com");
            insert.setString(2, name);
            insert.setString(3, name);
            insert.setString(4, name.equals("existing") ? oldUrl : null);
            insert.executeUpdate();
          }
        }

        Flyway.configure().dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
            .load().migrate();

        try (var statement = connection.createStatement();
            var rows = statement.executeQuery("""
                SELECT DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'profile_image_url'
                """)) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("varchar");
          assertThat(rows.getLong(2)).isEqualTo(2048);
          assertThat(rows.getString(3)).isEqualTo("YES");
        }
        try (var statement = connection.createStatement();
            var rows = statement.executeQuery("SELECT nickname, profile_image_url FROM `user` ORDER BY nickname")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("empty");
          assertThat(rows.getString(2)).isNull();
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo("existing");
          assertThat(rows.getString(2)).isEqualTo(oldUrl);
          assertThat(rows.next()).isFalse();
        }
        String longUrl = "https://social.example/" + "a".repeat(2048 - "https://social.example/".length());
        try (var update = connection.prepareStatement(
            "UPDATE `user` SET profile_image_url = ? WHERE nickname = 'existing'")) {
          update.setString(1, longUrl);
          assertThat(update.executeUpdate()).isOne();
        }
        try (var statement = connection.createStatement();
            var rows = statement.executeQuery("SELECT profile_image_url FROM `user` WHERE nickname = 'existing'")) {
          assertThat(rows.next()).isTrue();
          assertThat(rows.getString(1)).isEqualTo(longUrl);
        }
      }
    }
  }
}
