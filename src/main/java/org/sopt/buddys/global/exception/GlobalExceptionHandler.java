package org.sopt.buddys.global.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.global.common.code.ErrorCode;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    return errorResponse(errorCode);
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

  @ExceptionHandler(BindException.class)
  public ResponseEntity<BaseResponse<Void>> handleBindException(
      BindException e
  ) {

    log.warn("[BindException] {}", e.getMessage());
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e
  ) {

    log.warn("[MissingServletRequestParameterException] {}", e.getMessage());
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<BaseResponse<Void>> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e
  ) {

    log.warn("[MethodArgumentTypeMismatchException] {}", e.getMessage());
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e
  ) {

    log.warn("[HttpMessageNotReadableException] {}", e.getMessage());
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNoHandlerFound(
      NoHandlerFoundException e
  ) {

    log.warn("[NoHandlerFoundException] {}", e.getMessage());
    return errorResponse(GlobalErrorCode.RESOURCE_NOT_FOUND);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNoResourceFound(
      NoResourceFoundException e
  ) {

    log.warn("[NoResourceFoundException] {}", e.getResourcePath());
    return errorResponse(GlobalErrorCode.RESOURCE_NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<BaseResponse<Void>> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException e
  ) {

    log.warn("[HttpRequestMethodNotSupportedException] {}", e.getMessage());
    return errorResponse(GlobalErrorCode.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleException(
      Exception e
  ) {

    log.error("Unexpected Exception", e);

    return errorResponse(GlobalErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<BaseResponse<Void>> errorResponse(
      ErrorCode errorCode
  ) {

    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(BaseResponse.failure(errorCode));
  }
}
