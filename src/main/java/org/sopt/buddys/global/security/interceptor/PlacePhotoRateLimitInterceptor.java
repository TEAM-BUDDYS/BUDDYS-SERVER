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

  // 창이 끝나고도 한동안 재요청이 없으면 완전히 죽은 엔트리로 간주해 정리 대상으로 삼는다
  private static final long STALE_ENTRY_MILLIS = WINDOW_MILLIS * 2;
  // 정리 스윕은 매 요청마다 돌리지 않고 이 주기로만 수행한다 (O(n) 스캔 비용 절약)
  private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(5).toMillis();
  // 정리 주기가 돌기 전에 서로 다른 IP로 폭주 요청이 와도 메모리가 무한정 늘지 않도록 하는 하드 캡
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
    // computeIfAbsent의 매핑 함수가 null을 반환하면 삽입 없이 null을 돌려준다.
    // 같은 키에 한해 "한도 체크 + 삽입"이 원자적으로 처리되므로, get() 후 별도로
    // computeIfAbsent를 부르던 기존 방식의 체크-후-삽입 경쟁 상태가 사라진다.
    RequestWindow window = windows.computeIfAbsent(clientIp,
        key -> windows.size() >= MAX_TRACKED_CLIENTS ? null : new RequestWindow());

    if (window == null) {
      // 추적 가능한 클라이언트 수를 넘어선 새 클라이언트는 한도 없이 통과시키지 않고 429로 거절한다.
      writeTooManyRequests(response);
      return false;
    }

    if (window.incrementAndCheckExceeded()) {
      writeTooManyRequests(response);
      return false;
    }
    return true;
  }

  private void writeTooManyRequests(HttpServletResponse response) throws java.io.IOException {
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), BaseResponse.failure(GlobalErrorCode.TOO_MANY_REQUESTS));
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
    // X-Forwarded-For는 nginx가 클라이언트가 보낸 값에 실제 IP를 append만 하므로(deploy/nginx/buddys.conf의 $proxy_add_x_forwarded_for) 클라이언트가 조작한 값을 그대로 신뢰하게 될 수 있어 사용하지 않는다.
    // X-Real-IP는 nginx가 $remote_addr로 항상 덮어쓰므로(proxy_set_header X-Real-IP $remote_addr) 신뢰할 수 있다.
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
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
