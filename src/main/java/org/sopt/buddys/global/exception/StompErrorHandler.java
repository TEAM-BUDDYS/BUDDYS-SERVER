package org.sopt.buddys.global.exception;

import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompErrorHandler extends StompSubProtocolErrorHandler {

  private final ObjectMapper objectMapper;

  @Override
  public Message<byte[]> handleClientMessageProcessingError(
      Message<byte[]> clientMessage,
      Throwable ex
  ) {

    ErrorCode errorCode = resolveErrorCode(ex);

    log.warn(
        "[StompError] code={}, exceptionType={}",
        errorCode.getCode(),
        ex.getClass().getSimpleName()
    );

    return createErrorMessage(errorCode);
  }

  private ErrorCode resolveErrorCode(
      Throwable ex
  ) {

    Throwable cause = ex;

    while (cause != null) {
      if (cause instanceof BaseException baseException) {
        return baseException.getErrorCode();
      }

      if (cause instanceof BadCredentialsException) {
        return GlobalErrorCode.UNAUTHORIZED;
      }

      if (cause instanceof AccessDeniedException) {
        return GlobalErrorCode.FORBIDDEN;
      }

      cause = cause.getCause();
    }

    return GlobalErrorCode.INTERNAL_SERVER_ERROR;
  }

  private Message<byte[]> createErrorMessage(
      ErrorCode errorCode
  ) {

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
    accessor.setMessage(errorCode.getMessage());
    accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);

    return MessageBuilder.createMessage(
        toPayload(errorCode),
        accessor.getMessageHeaders()
    );
  }

  private byte[] toPayload(
      ErrorCode errorCode
  ) {

    try {
      return objectMapper.writeValueAsBytes(BaseResponse.failure(errorCode));
    } catch (Exception e) {
      log.error("[StompError] Failed to serialize error response", e);
      return "{}".getBytes(StandardCharsets.UTF_8);
    }
  }
}
