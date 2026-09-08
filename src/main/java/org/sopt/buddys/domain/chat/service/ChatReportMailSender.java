package org.sopt.buddys.domain.chat.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.code.ChatErrorCode;
import org.sopt.buddys.domain.chat.entity.ChatUserReport;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.mail.MailProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SesV2Exception;

@Component
@RequiredArgsConstructor
public class ChatReportMailSender {

  private static final String CHARSET = "UTF-8";

  private final SesV2Client sesV2Client;
  private final MailProperties mailProperties;

  public void send(ChatUserReport report) {
    SendEmailRequest request = SendEmailRequest.builder()
        .fromEmailAddress(mailProperties.sender())
        .destination(Destination.builder().toAddresses(mailProperties.operationsRecipient()).build())
        .content(EmailContent.builder()
            .simple(Message.builder()
                .subject(Content.builder()
                    .charset(CHARSET)
                    .data("[Buddys] 채팅 사용자 신고 접수 (채팅방 #%d)".formatted(report.getChatRoom().getId()))
                    .build())
                .body(Body.builder()
                    .html(Content.builder()
                        .charset(CHARSET)
                        .data(buildHtmlBody(report))
                        .build())
                    .build())
                .build())
            .build())
        .build();

    try {
      sesV2Client.sendEmail(request);
    } catch (SesV2Exception e) {
      throw new BaseException(ChatErrorCode.REPORT_MAIL_SEND_FAILED, e);
    }
  }

  private String buildHtmlBody(ChatUserReport report) {
    User reporter = report.getReporter();
    User reported = report.getReported();
    String reason = report.getReason() != null ? report.getReason() : "(사유 미입력)";
    return """
        <div>
          <p>채팅 중 사용자 신고가 접수되었습니다.</p>
          <p><b>채팅방 ID</b>: %d</p>
          <p><b>신고자</b>: %s (userId: %d, %s)</p>
          <p><b>신고 대상</b>: %s (userId: %d, %s)</p>
          <p><b>신고 사유</b>: %s</p>
          <p><b>접수 시각</b>: %s</p>
        </div>
        """.formatted(
        report.getChatRoom().getId(),
        reporter.getNickname(), reporter.getId(), reporter.getEmail(),
        reported.getNickname(), reported.getId(), reported.getEmail(),
        reason,
        report.getCreatedAt()
    );
  }
}
