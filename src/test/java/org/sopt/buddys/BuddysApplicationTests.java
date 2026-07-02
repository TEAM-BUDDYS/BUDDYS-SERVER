package org.sopt.buddys;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class BuddysApplicationTests {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Test
  void contextLoads() {
  }
}