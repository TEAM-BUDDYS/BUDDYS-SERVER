package org.sopt.buddys.global.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class OriginValidationInterceptor implements HandlerInterceptor {
  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String origin = request.getHeader("Origin");
    if (origin == null || !origin.equals(allowedOrigins)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      return false;
    }
    return true;
  }
}
