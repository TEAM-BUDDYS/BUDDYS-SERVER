package org.sopt.buddys.global.logging;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

  private static final String SENSITIVE_PACKAGE_PREFIX = "org.sopt.buddys.domain.auth.service";

  @Around("@within(org.springframework.stereotype.Service)")
  public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
    String signature = joinPoint.getSignature().toShortString();
    long start = System.currentTimeMillis();

    if (isSensitive(joinPoint)) {
      log.debug("[{}] 호출", signature);
    } else {
      log.debug("[{}] 호출 args={}", signature, Arrays.toString(joinPoint.getArgs()));
    }

    try {
      Object result = joinPoint.proceed();
      log.info("[{}] 호출 완료 ({}ms)", signature, System.currentTimeMillis() - start);
      return result;
    } catch (BaseException e) {
      log.warn("[{}] 비즈니스 예외 ({}ms) code={}, message={}", signature, System.currentTimeMillis() - start, e.getErrorCode(), e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("[{}] 예상치 못한 예외 ({}ms)", signature, System.currentTimeMillis() - start, e);
      throw e;
    }
  }

  private boolean isSensitive(ProceedingJoinPoint joinPoint) {
    return joinPoint.getSignature().getDeclaringTypeName().startsWith(SENSITIVE_PACKAGE_PREFIX);
  }

}
