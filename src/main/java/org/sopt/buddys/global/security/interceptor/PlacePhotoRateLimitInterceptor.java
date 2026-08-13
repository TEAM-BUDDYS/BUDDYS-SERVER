package org.sopt.buddys.global.security.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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

  // 창이 끝나고도 한동안 재요청이 없으면 완전히 죽은 엔트리로 간주해 정리 대상으로 삼는다.
  private static final long STALE_ENTRY_MILLIS = WINDOW_MILLIS * 2;
  // 정리 스윕은 매 요청마다 돌리지 않고 이 주기로만 수행한다 (O(n) 스캔 비용 절약).
  private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(5).toMillis();
  // 정리 주기가 돌기 전에 서로 다른 IP로 폭주 요청이 와도 메모리가 무한정 늘지 않도록 하는 하드 캡.
  private static final int MAX_TRACKED_CLIENTS = 10_000;

  private final ObjectMapper objectMapper;
  private final ConcurrentHashMap<String, RequestWindow> windows = new ConcurrentHashMap<>();
  private final AtomicLong lastCleanupAt = new AtomicLong(System.currentTimeMillis());

  public PlacePhotoRateLimitInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    evictStaleWindowsIfDue();

    String clientIp = resolveClientIp(request);
    RequestWindow window = windows.get(clientIp);
    if (window == null) {
      if (windows.size() >= MAX_TRACKED_CLIENTS) {
        // 추적 가능한 클라이언트 수를 넘어서면 더 이상 맵을 키우지 않고 이번 요청은 통과시킨다.
        return true;
      }
      window = windows.computeIfAbsent(clientIp, key -> new RequestWindow());
    }

    if (window.incrementAndCheckExceeded()) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      objectMapper.writeValue(response.getWriter(), BaseResponse.failure(GlobalErrorCode.TOO_MANY_REQUESTS));
      return false;
    }
    return true;
  }

  private void evictStaleWindowsIfDue() {
    long now = System.currentTimeMillis();
    long last = lastCleanupAt.get();
    if (now - last < CLEANUP_INTERVAL_MILLIS) {
      return;
    }
    if (lastCleanupAt.compareAndSet(last, now)) {
      windows.entrySet().removeIf(entry -> now - entry.getValue().windowStart >= STALE_ENTRY_MILLIS);
    }
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