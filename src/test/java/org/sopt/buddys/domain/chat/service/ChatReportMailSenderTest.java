package org.sopt.buddys.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.mail.MailProperties;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class ChatReportMailSenderTest {

  @Mock
  private SesV2Client sesV2Client;

  @Mock
  private MailProperties mailProperties;

  @Captor
  private ArgumentCaptor<SendEmailRequest> requestCaptor;

  @Test
  void send_maliciousReason_escapesHtmlInMailBody() {
    // given
    ChatReportMailSender chatReportMailSender = new ChatReportMailSender(sesV2Client, mailProperties);
    given(mailProperties.sender()).willReturn("noreply@buddys.com");
    given(mailProperties.operationsRecipient()).willReturn("ops@buddys.com");
    given(sesV2Client.sendEmail(requestCaptor.capture())).willReturn(SendEmailResponse.builder().build());

    User reporter = createUser(1L, "<script>alert(1)</script>");
    User reported = createUser(2L, "정상닉네임");
    ChatRoom chatRoom = createChatRoom(10L);
    ChatUserReport report = new ChatUserReport(
        chatRoom, reporter, reported, "<img src=x onerror=alert(1)>악성 신고 사유"
    );

    // when
    chatReportMailSender.send(report);

    // then
    String htmlBody = requestCaptor.getValue().content().simple().body().html().data();
    assertThat(htmlBody)
        .doesNotContain("<script>")
        .doesNotContain("<img src=x onerror=alert(1)>")
        .contains("&lt;script&gt;alert(1)&lt;/script&gt;")
        .contains("&lt;img src=x onerror=alert(1)&gt;악성 신고 사유");
  }

  private User createUser(Long id, String nickname) {
    return User.builder()
        .id(id)
        .email(nickname + "@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("provider-" + id)
        .nickname(nickname)
        .build();
  }

  private ChatRoom createChatRoom(Long id) {
    ChatRoom chatRoom = ChatRoom.createDirect("direct-chat-key");
    ReflectionTestUtils.setField(chatRoom, "id", id);
    return chatRoom;
  }
}
