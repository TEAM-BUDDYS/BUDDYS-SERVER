package org.sopt.buddys.global.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<BaseResponse<Void>> handleBaseException(
      BaseException e
  ) {

    ErrorCode errorCode = e.getErrorCode();

    log.warn(
        "[BusinessException] code={}, message={}",
        errorCode.getCode(),
        e.getMessage()
    );

    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(BaseResponse.failure(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Map<String, String>>> handleValidation(
      MethodArgumentNotValidException e
  ) {

    Map<String, String> errors = new LinkedHashMap<>();

    e.getBindingResult().getFieldErrors()
        .forEach(error ->
            errors.put(
                error.getField(),
                error.getDefaultMessage()
            ));

    return ResponseEntity
        .badRequest()
        .body(BaseResponse.failure(
            GlobalErrorCode.INVALID_REQUEST,
            errors
        ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleException(
      Exception e
  ) {

    log.error("Unexpected Exception", e);

    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(BaseResponse.failure(
            GlobalErrorCode.INTERNAL_SERVER_ERROR
        ));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<BaseResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
    return ResponseEntity
        .badRequest()
        .body(BaseResponse.failure(GlobalErrorCode.INVALID_REQUEST));
  }
}