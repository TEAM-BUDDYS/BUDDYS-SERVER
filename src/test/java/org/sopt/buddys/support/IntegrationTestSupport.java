package org.sopt.buddys.support;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class IntegrationTestSupport {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected JwtProvider jwtProvider;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  protected String bearerToken(Long userId) {
    return "Bearer " + jwtProvider.generateToken(userId);
  }

  protected void cleanUp() {
    jdbcTemplate.update("DELETE FROM course_comment");
    jdbcTemplate.update("DELETE FROM course_bookmark");
    jdbcTemplate.update("DELETE FROM course_flight");
    jdbcTemplate.update("DELETE FROM course_companion");
    jdbcTemplate.update("DELETE FROM course_place");
    jdbcTemplate.update("DELETE FROM course_image");
    jdbcTemplate.update("DELETE FROM course_day");
    jdbcTemplate.update("DELETE FROM course_tag");
    jdbcTemplate.update("DELETE FROM course_country");
    jdbcTemplate.update("DELETE FROM course_city");
    jdbcTemplate.update("DELETE FROM course");
    jdbcTemplate.update("DELETE FROM place_bookmark");
    jdbcTemplate.update("DELETE FROM place");
    jdbcTemplate.update("DELETE FROM tag");
    jdbcTemplate.update("DELETE FROM `user`");
    jdbcTemplate.update("DELETE FROM university");
    jdbcTemplate.update("DELETE FROM city");
    jdbcTemplate.update("DELETE FROM country");
  }
}
