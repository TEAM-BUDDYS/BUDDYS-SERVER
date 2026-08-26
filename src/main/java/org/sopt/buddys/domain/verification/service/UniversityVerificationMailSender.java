package org.sopt.buddys.domain.verification.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.verification.code.UniversityVerificationErrorCode;
import org.sopt.buddys.domain.verification.config.UniversityVerificationProperties;
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
public class UniversityVerificationMailSender {

  private static final String CHARSET = "UTF-8";

  private final SesV2Client sesV2Client;
  private final MailProperties mailProperties;
  private final UniversityVerificationProperties universityVerificationProperties;

  public void send(String toEmail, String universityName, String token) {
    String confirmLink = universityVerificationProperties.confirmUrl() + "?token=" + token;

    SendEmailRequest request = SendEmailRequest.builder()
        .fromEmailAddress(mailProperties.sender())
        .destination(Destination.builder().toAddresses(toEmail).build())
        .content(EmailContent.builder()
            .simple(Message.builder()
                .subject(Content.builder()
                    .charset(CHARSET)
                    .data("[Buddys] " + universityName + " 학교 이메일 인증")
                    .build())
                .body(Body.builder()
                    .html(Content.builder()
                        .charset(CHARSET)
                        .data(buildHtmlBody(universityName, confirmLink))
                        .build())
                    .build())
                .build())
            .build())
        .build();

    try {
      sesV2Client.sendEmail(request);
    } catch (SesV2Exception e) {
      throw new BaseException(UniversityVerificationErrorCode.MAIL_SEND_FAILED, e);
    }
  }

  private String buildHtmlBody(String universityName, String confirmLink) {
    return """
        <div>
          <p>%s 학교 이메일 인증을 완료하려면 아래 버튼을 눌러주세요.</p>
          <p><a href="%s">학교 이메일 인증하기</a></p>
          <p>본인이 요청하지 않았다면 이 메일을 무시하셔도 됩니다.</p>
        </div>
        """.formatted(universityName, confirmLink);
  }
}
