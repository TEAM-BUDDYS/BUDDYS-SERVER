package org.sopt.buddys.global.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Slf4j
@ControllerAdvice
public class WebSocketExceptionHandler {

  @MessageExceptionHandler(BaseException.class)
  @SendToUser(value = "/sub/errors", broadcast = false)
  public BaseResponse<Void> handleBaseException(
      BaseException e
  ) {

    ErrorCode errorCode = e.getErrorCode();

    log.warn(
        "[WebSocket BaseException] code={}, message={}",
        errorCode.getCode(),
        errorCode.getMessage()
    );

    return BaseResponse.failure(errorCode);
  }

  @MessageExceptionHandler({
      MethodArgumentNotValidException.class,
      BindException.class,
      ConstraintViolationException.class
  })
  @SendToUser(value = "/sub/errors", broadcast = false)
  public BaseResponse<Void> handleValidationException(
      Exception e
  ) {

    log.warn("[WebSocket ValidationException] type={}", e.getClass().getSimpleName());
    return BaseResponse.failure(GlobalErrorCode.INVALID_REQUEST);
  }

  @MessageExceptionHandler(Exception.class)
  @SendToUser(value = "/sub/errors", broadcast = false)
  public BaseResponse<Void> handleException(
      Exception e
  ) {

    log.error("[WebSocket Exception]", e);
    return BaseResponse.failure(GlobalErrorCode.INTERNAL_SERVER_ERROR);
  }
}
