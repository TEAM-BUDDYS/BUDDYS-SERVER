package org.sopt.buddys.domain.chat.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ChatUserReportRepositoryTest {

  @Container
  @ServiceConnection
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired
  private ChatUserReportRepository chatUserReportRepository;

  @Autowired
  private ChatRoomRepository chatRoomRepository;

  @Autowired
  private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    chatUserReportRepository.deleteAllInBatch();
    chatRoomRepository.deleteAllInBatch();
    userRepository.deleteAllInBatch();
  }

  @DisplayName("사유를 포함해 신고를 저장하고 다시 조회하면 사유를 포함한 모든 필드가 그대로 유지된다")
  @Test
  void save_withReason_roundTripsAllFields() {
    // given
    User reporter = userRepository.save(createUser("reporter@test.com", "provider-reporter", "신고자"));
    User reported = userRepository.save(createUser("reported@test.com", "provider-reported", "신고대상"));
    ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createDirect("1:2"));

    // when
    ChatUserReport saved = chatUserReportRepository.save(
        new ChatUserReport(chatRoom, reporter, reported, "부적절한 언행")
    );
    ChatUserReport found = chatUserReportRepository.findById(saved.getId()).orElseThrow();

    // then
    assertThat(found.getReason()).isEqualTo("부적절한 언행");
    assertThat(found.getReporter().getId()).isEqualTo(reporter.getId());
    assertThat(found.getReported().getId()).isEqualTo(reported.getId());
    assertThat(found.getChatRoom().getId()).isEqualTo(chatRoom.getId());
    assertThat(found.getCreatedAt()).isNotNull();
  }

  @DisplayName("사유 없이 신고를 저장해도 예외 없이 저장되고 reason은 null로 조회된다")
  @Test
  void save_withoutReason_persistsNullReason() {
    // given
    User reporter = userRepository.save(createUser("reporter2@test.com", "provider-reporter2", "신고자2"));
    User reported = userRepository.save(createUser("reported2@test.com", "provider-reported2", "신고대상2"));
    ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.createDirect("3:4"));

    // when
    ChatUserReport saved = chatUserReportRepository.save(
        new ChatUserReport(chatRoom, reporter, reported, null)
    );
    ChatUserReport found = chatUserReportRepository.findById(saved.getId()).orElseThrow();

    // then
    assertThat(found.getReason()).isNull();
  }

  private User createUser(String email, String providerId, String nickname) {
    return User.builder()
        .email(email)
        .provider(AuthProvider.KAKAO)
        .providerId(providerId)
        .nickname(nickname)
        .build();
  }
}
