package org.sopt.buddys.global.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

@Component
public class PlacePhotoRateLimitInterceptor implements HandlerInterceptor {

  private static final int MAX_REQUESTS_PER_WINDOW = 60;
  private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

  private final ObjectMapper objectMapper;
  private final ConcurrentHashMap<String, RequestWindow> windows = new ConcurrentHashMap<>();

  public PlacePhotoRateLimitInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String clientIp = resolveClientIp(request);
    RequestWindow window = windows.computeIfAbsent(clientIp, key -> new RequestWindow());

    if (window.incrementAndCheckExceeded()) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      objectMapper.writeValue(response.getWriter(), BaseResponse.failure(GlobalErrorCode.TOO_MANY_REQUESTS));
      return false;
    }
    return true;
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private static final class RequestWindow {
    private volatile long windowStart = System.currentTimeMillis();
    private final AtomicInteger count = new AtomicInteger();

    synchronized boolean incrementAndCheckExceeded() {
      long now = System.currentTimeMillis();
      if (now - windowStart >= WINDOW_MILLIS) {
        windowStart = now;
        count.set(0);
      }
      return count.incrementAndGet() > MAX_REQUESTS_PER_WINDOW;
    }
  }
}