package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.mail.MailProperties;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Component
@RequiredArgsConstructor
public class ExchangeVerificationNotificationMailSender {

  private static final String CHARSET = "UTF-8";

  private final SesV2Client sesV2Client;
  private final MailProperties mailProperties;

  public void sendApproved(String toEmail) {
    send(
        toEmail,
        "[Buddys] 파견교 인증이 승인되었습니다.",
        "파견교 서류 인증이 승인되었습니다."
    );
  }

  public void sendRejected(String toEmail) {
    send(
        toEmail,
        "[Buddys] 파견교 인증이 반려되었습니다.",
        "파견교 서류 인증이 반려되었습니다. 서류를 확인한 후 다시 신청해 주세요."
    );
  }

  private void send(String toEmail, String subject, String message) {
    SendEmailRequest request = SendEmailRequest.builder()
        .fromEmailAddress(mailProperties.sender())
        .destination(Destination.builder().toAddresses(toEmail).build())
        .content(EmailContent.builder()
            .simple(Message.builder()
                .subject(Content.builder()
                    .charset(CHARSET)
                    .data(subject)
                    .build())
                .body(Body.builder()
                    .html(Content.builder()
                        .charset(CHARSET)
                        .data("<div><p>" + message + "</p></div>")
                        .build())
                    .build())
                .build())
            .build())
        .build();

    sesV2Client.sendEmail(request);
  }
}
