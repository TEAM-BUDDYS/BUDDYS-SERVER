package org.sopt.buddys.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<BaseResponse<Void>> handleBaseException(
      BaseException e,
      HttpServletRequest request
  ) {

    ErrorCode errorCode = e.getErrorCode();

    if (isSensitiveErrorCode(errorCode)) {
      log.warn(
          "[BusinessException] {} {} → code={}",
          request.getMethod(),
          request.getRequestURI(),
          errorCode.getCode()
          );
    } else {
      log.warn(
          "[BusinessException] {} {} → code={}, message={}",
          request.getMethod(),
          request.getRequestURI(),
          errorCode.getCode(),
          e.getMessage()
      );
    }

    return errorResponse(errorCode);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Map<String, String>>> handleValidation(
      MethodArgumentNotValidException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[MethodArgumentNotValidException] {} {} → fieldErrorCount={}",
        request.getMethod(),
        request.getRequestURI(),
        e.getBindingResult().getFieldErrorCount()
    );

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
      BindException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[BindException] {} {} → objectName={}, fieldErrorCount={}",
        request.getMethod(),
        request.getRequestURI(),
        e.getObjectName(),
        e.getBindingResult().getFieldErrorCount()
    );
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(
      ConstraintViolationException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[ConstraintViolationException] {} {} → violationCount={}",
        request.getMethod(),
        request.getRequestURI(),
        e.getConstraintViolations().size()
    );
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<BaseResponse<Void>> handleHandlerMethodValidation(
      HandlerMethodValidationException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[HandlerMethodValidationException] {} {} → parameterValidationCount={}",
        request.getMethod(),
        request.getRequestURI(),
        e.getParameterValidationResults().size()
    );
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<BaseResponse<Void>> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[MissingServletRequestParameterException] {} {} → parameterName={}, parameterType={}",
        request.getMethod(),
        request.getRequestURI(),
        e.getParameterName(),
        e.getParameterType()
    );
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<BaseResponse<Void>> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[MethodArgumentTypeMismatchException] {} {} → parameterName={}, requiredType={}",
        request.getMethod(),
        request.getRequestURI(),
        e.getName(),
        getSimpleName(e.getRequiredType())
    );
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[HttpMessageNotReadableException] {} {} → causeType={}",
        request.getMethod(),
        request.getRequestURI(),
        getSimpleName(e.getMostSpecificCause().getClass())
    );
    return errorResponse(GlobalErrorCode.INVALID_REQUEST);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNoHandlerFound(
      NoHandlerFoundException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[NoHandlerFoundException] {} {}",
        request.getMethod(),
        request.getRequestURI()
    );
    return errorResponse(GlobalErrorCode.RESOURCE_NOT_FOUND);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<BaseResponse<Void>> handleNoResourceFound(
      NoResourceFoundException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[NoResourceFoundException] {} {}",
        request.getMethod(),
        request.getRequestURI()
    );
    return errorResponse(GlobalErrorCode.RESOURCE_NOT_FOUND);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<BaseResponse<Void>> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException e,
      HttpServletRequest request
  ) {

    log.warn(
        "[HttpRequestMethodNotSupportedException] {} {} → supportedMethods={}",
        request.getMethod(),
        request.getRequestURI(),
        e.getSupportedHttpMethods()
    );
    return errorResponse(GlobalErrorCode.METHOD_NOT_ALLOWED);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleException(
      Exception e,
      HttpServletRequest request
  ) {

    log.error(
        "[UnexpectedException] {} {}",
        request.getMethod(),
        request.getRequestURI(),
        e
    );

    return errorResponse(GlobalErrorCode.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<BaseResponse<Void>> errorResponse(
      ErrorCode errorCode
  ) {

    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(BaseResponse.failure(errorCode));
  }

  private String getSimpleName(
      Class<?> type
  ) {

    if (type == null) {
      return null;
    }
    return type.getSimpleName();
  }

  private boolean isSensitiveErrorCode(ErrorCode errorCode) {
    return errorCode.getCode().startsWith("AUTH-");
  }
}