package org.sopt.buddys.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.global.exception.BaseException;

class ServiceLoggingAspectTest {

  private final ServiceLoggingAspect aspect = new ServiceLoggingAspect();
  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    logAppender = new ListAppender<>();
    logAppender.start();
    aspectLogger().addAppender(logAppender);
    aspectLogger().setLevel(ch.qos.logback.classic.Level.DEBUG);
  }

  @AfterEach
  void tearDown() {
    aspectLogger().detachAppender(logAppender);
  }

  @DisplayName("auth 서비스 호출은 인자를 로그에 남기지 않는다")
  @Test
  void authServiceCall_doesNotLogArgs() throws Throwable {
    ProceedingJoinPoint joinPoint = joinPointOf(
        "org.sopt.buddys.domain.auth.service.AuthService",
        "kakaoLogin",
        new Object[]{"sensitive-kakao-code"}
    );
    given(joinPoint.proceed()).willReturn(null);

    aspect.logServiceCall(joinPoint);

    assertThat(messages()).noneMatch(msg -> msg.contains("sensitive-kakao-code"));
  }

  @DisplayName("auth 외 서비스 호출은 인자를 로그에 남긴다")
  @Test
  void nonAuthServiceCall_logsArgs() throws Throwable {
    ProceedingJoinPoint joinPoint = joinPointOf(
        "org.sopt.buddys.domain.post.service.PostService",
        "getPost",
        new Object[]{"post-title"}
    );
    given(joinPoint.proceed()).willReturn(null);

    aspect.logServiceCall(joinPoint);

    assertThat(messages()).anyMatch(msg -> msg.contains("post-title"));
  }

  @DisplayName("auth 서비스에서 BaseException이 발생하면 예외 메시지를 로그에 남기지 않는다")
  @Test
  void authServiceCall_businessException_doesNotLogMessage() throws Throwable {
    ProceedingJoinPoint joinPoint = joinPointOf(
        "org.sopt.buddys.domain.auth.service.AuthService",
        "reissue",
        new Object[]{"refresh-token-value"}
    );
    BaseException exception = new BaseException(AuthErrorCode.REFRESH_TOKEN_EXPIRED, "token=refresh-token-value");
    given(joinPoint.proceed()).willThrow(exception);

    org.junit.jupiter.api.Assertions.assertThrows(BaseException.class, () -> aspect.logServiceCall(joinPoint));

    assertThat(messages()).noneMatch(msg -> msg.contains("refresh-token-value"));
    assertThat(messages()).anyMatch(msg -> msg.contains(AuthErrorCode.REFRESH_TOKEN_EXPIRED.getCode()));
  }

  private ProceedingJoinPoint joinPointOf(String declaringTypeName, String methodName, Object[] args) {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    Signature signature = mock(Signature.class);
    given(signature.toShortString()).willReturn(methodName + "(..)");
    given(signature.getDeclaringTypeName()).willReturn(declaringTypeName);
    given(joinPoint.getSignature()).willReturn(signature);
    given(joinPoint.getArgs()).willReturn(args);
    return joinPoint;
  }

  private List<String> messages() {
    return logAppender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }

  private Logger aspectLogger() {
    return (Logger) LoggerFactory.getLogger(ServiceLoggingAspect.class);
  }
}